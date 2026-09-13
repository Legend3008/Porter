package com.porter.domain.model

import kotlinx.datetime.Instant

data class Quote(
    val id: String,
    val bookingDraftId: String,
    // All financial values in paise (Long) — NEVER Float/Double
    val baseFarePaise: Long,
    val fuelSurchargePaise: Long,
    val portHandlingPaise: Long,
    val otherSurchargePaise: Long,
    val subtotalPaise: Long,
    val gstPaise: Long,
    val totalAmountPaise: Long,
    val gstRate: Double,                // e.g. 18.0 for 18%
    val validUntil: Instant,            // Quote expiry — show countdown in UI
    val currency: String = "INR",
) {
    val totalFarePaise: Long get() = totalAmountPaise
    val tollChargesPaise: Long get() = otherSurchargePaise
    val terminalHandlingChargesPaise: Long get() = portHandlingPaise
}

/** Formatted display amount — converts paise to rupee string */
fun Long.toPaisesRupeeString(): String {
    val rupees = this / 100.0
    return "₹%,.2f".format(rupees)
}

data class QuoteBreakdownItem(
    val label: String,
    val amountPaise: Long,
    val isGst: Boolean = false,
)

fun Quote.breakdownItems(): List<QuoteBreakdownItem> = listOf(
    QuoteBreakdownItem("Base Fare", baseFarePaise),
    QuoteBreakdownItem("Fuel Surcharge", fuelSurchargePaise),
    QuoteBreakdownItem("Port Handling", portHandlingPaise),
    QuoteBreakdownItem("Other Surcharges", otherSurchargePaise),
    QuoteBreakdownItem("GST (${gstRate}%)", gstPaise, isGst = true),
)
