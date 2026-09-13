package com.porter.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.ShapeLg

/**
 * Porter utility card — white surface, 18dp radius, 1px hairline border.
 *
 * Design: store-utility-card pattern from design.md.
 * NO drop shadow — elevation 0dp per "no card shadows" rule.
 */
@Composable
fun PorterCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = ShapeLg,
            colors = CardDefaults.cardColors(containerColor = CanvasWhite),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,      // No shadow — design.md rule
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = ShapeLg,
            colors = CardDefaults.cardColors(containerColor = CanvasWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
        ) {
            content()
        }
    }
}

/**
 * Parchment tile card — full-bleed section, no border, no corner radius.
 * Maps to product-tile-parchment from design.md.
 */
@Composable
fun PorterSectionTile(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.porter.core.designsystem.theme.Parchment
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}
