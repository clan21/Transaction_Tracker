package com.example.myapplication

/**
 * One place that does all the money math, so Home, Wrap Up and the exports
 * always agree with each other.
 *
 * Rules:
 *  - Lending (borrowed): paid in Cash   -> Total Income goes down
 *                        paid in GCash  -> GCash Income goes down
 *  - Lending (returned): paid in Cash   -> Total Income goes up
 *                        paid in GCash  -> GCash Income goes up
 *  - GCash cash-in : Cash on Hand  -amount, GCash Income +amount
 *  - GCash cash-out: GCash Income  -amount, Cash on Hand +amount
 *  - GCash charge (both): added to Total Income, in cash
 *  - Remittance: paid in Cash   -> Total Income goes down
 *                paid in GCash  -> GCash Income goes down
 *
 * Cash on Hand is derived from Total Income, so it follows automatically.
 */
data class Totals(
    val income: Double,              // includes GCash charges and cash lending adjustments
    val expenses: Double,
    val personalIncome: Double,
    val personalExpenses: Double,
    val remit: Double,

    val borrowedTotal: Double,       // everything ever lent (returns do NOT reduce it)
    val borrowedOutstanding: Double, // lent and not yet returned
    val returnedTotal: Double,

    val cashIn: Double,
    val cashOut: Double,
    val gcashCharges: Double,
    val gcashIncome: Double,         // income received via GCash (personal + company)
    val gcashBalance: Double,        // gcashIncome - gcashExpenses
    val gcashExpenses: Double,

    val fuel: Double,
    val salary: Double,
    val others: Double,

    val incomeTotal: Double,         // Wrap Up Total Income: non-personal income, cash or GCash
    val collections: Double,
    val homeCollection: Double,      // Home Collection card: income before remittances
    val balance: Double,
    val cashOnHand: Double
)

fun computeTotals(
    transactions: List<Transaction>,
    lending: List<LendingEntry>,
    gcash: List<GcashTransaction>
): Totals {

    val incomeBase = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val expenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val personalIncome = transactions.filter { it.type == "personalIncome" }.sumOf { it.amount }
    val personalExpenses = transactions.filter { it.type == "personal" }.sumOf { it.amount }
    val remit = transactions.filter { it.type == "remit" }.sumOf { it.amount }

    // ---- Remittance (split by how it was paid) ----
    val remitGcash = transactions
        .filter { it.type == "remit" && it.paymentMethod == "GCash" }
        .sumOf { it.amount }
    val remitCash = remit - remitGcash

    // ---- Lending ----
    val borrowedTotal = lending.sumOf { it.amount }
    val borrowedOutstanding = lending.sumOf { it.balance }
    val returnedTotal = lending.sumOf { it.returnedAmount }

    // Carried-over balances were already deducted in the month they were lent
    val cashLent = lending
        .filter { !it.carriedOver && it.paymentMethod != "GCash" }
        .sumOf { it.amount }

    val gcashLent = lending
        .filter { !it.carriedOver && it.paymentMethod == "GCash" }
        .sumOf { it.amount }

    val returnedGcash = lending.sumOf { it.returnedGcash }
    val returnedCash = returnedTotal - returnedGcash

    // ---- GCash ----
    val cashIn = gcash.filter { it.type == GCASH_CASH_IN }.sumOf { it.amount }
    val cashOut = gcash.filter { it.type == GCASH_CASH_OUT }.sumOf { it.amount }
    val gcashCharges = gcash.sumOf { it.charge }

    val gcashMethodIncome = transactions
        .filter {
            (it.type == "income" || it.type == "personalIncome") &&
                    it.paymentMethod == "GCash"
        }.sumOf { it.amount }

    val gcashExpenses = transactions
        .filter {
            (it.type == "expense" || it.type == "personal") &&
                    it.paymentMethod == "GCash"
        }.sumOf { it.amount }

    fun category(name: String) = transactions
        .filter {
            (it.type == "expense" || it.type == "personal") &&
                    it.expenseCategory == name
        }.sumOf { it.amount }

    // Total Income carries the GCash charges, the cash side of lending
    // and the cash side of remittances
    val income = incomeBase + gcashCharges - cashLent + returnedCash - remitCash

    // Wrap Up formulas (cash and GCash combined):
    //   earned = income + personal income + GCash charges (cash-in and cash-out)
    //   Total Collections = earned - (expenses + personal expenses)
    //   Cash on Hand      = earned - (expenses + personal expenses + remittances)
    val earned = incomeBase + personalIncome + gcashCharges
    val wrapCollections = earned - (expenses + personalExpenses)
    val wrapCashOnHand = wrapCollections - remit

    return Totals(
        income = income,
        expenses = expenses,
        personalIncome = personalIncome,
        personalExpenses = personalExpenses,
        remit = remit,
        borrowedTotal = borrowedTotal,
        borrowedOutstanding = borrowedOutstanding,
        returnedTotal = returnedTotal,
        cashIn = cashIn,
        cashOut = cashOut,
        gcashCharges = gcashCharges,
        gcashIncome = gcashMethodIncome,
        gcashBalance = gcashMethodIncome - gcashExpenses,
        gcashExpenses = gcashExpenses,
        fuel = category("Fuel"),
        salary = category("Salary"),
        // company expenses only (personal ones are not counted here)
        others = transactions
            .filter { it.type == "expense" && it.expenseCategory == "Others" }
            .sumOf { it.amount },
        incomeTotal = incomeBase,
        collections = wrapCollections,
        // Home "Collection": the same as Total Income but with remittances added back,
        // so remitting money never lowers it. Wrap Up's Total Income still deducts them.
        homeCollection = income + remitCash,
        // Home Balance = Collection - Expenses
        balance = (income + remitCash) - expenses,
        cashOnHand = wrapCashOnHand
    )
}