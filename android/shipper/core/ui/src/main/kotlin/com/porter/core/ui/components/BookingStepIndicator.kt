package com.porter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.StatusSuccess

/**
 * Psychological Goal Gradient Step Indicator.
 *
 * Implements Principle 4 (Goal Gradient Effect):
 * - Displays genuine progress across booking milestones.
 * - Surfaces encouraging status near completion ("Almost there — 1 step remaining").
 * - Provides immediate clarity on "Where am I in this process?"
 */
@Composable
fun BookingStepIndicator(
    currentStep: Int,
    totalSteps: Int = 5,
    stepTitle: String,
    modifier: Modifier = Modifier
) {
    val remainingSteps = totalSteps - currentStep
    val subLabel = when {
        currentStep >= totalSteps -> "Final Step • Review"
        remainingSteps == 1 -> "Almost there • 1 step remaining"
        remainingSteps == 2 -> "Halfway through • 2 steps left"
        else -> "Step $currentStep of $totalSteps"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "Step $currentStep of $totalSteps: $stepTitle. $subLabel"
            }
    ) {
        // Step Title & Milestone Remaining Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stepTitle,
                style = BodyStrong.copy(fontSize = 14.sp, color = InkNearBlack)
            )
            Text(
                text = subLabel,
                style = FinePrint.copy(
                    fontWeight = if (remainingSteps <= 1) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (remainingSteps <= 1) StatusSuccess else InkMuted48
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Visual Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..totalSteps) {
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep
                val segmentColor = when {
                    isCompleted -> StatusSuccess
                    isCurrent -> ActionBlue
                    else -> Parchment
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segmentColor)
                )
            }
        }
    }
}
