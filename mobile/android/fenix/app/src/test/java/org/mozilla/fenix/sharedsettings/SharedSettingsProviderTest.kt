/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sharedsettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.Config

private const val DB_NAME = "shared_settings.sqlite"

@RunWith(AndroidJUnit4::class)
class SharedSettingsProviderTest {
    private lateinit var settings: SharedSettings

    @Before
    fun setUp() {
        // Unit tests run against the build type given by -PtestBuildType, which is `beta` on
        // release branches. Only Nightly and debug have a real implementation to test.
        assumeTrue(Config.channel.isNightlyOrDebug)
        testContext.getDatabasePath(DB_NAME).parentFile?.mkdirs()
        settings = requireNotNull(SharedSettingsProvider.create(testContext))
    }

    @After
    fun tearDown() {
        if (::settings.isInitialized) {
            settings.close()
        }
        testContext.getDatabasePath(DB_NAME).delete()
    }

    @Test
    fun `GIVEN a nightly or debug build WHEN creating storage THEN the component is available`() {
        assertNotNull(SharedSettingsProvider.create(testContext))
    }

    @Test
    fun `GIVEN values were set WHEN reading the whole namespace THEN they are returned`() {
        settings.set("test", """{"value": "foo"}""")

        assertEquals("foo", JSONObject(settings.get("test", "null")).getString("value"))
    }

    @Test
    fun `GIVEN values were set WHEN reading a single key THEN only that key is returned`() {
        settings.set("test", """{"first": 1, "second": 2}""")

        val read = JSONObject(settings.get("test", "\"second\""))

        assertEquals(1, read.length())
        assertEquals(2, read.getInt("second"))
    }

    @Test
    fun `GIVEN nothing was set WHEN reading a namespace THEN it is empty`() {
        assertEquals(0, JSONObject(settings.get("never-written", "null")).length())
    }

    @Test
    fun `GIVEN separate namespaces WHEN reading one THEN the other is not visible`() {
        settings.set("first", """{"value": "one"}""")
        settings.set("second", """{"value": "two"}""")

        assertEquals("one", JSONObject(settings.get("first", "null")).getString("value"))
        assertEquals("two", JSONObject(settings.get("second", "null")).getString("value"))
    }
}
