/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.sharedsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.textfield.TextField
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.fenix.R
import org.mozilla.fenix.sharedsettings.SharedSettings
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme

private const val DEFAULT_NAMESPACE = "debug"

/** Key of the throwaway object used to parse a value in [buildSetPayload]. */
private const val VALUE_KEY = "v"

/**
 * Parses [valueText] as a JSON value and returns the JSON object `{"<key>": <value>}` that [SharedSettings.set]
 * expects.
 *
 * The value is parsed inside a throwaway object so that the parser rejects anything trailing it, then re-attached under
 * [key] so text such as `1, "other": 2` can't smuggle in extra keys. Note `org.json` is lenient: an unquoted word such
 * as `hello` parses as the string "hello".
 *
 * @throws JSONException if [valueText] is not a JSON value.
 */
internal fun buildSetPayload(key: String, valueText: String): String {
    val value = JSONObject("{${JSONObject.quote(VALUE_KEY)}:$valueText}").get(VALUE_KEY)
    return JSONObject().put(key, value).toString()
}

/**
 * Turns a key into the `keys` argument of [SharedSettings.get], which is a JSON value. A blank key becomes `null`,
 * which selects the whole namespace.
 */
internal fun buildGetKeys(key: String): String = key.ifBlank { null }?.let { JSONObject.quote(it) } ?: "null"

/**
 * Debug UI for reading and writing [SharedSettings] values.
 *
 * @param sharedSettings The storage to read from and write to.
 */
@Composable
fun SharedSettingsTools(sharedSettings: SharedSettings) {
    val viewModel: SharedSettingsToolsViewModel = viewModel()
    val invalidJson = stringResource(R.string.debug_drawer_shared_settings_invalid_json)

    Surface {
        Column(
            modifier =
                Modifier.padding(all = FirefoxTheme.layout.space.static200)
                    .verticalScroll(state = rememberScrollState())
        ) {
            TextField(
                value = viewModel.namespace,
                onValueChange = { viewModel.namespace = it },
                placeholder = DEFAULT_NAMESPACE,
                errorText = "",
                modifier = Modifier.fillMaxWidth().padding(FirefoxTheme.layout.space.static50),
                label = stringResource(R.string.debug_drawer_shared_settings_namespace_label),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = FirefoxTheme.layout.space.static100))

            GetSection(
                key = viewModel.getKey,
                result = viewModel.getResult,
                onKeyChange = { viewModel.getKey = it },
                onGetClick = { viewModel.get(sharedSettings) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = FirefoxTheme.layout.space.static100))

            SetSection(
                key = viewModel.setKey,
                value = viewModel.setValue,
                valueError = viewModel.valueError,
                result = viewModel.setResult,
                onKeyChange = { viewModel.setKey = it },
                onValueChange = { viewModel.onSetValueChange(it) },
                onSetClick = { viewModel.set(sharedSettings, invalidJson) },
            )

            // TODO - a delete section belongs here once the shared-settings component exposes one.
            // The Rust implementation has `remove` and `clear`, but neither is exported over the FFI.
        }
    }
}

@Composable
private fun GetSection(key: String, result: String, onKeyChange: (String) -> Unit, onGetClick: () -> Unit) {
    Text(
        text = stringResource(R.string.debug_drawer_shared_settings_get_header),
        style = FirefoxTheme.typography.headline7,
    )

    TextField(
        value = key,
        onValueChange = onKeyChange,
        placeholder = "",
        errorText = "",
        modifier = Modifier.fillMaxWidth().padding(FirefoxTheme.layout.space.static50),
        supportingText = stringResource(R.string.debug_drawer_shared_settings_get_key_hint),
        label = stringResource(R.string.debug_drawer_shared_settings_key_label),
    )

    Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static100))

    FilledButton(
        text = stringResource(R.string.debug_drawer_shared_settings_get_button),
        modifier = Modifier.fillMaxWidth(),
        onClick = onGetClick,
    )

    Text(
        text = result,
        modifier = Modifier.padding(FirefoxTheme.layout.space.static50),
        style = FirefoxTheme.typography.body2,
    )
}

