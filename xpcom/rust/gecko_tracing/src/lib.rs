/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

//! This provides a way to direct rust tracing back into the application.
pub fn initialize_tracing() {
    use tracing_subscriber::prelude::*;
    tracing_subscriber::registry()
        // The application-services tracing-support library.
        .with(tracing_support::SimpleEventLayer)
        // More layers can be added by additional `.with(...)` statements.
        .init();
}
