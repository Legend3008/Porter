package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapeLg
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextField
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.ContainerType
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun ContainerDetailsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    var selectedType by remember { mutableStateOf(ContainerType.DRY_20FT) }
    var tempC by remember { mutableStateOf("") }

    val containerOptions = listOf(
        Triple(ContainerType.DRY_20FT, "20ft Standard Dry Container", "Max Payload: 28,200 kg • Vol: 33.2 m³"),
        Triple(ContainerType.DRY_40FT, "40ft Standard Dry Container", "Max Payload: 26,600 kg • Vol: 67.7 m³"),
        Triple(ContainerType.HIGH_CUBE_40FT, "40ft High Cube Container", "Max Payload: 26,460 kg • Vol: 76.3 m³"),
        Triple(ContainerType.REEFER_20FT, "20ft Refrigerated (Reefer)", "Temp range: -30°C to +30°C • Active genset"),
        Triple(ContainerType.REEFER_40FT, "40ft High Cube Reefer", "Temp range: -30°C to +30°C • Active genset"),
        Triple(ContainerType.FLAT_RACK_20FT, "20ft Flat Rack (Overdimensional)", "For heavy machinery & ODC freight")
    )

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Step 2 of 4: Container",
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Select Container",
                    style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
                )

                Text(
                    text = "Choose the ISO container specification required for your ocean or domestic freight cargo.",
                    style = BodyDefault.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(4.dp))

                containerOptions.forEach { (type, title, specs) ->
                    val isSelected = selectedType == type
                    val borderColor = if (isSelected) ActionBlue else Hairline
                    val bgColor = if (isSelected) Parchment else CanvasWhite

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, ShapeLg)
                            .background(bgColor, ShapeLg)
                            .clickable { selectedType = type }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = BodyStrong.copy(
                                        color = if (isSelected) ActionBlue else InkNearBlack
                                    )
                                )
                                Text(
                                    text = type.displayName,
                                    style = FinePrint.copy(
                                        color = if (isSelected) ActionBlue else InkMuted48
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = specs,
                                style = Caption.copy(color = InkMuted48)
                            )
                        }
                    }
                }

                if (selectedType == ContainerType.REEFER_20FT || selectedType == ContainerType.REEFER_40FT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PorterTextField(
                        value = tempC,
                        onValueChange = { tempC = it },
                        label = "Required Set Temperature (°C) *",
                        placeholder = "e.g. -18.0",
                        helperText = "Genset will maintain temperature throughout road transit"
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                PorterPrimaryButton(
                    text = "Next: Route & Addresses",
                    onClick = {
                        val tempVal = tempC.toDoubleOrNull()
                        viewModel.updateContainer(selectedType, tempVal)
                        onNext()
                    }
                )
            }
        }
    }
}
