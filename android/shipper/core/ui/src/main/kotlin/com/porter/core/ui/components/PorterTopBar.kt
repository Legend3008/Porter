package com.porter.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.InkNearBlack

/**
 * Porter standard top app bar — center-aligned title, optional back navigation.
 * Background: CanvasWhite. Uses BodyStrong (17sp/600) for title per design.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PorterTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val backAction = onBack ?: onNavigateBack
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = BodyStrong,
                color = InkNearBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (backAction != null) {
                IconButton(onClick = backAction) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = InkNearBlack
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = CanvasWhite,
            titleContentColor = InkNearBlack,
            navigationIconContentColor = InkNearBlack,
            actionIconContentColor = ActionBlue
        ),
        modifier = modifier
    )
}
