/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sharedsettings

import android.content.Context

/**
 * Benchmark (no-op) implementation.
 *
 * See /nightly/.../SharedSettingsProvider.kt for the real implementation.
 */
object SharedSettingsProvider : SharedSettingsProviderInterface {
    /** The `shared-settings` component isn't built into benchmark builds. */
    override val isAvailable = false

    /** The `shared-settings` component isn't built into benchmark builds. */
    override fun create(context: Context): SharedSettings? = null
}
