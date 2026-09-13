package com.porter.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.ShapeLg
import com.porter.core.designsystem.theme.StatusError

/**
 * Standard Porter text field.
 * Design: 18dp radius card shape, hairline border unfocused, ActionBlue focused, 17sp body text.
 */
@Composable
fun PorterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorMessage: String? = null,
    helperText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label, style = Caption) },
            placeholder = if (placeholder.isNotBlank()) {
                { Text(text = placeholder, style = BodyDefault, color = InkMuted48) }
            } else null,
            enabled = enabled,
            readOnly = readOnly,
            isError = errorMessage != null,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            textStyle = BodyDefault.copy(color = InkNearBlack),
            shape = ShapeLg,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ActionBlue,
                unfocusedBorderColor = Hairline,
                errorBorderColor = StatusError,
                focusedLabelColor = ActionBlue,
                unfocusedLabelColor = InkMuted48,
                errorLabelColor = StatusError,
                cursorColor = ActionBlue,
                focusedTextColor = InkNearBlack,
                unfocusedTextColor = InkNearBlack,
                disabledTextColor = InkMuted48,
                disabledBorderColor = Hairline.copy(alpha = 0.5f),
            )
        )

        // Error or helper text below the field
        val subText = errorMessage ?: helperText
        if (subText != null) {
            Text(
                text = subText,
                style = FinePrint,
                color = if (errorMessage != null) StatusError else InkMuted48,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
