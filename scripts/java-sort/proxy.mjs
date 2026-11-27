import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { prepareSorter, upstreamProxy } from './sorter.mjs';

export const command = 'ticketflow.sortMembersByCallOrder';

export function frame(message) {
  const body = Buffer.from(JSON.stringify(message));
  return Buffer.concat([Buffer.from(`Content-Length: ${body.length}\r\n\r\n`), body]);
}

export function receive(stream, onMessage) {
  let buffer = Buffer.alloc(0);
  stream.on('data', data => {
    buffer = Buffer.concat([buffer, data]);
    for (;;) {
      const split = buffer.indexOf('\r\n\r\n');
      if (split < 0) return;
      const match = /(?:^|\r\n)Content-Length:\s*(\d+)/i.exec(buffer.subarray(0, split).toString('ascii'));
      if (!match) throw new Error('Invalid LSP frame: Content-Length is missing.');
      const end = split + 4 + Number(match[1]);
      if (buffer.length < end) return;
      const message = JSON.parse(buffer.subarray(split + 4, end).toString('utf8'));
      buffer = buffer.subarray(end);
      onMessage(message);
    }
  });
}

function offset(text, position) {
  let index = 0;
  for (let line = 0; line < position.line; line++) {
    index = text.indexOf('\n', index);
    if (index < 0) throw new Error('Invalid editor range.');
    index++;
  }
  return index + position.character;
}

export function changes(text, edits) {
  for (const edit of edits) {
    text = edit.range
      ? text.slice(0, offset(text, edit.range.start)) + edit.text + text.slice(offset(text, edit.range.end))
      : edit.text;
  }
  return text;
}

export function bridge(clientInput, clientOutput, serverInput, serverOutput, sort) {
  const documents = new Map(), actions = new Map(), requests = new Map();
  const session = randomUUID();
  let nextId = 0;
  const sendClient = message => clientOutput.write(frame(message));
  const sendServer = message => serverOutput.write(frame(message));

  async function sortMembers(message) {
    try {
      const { uri, version } = message.params.arguments[0];
      const document = documents.get(uri);
      if (!document || document.version !== version) throw new Error('The buffer changed. Open Sort Members again.');
      const original = document.text;
      const sorted = await sort(original);
      if (documents.get(uri) !== document || document.version !== version) throw new Error('The buffer changed while sorting. No edits applied.');
      if (sorted !== original) {
        const id = `ticketflow/${session}/${++nextId}`;
        const applied = new Promise((accept, reject) => requests.set(id, { accept, reject }));
        const lines = original.split('\n');
        sendClient({ jsonrpc: '2.0', id, method: 'workspace/applyEdit', params: {
          label: 'Sort Java members by call order',
          edit: { documentChanges: [{ textDocument: { uri, version }, edits: [{
            range: { start: { line: 0, character: 0 }, end: { line: lines.length - 1, character: lines.at(-1).length } },
            newText: sorted,
          }] }] },
        } });
        const result = await applied;
        if (!result?.applied) throw new Error(result?.failureReason ?? 'Zed declined the member-order edit.');
      }
      sendClient({ jsonrpc: '2.0', id: message.id, result: null });
    } catch (error) {
      sendClient({ jsonrpc: '2.0', id: message.id, error: { code: -32603, message: error.message } });
    }
  }

  receive(clientInput, message => {
    if (requests.has(message.id) && !message.method) {
      const request = requests.get(message.id);
      requests.delete(message.id);
      if (message.error) request.reject(new Error(message.error.message));
      else request.accept(message.result);
      return;
    }
    if (message.method === 'workspace/executeCommand' && message.params?.command === command) {
      void sortMembers(message);
      return;
    }
    if (message.method === 'codeAction/resolve' && message.params?.command?.command === command) {
      sendClient({ jsonrpc: '2.0', id: message.id, result: message.params });
      return;
    }
    const params = message.params;
    if (message.method === 'textDocument/didOpen') {
      documents.set(params.textDocument.uri, { text: params.textDocument.text, version: params.textDocument.version });
    } else if (message.method === 'textDocument/didChange') {
      const document = documents.get(params.textDocument.uri);
      if (document) documents.set(params.textDocument.uri, {
        text: changes(document.text, params.contentChanges), version: params.textDocument.version,
      });
    } else if (message.method === 'textDocument/didClose') {
      documents.delete(params.textDocument.uri);
    } else if (message.method === 'textDocument/codeAction') {
      actions.set(message.id, params);
    }
    sendServer(message);
  });

  receive(serverInput, message => {
    const params = actions.get(message.id);
    if (params && !message.method) {
      actions.delete(message.id);
      const uri = params.textDocument.uri;
      const document = documents.get(uri);
      const kind = 'source.sortMembers';
      const requested = params.context?.only;
      if (!message.error && uri.startsWith('file:') && uri.endsWith('.java') && document
        && (!requested || requested.some(value => kind === value || kind.startsWith(value + '.')))) {
        const result = (message.result ?? []).filter(action => !/^Sort (?:Selected )?Members\b/.test(action.title));
        result.push({ title: `Sort Members for '${basename(fileURLToPath(uri))}' (call order)`, kind,
          command: { title: 'Sort Members', command, arguments: [{ uri, version: document.version }] } });
        message.result = result;
      }
    }
    if (message.result?.capabilities) {
      const provider = message.result.capabilities.executeCommandProvider ??= { commands: [] };
      if (!provider.commands.includes(command)) provider.commands.push(command);
    }
    if (message.method === 'client/registerCapability') {
      for (const registration of message.params.registrations) {
        if (registration.method === 'workspace/executeCommand') {
          const options = registration.registerOptions ??= { commands: [] };
          if (!options.commands.includes(command)) options.commands.push(command);
        }
      }
    }
    sendClient(message);
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  try {
    const [extensionRoot, java, ...args] = process.argv.slice(2);
    if (!extensionRoot || !java) throw new Error('This proxy must be launched by Zed Java through lsp_proxy_path.');
    const jar = args[args.indexOf('-jar') + 1];
    if (!args.includes('-jar')) throw new Error('A managed JDTLS launch is required for the AST sorter.');
    const sort = prepareSorter(java, dirname(jar));
    const upstream = spawn(upstreamProxy(extensionRoot), [extensionRoot, java, ...args], { stdio: ['pipe', 'pipe', 'pipe'] });
    upstream.stderr.pipe(process.stderr);
    upstream.on('error', error => { console.error(error); process.exitCode = 1; });
    upstream.on('close', code => { process.exitCode = code ?? 1; process.stdin.destroy(); });
    process.stdin.on('end', () => upstream.stdin.end());
    process.on('SIGTERM', () => upstream.kill());
    process.on('SIGINT', () => upstream.kill());
    bridge(process.stdin, process.stdout, upstream.stdout, upstream.stdin, sort);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
