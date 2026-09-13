package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextField
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun CreateShipmentScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    var description by remember { mutableStateOf("Precision Automotive & Engineering Components") }
    var weightKg by remember { mutableStateOf("18500") }
    var isHazardous by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Step 1 of 4: Cargo",
                onNavigateBack = onBack
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CanvasWhite)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Cargo Details",
                    style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
                )

                Text(
                    text = "Specify the shipment commodity and gross cargo weight for optimal trailer chassis matching.",
                    style = BodyDefault.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PorterTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Cargo / Commodity Description *",
                    placeholder = "e.g. Steel coils, Electronic parts, Textiles"
                )

                PorterTextField(
                    value = weightKg,
                    onValueChange = { if (it.all { c -> c.isDigit() }) weightKg = it },
                    label = "Gross Cargo Weight (in Kilograms) *",
                    placeholder = "e.g. 18500",
                    helperText = "Maximum trailer payload capacity is 28,000 kg (28 MT)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                PorterCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isHazardous,
                            onCheckedChange = { isHazardous = it },
                            colors = CheckboxDefaults.colors(checkedColor = ActionBlue)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Hazardous / Dangerous Goods (HAZMAT)",
                                style = BodyDefault.copy(color = InkNearBlack)
                            )
                            Text(
                                text = "Requires certified driver and emergency response documentation",
                                style = FinePrint.copy(color = InkMuted48)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                PorterPrimaryButton(
                    text = "Next: Container Selection",
                    onClick = {
                        val weight = weightKg.toDoubleOrNull() ?: 18500.0
                        viewModel.updateCargo(description, weight, isHazardous)
                        onNext()
                    },
                    enabled = description.isNotBlank() && weightKg.isNotBlank()
                )
            }
        }
    }
}
