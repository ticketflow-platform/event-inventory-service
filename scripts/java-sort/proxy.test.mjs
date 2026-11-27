import { test } from 'node:test';
import assert from 'node:assert/strict';
import { PassThrough } from 'node:stream';
import { bridge, changes, command, frame, receive } from './proxy.mjs';

function harness(sort = async source => source.replace('old', 'new')) {
  const client = new PassThrough(), toClient = new PassThrough();
  const server = new PassThrough(), toServer = new PassThrough();
  const clientMessages = [], serverMessages = [];
  receive(toClient, message => clientMessages.push(message));
  receive(toServer, message => serverMessages.push(message));
  bridge(client, toClient, server, toServer, sort);
  const uri = 'file:///C:/project/Sample.java';
  const send = message => client.write(frame(message));
  const respond = message => server.write(frame(message));
  const open = () => send({ method: 'textDocument/didOpen', params: { textDocument: { uri, text: 'old Привет', version: 1 } } });
  return { client, send, respond, open, uri, clientMessages, serverMessages };
}

const tick = () => new Promise(resolve => setImmediate(resolve));

test('LSP framing handles split frames, batches and UTF-8', () => {
  const stream = new PassThrough(), messages = [];
  receive(stream, message => messages.push(message));
  const message = { id: 1, result: 'Привет 👋' };
  const bytes = frame(message);
  for (const byte of bytes) stream.write(Buffer.from([byte]));
  stream.write(Buffer.concat([frame(message), frame(message)]));
  assert.deepEqual(messages, [message, message, message]);
});

test('incremental edits use UTF-16 offsets and apply in sequence', () => {
  assert.equal(changes('👋 old\r\nПривет', [
    { range: { start: { line: 0, character: 3 }, end: { line: 0, character: 6 } }, text: 'new' },
    { range: { start: { line: 1, character: 0 }, end: { line: 1, character: 6 } }, text: 'мир' },
  ]), '👋 new\r\nмир');
});

test('native sort actions are replaced, other actions and messages pass through', () => {
  const h = harness(); h.open();
  h.send({ id: 1, method: 'textDocument/codeAction', params: { textDocument: { uri: h.uri }, context: {} } });
  h.respond({ id: 1, result: [{ title: "Sort Members for 'Sample.java'" }, { title: 'Organize imports' }] });
  const result = h.clientMessages.at(-1).result;
  assert.deepEqual(result.map(action => action.title), ['Organize imports', "Sort Members for 'Sample.java' (call order)"]);
  assert.equal(result[1].command.command, command);
  h.respond({ method: 'textDocument/publishDiagnostics', params: { diagnostics: [] } });
  assert.equal(h.clientMessages.at(-1).method, 'textDocument/publishDiagnostics');
  assert.equal(h.serverMessages.at(-1).method, 'textDocument/codeAction');
});

test('sort is offered even when alphabetical JDTLS order already needs no edits', () => {
  const h = harness(); h.open();
  h.send({ id: 1, method: 'textDocument/codeAction', params: { textDocument: { uri: h.uri }, context: {} } });
  h.respond({ id: 1, result: null });
  assert.equal(h.clientMessages.at(-1).result[0].command.command, command);
});

test('Zed continues to accept the sorter after JDTLS dynamically replaces its command capabilities', () => {
  const h = harness(); h.open();
  h.respond({ id: 0, result: { capabilities: { executeCommandProvider: { commands: ['java.original'] } } } });
  const initialCommands = h.clientMessages.at(-1).result.capabilities.executeCommandProvider.commands;
  assert.ok(initialCommands.includes(command));
  h.respond({ id: 'register', method: 'client/registerCapability', params: { registrations: [
    { id: 'commands', method: 'workspace/executeCommand', registerOptions: { commands: ['java.dynamic'] } },
    { id: 'files', method: 'workspace/didChangeWatchedFiles', registerOptions: { watchers: [] } },
  ] } });
  const registration = h.clientMessages.at(-1);
  const currentCommands = registration.params.registrations[0].registerOptions.commands;
  assert.deepEqual(currentCommands, ['java.dynamic', command]);
  assert.deepEqual(registration.params.registrations[1].registerOptions, { watchers: [] });
  h.send({ id: 1, method: 'textDocument/codeAction', params: { textDocument: { uri: h.uri }, context: {} } });
  h.respond({ id: 1, result: [] });
  // This is the same command-capability check performed by Zed's GetCodeActions.
  const visible = h.clientMessages.at(-1).result.filter(action => currentCommands.includes(action.command.command));
  assert.equal(visible.length, 1);
  assert.equal(visible[0].command.command, command);
});

