// Test that the onNativeCall hook is called when native function is
// called inside self-hosted JS.

load(libdir + 'eqArrayHelper.js');

var g = newGlobal({ newCompartment: true });
var dbg = new Debugger();
var gdbg = dbg.addDebuggee(g);

let rv = [];
dbg.onNativeCall = (callee, reason) => {
  rv.push(callee.name);
};

gdbg.executeInGlobal(`
// Built-in native function.
[1, 2, 3].map(Array.prototype.push, Array.prototype);

// Built-in native function with non-optimized species lookup in 'map'.
var arr = [1, 2, 3];
Object.setPrototypeOf(arr, Object.create(Array.prototype));
arr.map(Array.prototype.push, Array.prototype);

// Self-hosted function.
[1, 2, 3].map(String.prototype.padStart, "");

// Other native function.
[1, 2, 3].map(dateNow);
`);
assertEqArray(rv, [
  "map", "push", "push", "push",
  "create", "setPrototypeOf", "map", "get [Symbol.species]", "push", "push", "push",
  "map", "padStart", "padStart", "padStart",
  "map", "dateNow", "dateNow", "dateNow",
]);
rv = [];
gdbg.executeInGlobal(`
  // Optimized 'match' (no callContentFunction).
  var re = /a./;
  "abc".match(re);

  // Non-optimized 'match'. This calls RegExp.prototype[@@match] and getters on
  // RegExp.prototype.
  Object.setPrototypeOf(re, Object.create(RegExp.prototype));
  "abc".match(re);
`);
assertEqArray(rv, [
  "match",
  "create", "setPrototypeOf",
  "match", "[Symbol.match]",
  "get flags", "get hasIndices", "get global", "get ignoreCase", "get multiline",
  "get dotAll", "get unicode", "get unicodeSets", "get sticky",
]);
rv = [];
gdbg.executeInGlobal(`
// Nested getters called internally inside self-hosted.
const r = /a./;
r.foo = 10;
"abc".match(r);

// Setter inside self-hosted JS.
// Hook "A.length = k" in Array.from.
var ctor = function() {
  let obj = {};
  Object.defineProperty(obj, "length", { set: Array.prototype.join });
  return obj;
};
var a = [1, 2, 3];
a[Symbol.iterator] = null;
void Array.from.call(ctor, a);
`);
assertEqArray(rv, [ 
  "match", "[Symbol.match]",
  "get flags",
  "get hasIndices", "get global", "get ignoreCase", "get multiline",
  "get dotAll", "get unicode", "get unicodeSets", "get sticky",
  "call", "from", "defineProperty", "join",
]);

rv = [];
gdbg.executeInGlobal(`
var origExec = RegExp.prototype.exec;
RegExp.prototype.exec = dateNow;
try {
  (/a.b/).test("abc");
} catch (e) {} // Throws not-object-or-null.
RegExp.prototype.exec = origExec;
`);
assertEqArray(rv, ["test", "dateNow"]);
