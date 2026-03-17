package com.splitease.service

import com.splitease.model.*
import com.splitease.repository.ExpenseRepository
import com.splitease.repository.SettlementRepository
import org.springframework.stereotype.Service

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val groupService: GroupService,
    private val authService: AuthService
) {

    fun addExpense(groupId: String, request: CreateExpenseRequest): ExpenseResponse {
        val group = groupService.getGroupById(groupId)

        // Validate payer is a group member
        require(group.members.contains(request.paidBy)) {
            "Payer ${request.paidBy} is not a member of this group"
        }

        val splits: List<ExpenseSplit> = if (request.splitEqually) {
            // Split equally among all group members
            val perPerson = request.amount / group.members.size
            group.members.map { memberId ->
                ExpenseSplit(userId = memberId, amount = perPerson)
            }
        } else {
            // Use custom splits — validate they sum to total
            val customSplits = request.customSplits
                ?: throw IllegalArgumentException("Custom splits required when splitEqually is false")

            val splitTotal = customSplits.sumOf { it.amount }
            require(Math.abs(splitTotal - request.amount) < 0.01) {
                "Custom splits total ($splitTotal) doesn't match expense amount (${request.amount})"
            }

            customSplits
        }

        val expense = Expense(
            groupId = groupId,
            paidBy = request.paidBy,
            amount = request.amount,
            description = request.description,
            splits = splits,
            billImageUrl = request.billImageUrl,
            billId = request.billId
        )

        val saved = expenseRepository.save(expense)
        return toExpenseResponse(saved)
    }

    fun getExpensesForGroup(groupId: String): List<ExpenseResponse> {
        return expenseRepository.findByGroupId(groupId)
            .map { toExpenseResponse(it) }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        val expense = expenseRepository.findById(expenseId).orElseThrow { Exception("Expense not found") }
        require(expense.groupId == groupId) { "Expense does not belong to this group" }
        expenseRepository.deleteById(expenseId)
    }

    fun calculateBalances(groupId: String): List<BalanceEntry> {
        val expenses = expenseRepository.findByGroupId(groupId)
        val settlements = settlementRepository.findByGroupId(groupId)

        // Net balance map: userId -> net amount (positive = is owed, negative = owes)
        val netBalance = mutableMapOf<String, Double>()

        // Process expenses
        for (expense in expenses) {
            // Payer is owed the total amount
            netBalance[expense.paidBy] = (netBalance[expense.paidBy] ?: 0.0) + expense.amount

            // Each person in the split owes their share
            for (split in expense.splits) {
                netBalance[split.userId] = (netBalance[split.userId] ?: 0.0) - split.amount
            }
        }

        // Process settlements
        for (settlement in settlements) {
            // From paid money, so their debt decreases (net goes up)
            netBalance[settlement.fromUserId] = (netBalance[settlement.fromUserId] ?: 0.0) + settlement.amount
            // To received money, so their credit decreases (net goes down)
            netBalance[settlement.toUserId] = (netBalance[settlement.toUserId] ?: 0.0) - settlement.amount
        }

        // Simplify debts: separate into creditors and debtors
        val creditors = mutableListOf<Pair<String, Double>>() // people who are owed money
        val debtors = mutableListOf<Pair<String, Double>>()   // people who owe money

        for ((userId, balance) in netBalance) {
            when {
                balance > 0.01 -> creditors.add(userId to balance)
                balance < -0.01 -> debtors.add(userId to -balance) // store as positive
            }
        }

        // Greedy algorithm to minimize transactions
        val result = mutableListOf<BalanceEntry>()
        val sortedCreditors = creditors.sortedByDescending { it.second }.toMutableList()
        val sortedDebtors = debtors.sortedByDescending { it.second }.toMutableList()

        var i = 0
        var j = 0

        while (i < sortedCreditors.size && j < sortedDebtors.size) {
            val (creditorId, creditAmt) = sortedCreditors[i]
            val (debtorId, debtAmt) = sortedDebtors[j]

            val settleAmount = minOf(creditAmt, debtAmt)

            if (settleAmount > 0.01) {
                val fromUser = authService.getUserById(debtorId)
                val toUser = authService.getUserById(creditorId)

                result.add(
                    BalanceEntry(
                        from = authService.toMemberInfo(fromUser),
                        to = authService.toMemberInfo(toUser),
                        amount = Math.round(settleAmount * 100.0) / 100.0
                    )
                )
            }

            sortedCreditors[i] = creditorId to (creditAmt - settleAmount)
            sortedDebtors[j] = debtorId to (debtAmt - settleAmount)

            if (sortedCreditors[i].second < 0.01) i++
            if (sortedDebtors[j].second < 0.01) j++
        }

        return result
    }

    private fun toExpenseResponse(expense: Expense): ExpenseResponse {
        val payer = authService.getUserById(expense.paidBy)
        val splitDetails = expense.splits.map { split ->
            val user = authService.getUserById(split.userId)
            SplitDetail(
                user = authService.toMemberInfo(user),
                amount = split.amount
            )
        }

        return ExpenseResponse(
            id = expense.id!!,
            groupId = expense.groupId,
            paidBy = authService.toMemberInfo(payer),
            amount = expense.amount,
            description = expense.description,
            splits = splitDetails,
            billImageUrl = expense.billImageUrl,
            billId = expense.billId,
            date = expense.date
        )
    }
}
