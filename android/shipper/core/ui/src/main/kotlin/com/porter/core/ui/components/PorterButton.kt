package com.porter.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.ShapeSm

/**
 * Primary blue pill CTA — the signature Porter action.
 *
 * Design tokens: background=ActionBlue, shape=ShapePill, text=BodyDefault (17sp/400).
 * Press state: transform scale(0.95) per design.md micro-interaction rule.
 */
@Composable
fun PorterPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "button_scale"
    )

    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled && !loading,
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = ActionBlue,
            contentColor = CanvasWhite,
            disabledContainerColor = ActionBlue.copy(alpha = 0.4f),
            disabledContentColor = CanvasWhite.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
        interactionSource = interactionSource
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CanvasWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = BodyDefault)
        }
    }
}

/**
 * Secondary ghost pill CTA — transparent background with Action Blue border.
 * Used as the "Learn more" companion when two pills appear together.
 */
@Composable
fun PorterSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "secondary_button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        shape = ShapePill,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ActionBlue,
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
        interactionSource = interactionSource
    ) {
        Text(text = text, style = BodyDefault, color = ActionBlue)
    }
}

/**
 * Dark utility button — used for secondary actions (compact, 8dp radius).
 * Maps to design.md button-dark-utility: bg=InkNearBlack, shape=ShapeSm.
 */
@Composable
fun PorterUtilityButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "utility_button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        shape = ShapeSm,
        colors = ButtonDefaults.buttonColors(
            containerColor = InkNearBlack,
            contentColor = CanvasWhite,
        ),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
        interactionSource = interactionSource
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = CanvasWhite)
    }
}

/**
 * Text-only link button — Action Blue, no background.
 */
@Composable
fun PorterTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = ActionBlue)
    ) {
        Text(text = text, style = BodyDefault, color = ActionBlue)
    }
}
