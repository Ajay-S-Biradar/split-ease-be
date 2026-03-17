package com.splitease.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "bills")
data class Bill(
    @Id val id: String? = null,
    val groupId: String,
    val uploadedBy: String, // userId
    val imageUrl: String,
    val items: List<BillItem> = emptyList(),
    val taxes: List<TaxInfo> = emptyList(),
    val subtotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val itemAssignments: Map<String, List<String>> = emptyMap(), // itemIndex -> list of userIds
    val status: BillStatus = BillStatus.UPLOADED,
    val rawText: String? = null,
    val expenseId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class BillStatus {
    UPLOADED,
    ANALYSED,
    SPLIT
}
