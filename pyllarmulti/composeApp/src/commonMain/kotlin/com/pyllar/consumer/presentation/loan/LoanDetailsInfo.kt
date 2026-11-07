package com.pyllar.consumer.presentation.loan

import com.pyllar.consumer.data.remote.requests.LoanDetailsDto
import com.pyllar.consumer.presentation.dashboard.LoanCardInfo
import kotlinx.datetime.LocalDate
import kotlin.math.round

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * What the loan details screen shows, worked out from what we stored when the offer was accepted. Fields the lender
 * did not send are left out rather than shown as zero. There is no disbursal date or repayment schedule: we do not
 * receive them yet.
 */
data class LoanDetailsInfo(
    val lenderName: String,
    val amount: String,
    val rows: List<Pair<String, String>>,
    val kfsUrl: String?,
    val tncUrl: String?
) {
    companion object {
        fun from(dto: LoanDetailsDto): LoanDetailsInfo? {
            val offer = dto.offer
            val principal = offer.principal ?: return null
            val rows = buildList {
                add("Loan amount" to rupees(principal))
                offer.netDisbursed?.let { add("Received in your bank" to rupees(it)) }
                offer.interestRate?.let { add("Interest rate" to "${percent(it)} a year") }
                offer.apr?.let { add("APR" to percent(it)) }
                offer.term?.takeIf { it.isNotBlank() }?.let { add("Tenure" to it) }
                repayment(offer.installment, offer.installments)?.let { add("Repayment" to it) }
                fee(offer.processingFee)?.let { add("Processing fee" to it) }
                offer.totalInterest?.let { add("Total interest" to rupees(it)) }
                offer.totalPayable?.let { add("Total to repay" to rupees(it)) }
                date(dto.appliedOn)?.let { add("Applied on" to it) }
            }
            return LoanDetailsInfo(
                lenderName = offer.lenderName,
                amount = rupees(principal),
                rows = rows,
                kfsUrl = offer.kfsUrl?.takeIf { it.isNotBlank() },
                tncUrl = offer.tncUrl?.takeIf { it.isNotBlank() }
            )
        }

        private fun rupees(v: Double) = LoanCardInfo.rupees(v)

        private fun percent(v: Double): String {
            val rounded = round(v * 100.0) / 100.0
            val integerPart = rounded.toLong()
            val fracPart = round((rounded - integerPart) * 100.0).toLong()
            return "$integerPart.${fracPart.toString().padStart(2, '0')}%"
        }

        private fun repayment(installment: Double?, count: Int?): String? = when {
            installment == null -> null
            count == 1 -> "${rupees(installment)} in one payment"
            count != null && count > 1 -> "${rupees(installment)} a month for $count months"
            else -> "${rupees(installment)} a month"
        }

        /** The provider sends fees as text ("11800", "0 INR"); show a plain rupee amount when it is one. */
        private fun fee(raw: String?): String? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            val amount = text.removeSuffix("INR").trim().toDoubleOrNull() ?: return text
            return rupees(amount)
        }

        private fun date(iso: String?): String? =
            runCatching {
                if (iso.isNullOrBlank()) return@runCatching null
                val d = LocalDate.parse(iso.take(10))
                val monthName = MONTH_NAMES.getOrNull(d.monthNumber - 1) ?: d.monthNumber.toString()
                "${d.dayOfMonth} $monthName ${d.year}"
            }.getOrNull()
    }
}

