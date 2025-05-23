// |reftest| shell-option(--enable-explicit-resource-management) skip-if(!(this.hasOwnProperty('getBuildConfiguration')&&getBuildConfiguration('explicit-resource-management'))||!xulRuntime.shell) -- explicit-resource-management is not enabled unconditionally, requires shell-options
// Copyright (C) 2023 Ron Buckton. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-disposablestack
description: >
  Throws a TypeError if NewTarget is undefined.
info: |
  DisposableStack ( )

  1. If NewTarget is undefined, throw a TypeError exception.
  ...
features: [explicit-resource-management]
---*/

assert.throws(TypeError, function() {
  DisposableStack();
});

reportCompare(0, 0);
