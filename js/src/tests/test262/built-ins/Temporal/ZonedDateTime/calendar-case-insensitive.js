// |reftest| shell-option(--enable-temporal) skip-if(!this.hasOwnProperty('Temporal')||!xulRuntime.shell) -- Temporal is not enabled unconditionally, requires shell-options
// Copyright (C) 2022 Igalia, S.L. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-temporal.zoneddatetime
description: Calendar names are case-insensitive
features: [Temporal]
---*/

let arg = "iSo8601";

const result = new Temporal.ZonedDateTime(0n, "UTC", arg);
assert.sameValue(result.calendarId, "iso8601", "Calendar is case-insensitive");

arg = "\u0130SO8601";
assert.throws(
  RangeError,
  () => new Temporal.ZonedDateTime(0n, "UTC", arg),
  "calendar ID is capital dotted I is not lowercased"
);

reportCompare(0, 0);
