package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextField
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.Address
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun PickupDeliveryScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    var pickupLine1 by remember { mutableStateOf("JNPT Container Terminal 4, Gate 2") }
    var pickupCity by remember { mutableStateOf("Navi Mumbai") }
    var pickupState by remember { mutableStateOf("Maharashtra") }
    var pickupPostal by remember { mutableStateOf("400707") }
    var pickupContactName by remember { mutableStateOf("Suresh Patil") }
    var pickupContactPhone by remember { mutableStateOf("9820123456") }

    var deliveryLine1 by remember { mutableStateOf("Chakan MIDC Phase II, Plot B-12") }
    var deliveryCity by remember { mutableStateOf("Pune") }
    var deliveryState by remember { mutableStateOf("Maharashtra") }
    var deliveryPostal by remember { mutableStateOf("410501") }
    var deliveryContactName by remember { mutableStateOf("Amit Deshmukh") }
    var deliveryContactPhone by remember { mutableStateOf("9823098765") }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Step 3 of 4: Route",
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
                    text = "Origin & Destination",
                    style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
                )

                Text(
                    text = "Provide verified dispatch and delivery coordinates for trailer driver allocation.",
                    style = com.porter.core.designsystem.theme.BodyDefault.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PICKUP LOCATION (ORIGIN)",
                    style = FinePrint.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ActionBlue
                    )
                )

                PorterTextField(
                    value = pickupLine1,
                    onValueChange = { pickupLine1 = it },
                    label = "Pickup Address / Port / CFS *",
                    placeholder = "Terminal / Yard address"
                )

                PorterTextField(
                    value = pickupCity,
                    onValueChange = { pickupCity = it },
                    label = "Pickup City *",
                    placeholder = "City"
                )

                PorterTextField(
                    value = pickupPostal,
                    onValueChange = { pickupPostal = it },
                    label = "Pickup PIN Code *",
                    placeholder = "6 digits",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                PorterTextField(
                    value = pickupContactName,
                    onValueChange = { pickupContactName = it },
                    label = "Dispatch Point Contact Name *",
                    placeholder = "Yard supervisor / contact"
                )

                PorterTextField(
                    value = pickupContactPhone,
                    onValueChange = { pickupContactPhone = it },
                    label = "Dispatch Contact Phone *",
                    placeholder = "Mobile number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DELIVERY LOCATION (DESTINATION)",
                    style = FinePrint.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ActionBlue
                    )
                )

                PorterTextField(
                    value = deliveryLine1,
                    onValueChange = { deliveryLine1 = it },
                    label = "Delivery Address / Warehouse *",
                    placeholder = "Warehouse / Factory address"
                )

                PorterTextField(
                    value = deliveryCity,
                    onValueChange = { deliveryCity = it },
                    label = "Delivery City *",
                    placeholder = "City"
                )

                PorterTextField(
                    value = deliveryPostal,
                    onValueChange = { deliveryPostal = it },
                    label = "Delivery PIN Code *",
                    placeholder = "6 digits",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                PorterTextField(
                    value = deliveryContactName,
                    onValueChange = { deliveryContactName = it },
                    label = "Receiving Contact Name *",
                    placeholder = "Warehouse manager / receiver"
                )

                PorterTextField(
                    value = deliveryContactPhone,
                    onValueChange = { deliveryContactPhone = it },
                    label = "Receiving Contact Phone *",
                    placeholder = "Mobile number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                PorterPrimaryButton(
                    text = "Calculate Instant Quote",
                    onClick = {
                        val pickup = Address(
                            line1 = pickupLine1,
                            line2 = null,
                            city = pickupCity,
                            state = pickupState,
                            pincode = pickupPostal,
                            contactName = pickupContactName,
                            contactPhone = pickupContactPhone
                        )
                        val delivery = Address(
                            line1 = deliveryLine1,
                            line2 = null,
                            city = deliveryCity,
                            state = deliveryState,
                            pincode = deliveryPostal,
                            contactName = deliveryContactName,
                            contactPhone = deliveryContactPhone
                        )
                        viewModel.updateAddresses(pickup, delivery)
                        onNext()
                    }
                )
            }
        }
    }
}
