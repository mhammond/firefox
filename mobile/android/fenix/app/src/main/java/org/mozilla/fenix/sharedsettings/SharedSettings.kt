/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sharedsettings

/**
 * Storage for settings which are intended to sync across all platforms.
 *
 * Values are JSON strings, matching the API of the application-services `shared-settings` component this wraps.
 */
interface SharedSettings {
    /**
     * Store one or more key-value pairs.
     *
     * @param namespace Identifies the consumer the values belong to.
     * @param json A JSON object whose properties are the keys to store.
     */
    fun set(namespace: String, json: String)

    /**
     * Read values previously stored with [set], returning a JSON object.
     *
     * @param namespace Identifies the consumer the values belong to.
     * @param keys A JSON value selecting which keys to return: `null` for everything in the namespace, a string for a
     *   single key, an array of strings for several, or an object mapping keys to the default to use when a key is
     *   missing.
     */
    fun get(namespace: String, keys: String): String

    /** Close the underlying database connection. */
    fun close()
}