@Composable
private fun SetSection(
    key: String,
    value: String,
    valueError: String,
    result: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onSetClick: () -> Unit,
) {
    Text(
        text = stringResource(R.string.debug_drawer_shared_settings_set_header),
        style = FirefoxTheme.typography.headline7,
    )

    TextField(
        value = key,
        onValueChange = onKeyChange,
        placeholder = "",
        errorText = "",
        modifier = Modifier.fillMaxWidth().padding(FirefoxTheme.layout.space.static50),
        label = stringResource(R.string.debug_drawer_shared_settings_key_label),
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = """{"a": 1}""",
        errorText = valueError,
        modifier = Modifier.fillMaxWidth().padding(FirefoxTheme.layout.space.static50),
        isError = valueError.isNotEmpty(),
        supportingText = stringResource(R.string.debug_drawer_shared_settings_value_hint),
        label = stringResource(R.string.debug_drawer_shared_settings_value_label),
    )

    Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static100))

    FilledButton(
        text = stringResource(R.string.debug_drawer_shared_settings_set_button),
        modifier = Modifier.fillMaxWidth(),
        onClick = onSetClick,
    )

    Text(
        text = result,
        modifier = Modifier.padding(FirefoxTheme.layout.space.static50),
        style = FirefoxTheme.typography.body2,
    )
}

/** Holds the debug UI input and results for [SharedSettingsTools]. */
class SharedSettingsToolsViewModel : ViewModel() {
    var namespace by mutableStateOf(DEFAULT_NAMESPACE)
    var getKey by mutableStateOf("")
    var getResult by mutableStateOf("")
        private set

    var setKey by mutableStateOf("")
    var setValue by mutableStateOf("")
        private set

    var valueError by mutableStateOf("")
        private set

    var setResult by mutableStateOf("")
        private set

    /** Records edits to the value field, clearing any error left over from the last attempt. */
    fun onSetValueChange(value: String) {
        setValue = value
        valueError = ""
    }

    /** Reads [getKey] from [sharedSettings], or the whole namespace when it is blank. */
    fun get(sharedSettings: SharedSettings) {
        val namespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        val keys = buildGetKeys(getKey)
        viewModelScope.launch {
            getResult = withContext(Dispatchers.IO) { runCatching { sharedSettings.get(namespace, keys) }.format() }
        }
    }

    /**
     * Writes [setValue], parsed as a JSON value, to [sharedSettings] under [setKey].
     *
     * @param sharedSettings The storage to write to.
     * @param invalidJsonMessage Shown against the value field when it doesn't hold a JSON value.
     */
    fun set(sharedSettings: SharedSettings, invalidJsonMessage: String) {
        val namespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        val json =
            try {
                buildSetPayload(setKey, setValue)
            } catch (e: JSONException) {
                valueError = invalidJsonMessage
                setResult = "error: ${e.message}"
                return
            }

        viewModelScope.launch {
            setResult =
                withContext(Dispatchers.IO) {
                    runCatching {
                        sharedSettings.set(namespace, json)
                        "stored $json"
                    }
                        .format()
                }
        }
    }
}

/**
 * Renders the outcome of a [SharedSettings] call. Failures are reported rather than thrown because [SharedSettings]
 * deliberately hides the component's exception type from its callers.
 */
private fun Result<String>.format(): String = fold(onSuccess = { it }, onFailure = { "error: $it" })

/** In-memory [SharedSettings] used by previews and tests. */
internal class FakeSharedSettings : SharedSettings {
    private val values = mutableMapOf<String, String>()

    override fun set(namespace: String, json: String) {
        values[namespace] = json
    }

    override fun get(namespace: String, keys: String): String = values[namespace] ?: "{}"

    override fun close() = Unit
}

@Preview
@Composable
private fun SharedSettingsToolsPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        SharedSettingsTools(sharedSettings = FakeSharedSettings())
    }
}
