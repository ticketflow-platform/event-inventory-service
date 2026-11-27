import { before, test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';
import { readdirSync } from 'node:fs';
import { prepareSorter } from './sorter.mjs';

let sort;
before(() => {
  const base = join(process.env.LOCALAPPDATA, 'Zed/extensions/work/java/jdtls');
  const version = readdirSync(base).filter(name => /^\d/.test(name))
    .sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))[0];
  const java = spawnSync('where.exe', ['java'], { encoding: 'utf8' }).stdout.trim().split(/\r?\n/)[0];
  sort = prepareSorter(java, join(base, version, 'plugins'));
});

function methods(source) {
  return [...source.matchAll(/\bvoid\s+(\w+)\s*\(/g)].map(match => match[1]);
}

test('public roots retain source order; private calls are visited depth first', async () => {
  const source = `class Sample {
  private void second() {}
  private void nested() {}
  public void zebra() { first(); second(); }
  private void first() { nested(); }
  public void alpha() { last(); }
  private void last() {}
}`;
  const result = await sort(source);
  assert.deepEqual(methods(result), ['zebra', 'alpha', 'first', 'nested', 'second', 'last']);
  assert.equal(await sort(result), result);
});

test('shared methods and recursion appear only once; unused private methods retain source order', async () => {
  const result = await sort(`class Sample {
  private void unusedZ() {}
  private void nested() { first(); }
  private void unusedA() {}
  private void second() { nested(); }
  private void first() { first(); nested(); }
  public void run() { first(); second(); }
}`);
  assert.deepEqual(methods(result), ['run', 'first', 'nested', 'second', 'unusedZ', 'unusedA']);
});

test('implicit public interface methods retain their order alongside explicitly public methods', async () => {
  const result = await sort(`interface Sample {
  default void zebra() { first(); }
  public default void alpha() { second(); }
  private void second() {}
  private void first() {}
}`);
  assert.deepEqual(methods(result), ['zebra', 'alpha', 'first', 'second']);
});

test('fields, initializers, constructors, comments, annotations and method bodies are preserved', async () => {
  const source = `class Sample {
  final String text = "private void fake() {}";
  // helper comment
  /** helper docs */
  @Deprecated private void helper() { System.out.println("Привет"); } // helper trailing comment
  { System.out.println("initializer"); }
  Sample() { System.out.println("constructor"); }
  public void run() { helper(); }
}`;
  const result = await sort(source);
  assert.ok(result.indexOf('public void run') < result.indexOf('@Deprecated private void helper'));
  assert.equal((result.match(/helper comment/g) ?? []).length, 1);
  assert.match(result, /helper comment\r?\n\s*\/\*\* helper docs \*\/\r?\n\s*@Deprecated private void helper\(\) \{ System\.out\.println\("Привет"\); \} \/\/ helper trailing comment/);
  assert.ok(result.includes('System.out.println("Привет");'));
  assert.ok(result.includes('helper trailing comment'));
  assert.ok(result.includes('final String text = "private void fake() {}";'));
  assert.ok(result.includes('{ System.out.println("initializer"); }'));
  assert.ok(result.includes('Sample() { System.out.println("constructor"); }'));
});

test('nested classes are sorted independently and calls on other receivers do not create edges', async () => {
  const result = await sort(`class Sample {
  private void ignored() {}
  private void local() {}
  public void run(Sample other) { other.ignored(); this.local(); }
  class Inner {
    private void z() {}
    public void run() { z(); }
  }
}`);
  assert.deepEqual(methods(result), ['run', 'local', 'ignored', 'run', 'z']);
});

test('this method references, static calls, varargs and lambdas are dependencies', async () => {
  const result = await sort(`class Sample {
  private void a() {}
  private static void b(String... values) {}
  private void c() {}
  public void run() { Runnable r = this::c; Sample.b("a", "b"); Runnable l = () -> a(); }
}`);
  assert.deepEqual(methods(result), ['run', 'c', 'b', 'a']);
});

test('overloads with different arities can be resolved; ambiguous overloads fail without output', async () => {
  const result = await sort(`class Sample {
  private void helper() {}
  private void helper(int n) {}
  public void run() { helper(1); helper(); }
}`);
  assert.match(result, /public void run[\s\S]*private void helper\(int n\)[\s\S]*private void helper\(\)/);
  await assert.rejects(sort(`class Sample {
    public void run() { helper(1); }
    private void helper(int n) {}
    private void helper(String n) {}
  }`), /Ambiguous overload helper/);
});

test('syntax errors abort sorting', async () => {
  await assert.rejects(sort('class Sample { public void broken( }'), /Line 1/);
});

test('CRLF, records, enum order and anonymous classes survive sorting', async () => {
  const source = `record Sample(int value) {
  private void helper() {}
  public void run() { helper(); }
  enum Kind { Z, A; private void helper() {} public void run() { helper(); } }
  Object object = new Object() { private void helper() {} public void run() { helper(); } };
}`.replaceAll('\n', '\r\n');
  const result = await sort(source);
  assert.deepEqual(methods(result), ['run', 'helper', 'run', 'helper', 'run', 'helper']);
  assert.ok(result.includes('enum Kind { Z, A;'));
  assert.equal(result.replaceAll('\r\n', '').includes('\n'), false);
});
