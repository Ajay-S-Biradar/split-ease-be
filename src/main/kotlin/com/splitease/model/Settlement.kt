package com.splitease.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "settlements")
data class Settlement(
    @Id val id: String? = null,
    val groupId: String,
    val fromUserId: String,         // who paid the debt
    val toUserId: String,           // who received the payment
    val amount: Double,
    val settledAt: LocalDateTime = LocalDateTime.now()
)
