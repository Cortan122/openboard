// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.ColorSetting
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.default
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.colorPrefsAndResIds
import helium314.keyboard.latin.settings.getColorPrefsToHideInitially
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.Theme
import helium314.keyboard.settings.dialogs.ColorPickerDialog
import helium314.keyboard.settings.keyboardNeedsReload

@Composable
fun ColorsScreen(
    isNight: Boolean,
    onClickBack: () -> Unit
) {
    // todo:
    //  allow switching moreColors (three dot menu? dropdown / spinner for visibility?)
    //  allow save (load should be in theme selector, maybe here too)
    //   import/export should now also store theme name
    //   handle name collisions on load by simply appending a number
    //  allow editing theme name
    //  make sure import of old colors works

    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")

    val themeName = if (isNight) prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT)!!
        else prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)!!
    val moreColors = KeyboardTheme.readUserMoreColors(prefs, themeName)
    val userColors = KeyboardTheme.readUserColors(prefs, themeName)
    val shownColors = if (moreColors == 2) {
        val allColors = KeyboardTheme.readUserAllColors(prefs, themeName)
        ColorType.entries.map {
            ColorSetting(it.name, null, allColors[it] ?: it.default())
        }
    } else {
        val toDisplay = colorPrefsAndResIds.map { (colorName, resId) ->
            val cs = userColors.firstOrNull { it.name == colorName } ?: ColorSetting(colorName, true, null)
            cs.displayName = stringResource(resId)
            cs
        }
        val colorsToHide = getColorPrefsToHideInitially(prefs)
        if (moreColors == 1) toDisplay
        else toDisplay.filter { it.color != null || it.name !in colorsToHide }
    }
    fun ColorSetting.displayColor() = if (auto == true) KeyboardTheme.determineUserColor(userColors, ctx, name, isNight)
        else color ?: KeyboardTheme.determineUserColor(userColors, ctx, name, isNight)

    var chosenColor: ColorSetting? by remember { mutableStateOf(null) }
    SearchScreen(
        title = themeName,
        onClickBack = onClickBack,
        filteredItems = { search -> shownColors.filter {
            it.displayName.split(" ", "_").any { it.startsWith(search, true) }
        } },
        itemContent = { colorSetting ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { chosenColor = colorSetting }
            ) {
                Spacer(
                    modifier = Modifier
                        .background(Color(colorSetting.displayColor()), shape = CircleShape)
                        .size(50.dp)
                )
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(colorSetting.displayName)
                    if (colorSetting.auto == true)
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(stringResource(R.string.auto_user_color))
                        }
                }
                if (colorSetting.auto != null)
                    Switch(colorSetting.auto, onCheckedChange = {
                        val oldUserColors = KeyboardTheme.readUserColors(prefs, themeName)
                        val newUserColors = (oldUserColors + ColorSetting(colorSetting.name, it, colorSetting.color))
                            .reversed().distinctBy { it.displayName }
                        KeyboardTheme.writeUserColors(prefs, themeName, newUserColors)
                        keyboardNeedsReload = true
                    })
            }
        }
    )
    if (chosenColor != null) {
        val color = chosenColor!!
        ColorPickerDialog(
            onDismissRequest = { chosenColor = null },
            initialColor = color.displayColor(),
            title = color.displayName,
        ) {
            if (moreColors == 2) {
                val oldColors = KeyboardTheme.readUserAllColors(prefs, themeName)
                oldColors[ColorType.valueOf(color.name)] = it
                KeyboardTheme.writeUserAllColors(prefs, themeName, oldColors)
            } else {
                val oldUserColors = KeyboardTheme.readUserColors(prefs, themeName)
                val newUserColors = (oldUserColors + ColorSetting(color.name, false, it))
                    .reversed().distinctBy { it.displayName }
                KeyboardTheme.writeUserColors(prefs, themeName, newUserColors)
            }
            keyboardNeedsReload = true
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Theme(true) {
        Surface {
            ColorsScreen(false) { }
        }
    }
}
