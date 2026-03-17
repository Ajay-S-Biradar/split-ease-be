package com.splitease.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "expenses")
data class Expense(
    @Id val id: String? = null,
    val groupId: String,
    val paidBy: String,             // userId who paid
    val amount: Double,
    val description: String,
    val splits: List<ExpenseSplit>,  // how much each person owes
    val billImageUrl: String? = null,
    val billId: String? = null,
    val date: LocalDateTime = LocalDateTime.now()
)

data class ExpenseSplit(
    val userId: String,
    val amount: Double
)
