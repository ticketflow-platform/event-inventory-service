import { spawn, spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { delimiter, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

export function upstreamProxy(extensionRoot) {
  const binary = process.platform === 'win32' ? 'java-lsp-proxy.exe' : 'java-lsp-proxy';
  const versions = readdirSync(join(extensionRoot, 'bin'))
    .sort((a, b) => b.localeCompare(a, undefined, { numeric: true }));
  const path = versions.map(version => join(extensionRoot, 'bin', version, binary)).find(existsSync);
  if (!path) throw new Error('The original Zed Java proxy is not installed. Restore the default Java extension first.');
  return path;
}

export function prepareSorter(java, plugins) {
  const source = join(root, 'scripts/java-sort/SortMembers.java');
  const signature = createHash('sha256').update(readFileSync(source)).update(java).update(plugins).digest('hex');
  const classes = join(root, 'build/java-member-order', signature);
  const stamp = join(classes, 'ready');
  if (!existsSync(stamp)) {
    mkdirSync(classes, { recursive: true });
    const javac = join(dirname(java), process.platform === 'win32' ? 'javac.exe' : 'javac');
    const compile = spawnSync(javac, ['-encoding', 'UTF-8', '-cp', join(plugins, '*'), '-d', classes, source], { encoding: 'utf8' });
    if (compile.error || compile.status !== 0) throw new Error(compile.error?.message ?? compile.stderr);
    writeFileSync(stamp, signature);
  }
  return sourceText => new Promise((accept, reject) => {
    const process = spawn(java, ['-cp', classes + delimiter + join(plugins, '*'), 'SortMembers'], { stdio: ['pipe', 'pipe', 'pipe'] });
    const output = [], errors = [];
    process.stdout.on('data', data => output.push(data));
    process.stderr.on('data', data => errors.push(data));
    process.on('error', reject);
    process.stdin.on('error', reject);
    process.on('close', code => code === 0
      ? accept(Buffer.concat(output).toString('utf8'))
      : reject(new Error(Buffer.concat(errors).toString('utf8'))));
    process.stdin.end(sourceText, 'utf8');
  });
}
