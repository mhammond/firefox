/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sharedsettings

import android.content.Context
import mozilla.appservices.shared_settings.SharedSettingsStore

private const val DB_NAME = "shared_settings.sqlite"

/**
 * Nightly implementation, backed by the application-services `shared-settings` component.
 *
 * Kept in sync with /debug/.../SharedSettingsProvider.kt.
 */
object SharedSettingsProvider : SharedSettingsProviderInterface {
    override val isAvailable = true

    override fun create(context: Context): SharedSettings =
        AppServicesSharedSettings(SharedSettingsStore(context.getDatabasePath(DB_NAME).absolutePath))
}

private class AppServicesSharedSettings(private val store: SharedSettingsStore) : SharedSettings {
    override fun set(namespace: String, json: String) = store.set(namespace, json)

    override fun get(namespace: String, keys: String): String = store.get(namespace, keys)

    override fun close() = store.shutdown()
}
