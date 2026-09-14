package com.porter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapeXl
import com.porter.core.designsystem.theme.StatusSuccess
import kotlinx.coroutines.delay

/**
 * Psychological Labor Illusion Staged Loader.
 *
 * Implements Principle 6 (Labor Illusion) & Principle 17 (System Status Visibility):
 * - Replaces ambiguous, frustrating blank spinners with genuine, staged operational milestones.
 * - Shows users the concrete work the system is performing (route calculation, toll estimation, price lock).
 * - Reduces perceived wait time and significantly increases confidence in the calculation's accuracy.
 */
@Composable
fun StagedLaborLoader(
    title: String = "Calculating Guaranteed Rate",
    stages: List<String> = listOf(
        "Checking port terminal & ICD route coordinates",
        "Calculating freight rate, expressway toll & fuel surcharge",
        "Securing 15-minute price lock guarantee"
    ),
    footnote: String = "All container transport rates are guaranteed with zero hidden fees.",
    modifier: Modifier = Modifier
) {
    var activeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(stages) {
        for (i in stages.indices) {
            activeIndex = i
            delay(1200L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Parchment.copy(alpha = 0.6f), ShapeXl)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ActionBlue.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = BodyStrong.copy(fontSize = 20.sp, color = InkNearBlack),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Staged Milestones
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                stages.forEachIndexed { index, stageText ->
                    val isDone = index < activeIndex
                    val isCurrent = index == activeIndex

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status indicator icon
                        if (isDone) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(StatusSuccess, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = CanvasWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else if (isCurrent) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = ActionBlue
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Parchment, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(InkMuted48.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = stageText,
                            style = if (isCurrent) BodyStrong.copy(fontSize = 14.sp, color = InkNearBlack)
                            else if (isDone) BodyDefault.copy(fontSize = 14.sp, color = InkNearBlack)
                            else Caption.copy(fontSize = 14.sp, color = InkMuted48)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = footnote,
                style = FinePrint.copy(color = InkMuted48),
                textAlign = TextAlign.Center
            )
        }
    }
}