test('fully specified custom actions resolve locally without JDTLS data', () => {
  const h = harness();
  const action = { title: 'Sort Members', kind: 'source.sortMembers',
    command: { title: 'Sort Members', command, arguments: [{ uri: h.uri, version: 1 }] } };
  h.send({ id: 1, method: 'codeAction/resolve', params: action });
  assert.deepEqual(h.clientMessages.at(-1), { jsonrpc: '2.0', id: 1, result: action });
  assert.deepEqual(h.serverMessages, []);
});

test('quick-fix-only requests and JDTLS errors are not rewritten', () => {
  const h = harness(); h.open();
  h.send({ id: 1, method: 'textDocument/codeAction', params: { textDocument: { uri: h.uri }, context: { only: ['quickfix'] } } });
  h.respond({ id: 1, result: [] });
  assert.deepEqual(h.clientMessages.at(-1).result, []);
  h.send({ id: 2, method: 'textDocument/codeAction', params: { textDocument: { uri: h.uri }, context: {} } });
  h.respond({ id: 2, error: { code: -1, message: 'failed' } });
  assert.equal(h.clientMessages.at(-1).error.message, 'failed');
});

test('sorting edits the current unsaved buffer with a versioned workspace edit', async () => {
  const h = harness(); h.open();
  h.send({ id: 1, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 1 }] } });
  await tick();
  const edit = h.clientMessages.at(-1);
  assert.equal(edit.method, 'workspace/applyEdit');
  assert.deepEqual(edit.params.edit.documentChanges[0].textDocument, { uri: h.uri, version: 1 });
  assert.equal(edit.params.edit.documentChanges[0].edits[0].newText, 'new Привет');
  h.send({ id: edit.id, result: { applied: true } });
  await tick();
  assert.deepEqual(h.clientMessages.at(-1), { jsonrpc: '2.0', id: 1, result: null });
  assert.equal(h.serverMessages.some(message => message.params?.command === command), false);
});

test('no-op sort produces no workspace edit', async () => {
  const h = harness(async source => source); h.open();
  h.send({ id: 1, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 1 }] } });
  await tick();
  assert.deepEqual(h.clientMessages, [{ jsonrpc: '2.0', id: 1, result: null }]);
});

test('stale or closed buffers and concurrent edits never receive stale replacements', async () => {
  let finish;
  const h = harness(() => new Promise(resolve => { finish = resolve; })); h.open();
  h.send({ id: 1, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 0 }] } });
  await tick();
  assert.match(h.clientMessages.at(-1).error.message, /buffer changed/);
  h.send({ id: 2, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 1 }] } });
  h.send({ method: 'textDocument/didChange', params: { textDocument: { uri: h.uri, version: 2 }, contentChanges: [{ text: 'newer' }] } });
  finish('stale result');
  await tick();
  assert.match(h.clientMessages.at(-1).error.message, /buffer changed while sorting/);
  assert.equal(h.clientMessages.some(message => message.method === 'workspace/applyEdit'), false);
  h.send({ method: 'textDocument/didClose', params: { textDocument: { uri: h.uri } } });
  h.send({ id: 3, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 2 }] } });
  await tick();
  assert.ok(h.clientMessages.at(-1).error);
});

test('sorter failures and declined edits are returned as errors, not successful refactorings', async () => {
  const failed = harness(async () => { throw new Error('Ambiguous overload'); }); failed.open();
  failed.send({ id: 1, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: failed.uri, version: 1 }] } });
  await tick();
  assert.match(failed.clientMessages.at(-1).error.message, /Ambiguous overload/);
  const h = harness(); h.open();
  h.send({ id: 1, method: 'workspace/executeCommand', params: { command, arguments: [{ uri: h.uri, version: 1 }] } });
  await tick();
  h.send({ id: h.clientMessages.at(-1).id, result: { applied: false, failureReason: 'buffer changed' } });
  await tick();
  assert.match(h.clientMessages.at(-1).error.message, /buffer changed/);
});
