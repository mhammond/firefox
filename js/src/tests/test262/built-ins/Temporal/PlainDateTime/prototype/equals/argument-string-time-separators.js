// |reftest| shell-option(--enable-temporal) skip-if(!this.hasOwnProperty('Temporal')||!xulRuntime.shell) -- Temporal is not enabled unconditionally, requires shell-options
// Copyright (C) 2022 Igalia, S.L. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-temporal.plaindatetime.prototype.equals
description: Time separator in string argument can vary
features: [Temporal]
---*/

const tests = [
  ["1976-11-18T15:23", "uppercase T"],
  ["1976-11-18t15:23", "lowercase T"],
  ["1976-11-18 15:23", "space between date and time"],
];

const instance = new Temporal.PlainDateTime(1976, 11, 18, 15, 23);

tests.forEach(([arg, description]) => {
  const result = instance.equals(arg);

  assert.sameValue(
    result,
    true,
    `variant time separators (${description})`
  );
});

reportCompare(0, 0);
