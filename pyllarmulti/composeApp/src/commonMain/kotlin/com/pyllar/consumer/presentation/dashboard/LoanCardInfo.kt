package com.pyllar.consumer.presentation.dashboard

import com.pyllar.consumer.data.remote.model.dto.HomeLoanDto
import kotlin.math.round

/**
 * What the dashboard loans card shows beyond "Check eligibility": the user's offers (design 1b) or the loan they took
 * (design 1c). Null means the card stays on the "Check eligibility" layout.
 */
data class LoanCardInfo(
    val kind: Kind = Kind.OFFERS,
    val offerCount: Int,
    val lenderName: String,
    val apr: Double?,
    val principal: Double,
    val installment: Double?,
    val term: String? = null,
    val installments: Int? = null
) {
    enum class Kind { OFFERS, ACTIVE }

    val pillText: String
        get() = when {
            kind == Kind.ACTIVE -> "ACTIVE"
            offerCount == 1 -> "1 OFFER"
            else -> "$offerCount OFFERS"
        }
    val aprText: String
        get() {
            val v = apr ?: 0.0
            val rounded = round(v * 100.0) / 100.0
            val integerPart = rounded.toLong()
            val fracPart = round((rounded - integerPart) * 100.0).toLong()
            return "$integerPart.${fracPart.toString().padStart(2, '0')}%"
        }
    val amountText: String
        get() = rupees(principal) + (installment?.let { " · ${rupees(it)}/month" } ?: "")

    // The card's left value, right detail line and footer, by kind.
    val leftLabel: String get() = if (kind == Kind.ACTIVE) "Loan amount" else "Best APR"
    val leftValue: String get() = if (kind == Kind.ACTIVE) rupees(principal) else aprText
    val detailText: String
        get() = if (kind == Kind.ACTIVE) activeDetail() else amountText
    val footerHint: String get() = if (kind == Kind.ACTIVE) "Loan details and documents" else "Pick one to get the money"
    val footerAction: String? get() = if (kind == Kind.ACTIVE) "View ›" else "View offers ›"

    private fun activeDetail(): String {
        val due = installment ?: return term.orEmpty()
        val amount = if (installments == 1) "Repay ${rupees(due)}" else "${rupees(due)}/month"
        return if (term.isNullOrBlank()) amount else "$amount · $term"
    }

    companion object {
        // OFFERS_AVAILABLE: offers waiting to be picked. HANDED_OFF: the user chose one and went to the lender; the
        // design has no separate card for that yet, so it shows the same offers card.
        private val OFFER_STATUSES = setOf("OFFERS_AVAILABLE", "HANDED_OFF")

        fun from(loan: HomeLoanDto?): LoanCardInfo? {
            val best = loan?.bestOffer ?: return null
            val name = best.lenderName?.takeIf { it.isNotBlank() } ?: return null
            if (loan.status == "DISBURSED") {
                return LoanCardInfo(
                    kind = Kind.ACTIVE,
                    offerCount = 1,
                    lenderName = name,
                    apr = best.apr,
                    principal = best.principal ?: return null,
                    installment = best.installment,
                    term = best.term?.takeIf { it.isNotBlank() },
                    installments = best.installments
                )
            }
            if (loan.status !in OFFER_STATUSES || loan.offerCount <= 0) return null
            return LoanCardInfo(
                offerCount = loan.offerCount,
                lenderName = name,
                apr = best.apr ?: return null,
                principal = best.principal ?: return null,
                installment = best.installment
            )
        }

        /** ₹5,00,000 style (last three digits, then pairs), written out so it does not depend on the platform locale. */
        internal fun rupees(amount: Double): String {
            val digits = round(amount).toLong().toString()
            if (digits.length <= 3) return "₹$digits"
            val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            return "₹$head,${digits.takeLast(3)}"
        }
    }
}

