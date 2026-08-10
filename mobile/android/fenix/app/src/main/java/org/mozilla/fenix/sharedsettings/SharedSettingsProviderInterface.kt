/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sharedsettings

import android.content.Context

/**
 * Creates the [SharedSettings] for the current release channel.
 *
 * The application-services component backing [SharedSettings] is still experimental, so it is
 * only built into Nightly and debug builds. This indirection keeps the rest of the app free of
 * per-channel conditionals and of the component's types.
 */
interface SharedSettingsProviderInterface {
    /**
     * Returns storage backed by the `shared-settings` component, or `null` on channels where
     * the component isn't available.
     *
     * @param context The context used to locate the database.
     */
    fun create(context: Context): SharedSettings?
}
