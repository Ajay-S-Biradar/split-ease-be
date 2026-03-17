package com.splitease.controller

import com.splitease.model.BalanceEntry
import com.splitease.model.CreateExpenseRequest
import com.splitease.model.ExpenseResponse
import com.splitease.service.ExpenseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups/{groupId}")
@Tag(name = "Expenses", description = "Add expenses and view balances within a group")
class ExpenseController(
    private val expenseService: ExpenseService
) {

    @PostMapping("/expenses")
    @Operation(summary = "Add expense", description = "Add a new expense to the group. Split equally among all members or provide custom splits.")
    fun addExpense(
        @PathVariable groupId: String,
        @RequestBody request: CreateExpenseRequest
    ): ResponseEntity<ExpenseResponse> {
        return ResponseEntity.ok(expenseService.addExpense(groupId, request))
    }

    @GetMapping("/expenses")
    @Operation(summary = "Get expenses", description = "Returns all expenses in the group, newest first.")
    fun getExpenses(@PathVariable groupId: String): ResponseEntity<List<ExpenseResponse>> {
        return ResponseEntity.ok(expenseService.getExpensesForGroup(groupId))
    }

    @GetMapping("/balances")
    @Operation(summary = "Get balances", description = "Calculate who owes whom in the group. Debts are simplified to minimize the number of transactions.")
    fun getBalances(@PathVariable groupId: String): ResponseEntity<List<BalanceEntry>> {
        return ResponseEntity.ok(expenseService.calculateBalances(groupId))
    }

    @DeleteMapping("/expenses/{expenseId}")
    @Operation(summary = "Delete expense", description = "Permanently removes an expense from the group.")
    fun deleteExpense(
        @PathVariable groupId: String,
        @PathVariable expenseId: String
    ): ResponseEntity<Void> {
        expenseService.deleteExpense(groupId, expenseId)
        return ResponseEntity.ok().build()
    }
}
