import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { command, frame, receive } from './proxy.mjs';

test('real Zed Java proxy and JDTLS still initialize; member ordering edits the unsaved buffer', { timeout: 45000 }, async t => {
  const log = readFileSync(join(process.env.LOCALAPPDATA, 'Zed/logs/Zed.log'), 'utf8');
  const launch = log.split('\n').reverse().find(line => line.includes('starting language server process.')
    && (line.includes('java-lsp-proxy.exe') || line.includes('java-sort-proxy.cmd'))
    && line.includes('event-inventory-service'));
  assert.ok(launch, 'Run the standard Zed Java extension once before this integration test.');
  const args = JSON.parse(launch.slice(launch.indexOf('args: ') + 6));
  const temp = mkdtempSync(join(process.env.LOCALAPPDATA, 'Temp/opencode/java-member-order-'));
  args[args.indexOf('-data') + 1] = temp;
  const wrapper = resolve('scripts/java-sort-proxy.cmd');
  const commandLine = `""${wrapper}" ${args.map(value => `"${value}"`).join(' ')}"`;
  const proxy = spawn(process.env.ComSpec ?? 'cmd.exe', ['/d', '/s', '/c', commandLine], {
    windowsVerbatimArguments: true, stdio: ['pipe', 'pipe', 'pipe'],
  });
  t.after(() => {
    if (proxy.exitCode === null) {
      spawnSync('taskkill.exe', ['/pid', String(proxy.pid), '/t', '/f']);
    }
  });
  let diagnostics = '';
  proxy.stderr.on('data', data => { diagnostics += data; });
  const pending = new Map();
  let nextId = 0, receivedEdit, availableCommands = [];
  let commandRegistrations = 0;
  const send = message => proxy.stdin.write(frame({ jsonrpc: '2.0', ...message }));
  const request = (method, params) => new Promise((accept, reject) => {
    const id = ++nextId;
    pending.set(id, { accept, reject });
    send({ id, method, params });
  });
  receive(proxy.stdout, message => {
    if (message.method === 'workspace/applyEdit') {
      receivedEdit = message.params.edit;
      send({ id: message.id, result: { applied: true } });
    } else if (message.method === 'client/registerCapability') {
      for (const registration of message.params.registrations) {
        if (registration.method === 'workspace/executeCommand') {
          availableCommands = registration.registerOptions.commands;
          commandRegistrations++;
        }
      }
      send({ id: message.id, result: null });
    } else if (message.method && message.id != null) {
      send({ id: message.id, result: null });
    } else if (pending.has(message.id)) {
      const handler = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) handler.reject(new Error(message.error.message));
      else handler.accept(message.result);
    }
  });
  proxy.on('close', code => {
    for (const handler of pending.values()) handler.reject(new Error(`Proxy exited (${code}): ${diagnostics}`));
  });
  const rootUri = pathToFileURL(process.cwd()).href;
  const initialized = await request('initialize', {
    processId: process.pid, rootUri,
    workspaceFolders: [{ uri: rootUri, name: 'event-inventory-service' }],
    capabilities: {
      workspace: { applyEdit: true, executeCommand: { dynamicRegistration: true } },
      textDocument: {
        synchronization: { dynamicRegistration: true },
        codeAction: {
          dynamicRegistration: true, dataSupport: true,
          codeActionLiteralSupport: { codeActionKind: { valueSet: ['refactor', 'quickfix', 'source'] } },
          resolveSupport: { properties: ['kind', 'diagnostics', 'isPreferred', 'disabled', 'edit', 'command'] },
        },
      },
    },
    initializationOptions: { settings: { java: { autobuild: { enabled: false } } } },
  });
  assert.ok(initialized.capabilities.executeCommandProvider.commands.includes(command));
  availableCommands = initialized.capabilities.executeCommandProvider.commands;
  assert.ok(initialized.capabilities.renameProvider, 'JDTLS rename capability must survive the middleware.');
  send({ method: 'initialized', params: {} });
  const uri = pathToFileURL(resolve('src/main/java/ru/ticketflowplatform/eventinventoryservice/service/EventService.java')).href;
  const source = 'class Sample { private void second() {} private void nested() {} public void run() { first(); second(); } private void first() { nested(); } }';
  send({ method: 'textDocument/didOpen', params: { textDocument: { uri, languageId: 'java', version: 1, text: source } } });
  const actions = await request('textDocument/codeAction', {
    textDocument: { uri },
    range: { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } },
    context: { diagnostics: [] },
  });
  const action = actions.find(value => value.command?.command === command);
  assert.ok(action, 'The Alt+Enter code-action response must contain the call-order sorter.');
  assert.ok(commandRegistrations > 0, 'Exercise the dynamic command registration used by the real Zed client.');
  assert.ok(availableCommands.includes(command), 'Zed must not filter out the custom command after dynamic registration.');
  assert.equal(actions.some(value => /^Sort Members/.test(value.title) && value !== action), false);
  const resolved = await request('codeAction/resolve', action);
  assert.equal(resolved.command.command, command);
  await request('workspace/executeCommand', resolved.command);
  const sorted = receivedEdit.documentChanges[0].edits[0].newText;
  assert.deepEqual([...sorted.matchAll(/void\s+(\w+)\(/g)].map(match => match[1]), ['run', 'first', 'nested', 'second']);
  assert.deepEqual(receivedEdit.documentChanges[0].textDocument, { uri, version: 1 });
  await request('shutdown', null);
});
