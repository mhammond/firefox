// |reftest| shell-option(--enable-temporal) skip-if(!this.hasOwnProperty('Temporal')||!xulRuntime.shell) -- Temporal is not enabled unconditionally, requires shell-options
// Copyright (C) 2022 Igalia, S.L. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-temporal.plaindate.prototype.equals
description: Various forms of calendar annotation; critical flag has no effect
features: [Temporal]
---*/

const tests = [
  ["2000-05-02[u-ca=iso8601]", "without time or time zone"],
  ["2000-05-02[UTC][u-ca=iso8601]", "with time zone and no time"],
  ["2000-05-02T15:23[u-ca=iso8601]", "without time zone"],
  ["2000-05-02T15:23[UTC][u-ca=iso8601]", "with time zone"],
  ["2000-05-02T15:23[!u-ca=iso8601]", "with ! and no time zone"],
  ["2000-05-02T15:23[UTC][!u-ca=iso8601]", "with ! and time zone"],
  ["2000-05-02T15:23[u-ca=iso8601][u-ca=discord]", "second annotation ignored"],
];

const instance = new Temporal.PlainDate(2000, 5, 2);

tests.forEach(([arg, description]) => {
  const result = instance.equals(arg);

  assert.sameValue(
    result,
    true,
    `calendar annotation (${description})`
  );
});

reportCompare(0, 0);
