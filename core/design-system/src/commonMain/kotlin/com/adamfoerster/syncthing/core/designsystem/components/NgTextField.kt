package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

/** Labelled single- or multi-line input in the grid style. */
@Composable
fun NgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    textStyle: TextStyle = NgTheme.type.body,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = NgTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NgSectionLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = c.text),
            cursorBrush = SolidColor(c.cyan),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .background(c.bg)
                .border(1.dp, if (isError) c.err else c.lineStrong),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = c.disabledText)
                    }
                    inner()
                }
            },
        )
        if (supportingText != null) {
            Text(supportingText, style = NgTheme.type.sub, color = if (isError) c.err else c.text2)
        }
    }
}
