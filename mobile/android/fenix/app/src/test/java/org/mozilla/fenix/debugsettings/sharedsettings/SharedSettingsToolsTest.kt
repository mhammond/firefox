/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.sharedsettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedSettingsToolsTest {

    @Test
    fun `GIVEN a scalar value WHEN building a set payload THEN its JSON type is preserved`() {
        assertEquals("""{"k":42}""", buildSetPayload("k", "42"))
        assertEquals("""{"k":true}""", buildSetPayload("k", "true"))
        assertEquals("""{"k":1.5}""", buildSetPayload("k", "1.5"))
    }

    @Test
    fun `GIVEN a quoted string WHEN building a set payload THEN it is not double escaped`() {
        assertEquals("""{"k":"hi"}""", buildSetPayload("k", "\"hi\""))
    }

    @Test
    fun `GIVEN a container value WHEN building a set payload THEN it is nested unchanged`() {
        assertEquals("""{"k":{"a":1}}""", buildSetPayload("k", """{"a": 1}"""))
        assertEquals("""{"k":[1,2]}""", buildSetPayload("k", "[1,2]"))
    }

    @Test
    fun `GIVEN a null value WHEN building a set payload THEN it stores JSON null`() {
        assertEquals("""{"k":null}""", buildSetPayload("k", "null"))
    }

    @Test
    fun `GIVEN a key needing escaping WHEN building a set payload THEN the key is escaped`() {
        assertEquals("""{"a\"b":1}""", buildSetPayload("a\"b", "1"))
    }

    @Test
    fun `GIVEN blank input WHEN building a set payload THEN it is rejected`() {
        assertThrows(JSONException::class.java) { buildSetPayload("k", "") }
    }

    @Test
    fun `GIVEN trailing content WHEN building a set payload THEN it is rejected`() {
        assertThrows(JSONException::class.java) { buildSetPayload("k", "1 2") }
        assertThrows(JSONException::class.java) { buildSetPayload("k", """{"a":1} junk""") }
    }

    @Test
    fun `GIVEN input that tries to add a sibling key WHEN building a set payload THEN only the key is stored`() {
        assertEquals("""{"k":1}""", buildSetPayload("k", """1, "other": 2"""))
    }

    @Test
    fun `GIVEN a key WHEN building get keys THEN it is quoted`() {
        assertEquals("\"mykey\"", buildGetKeys("mykey"))
    }

    @Test
    fun `GIVEN a blank key WHEN building get keys THEN the whole namespace is selected`() {
        assertEquals("null", buildGetKeys(""))
        assertEquals("null", buildGetKeys("   "))
    }
}
