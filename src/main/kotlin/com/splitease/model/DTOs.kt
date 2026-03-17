package com.splitease.model

import java.time.LocalDateTime

// ===== Auth DTOs =====
data class VerifyTokenRequest(val firebaseToken: String)

data class AuthResponse(
    val jwtToken: String,
    val userId: String,
    val name: String?,
    val isNewUser: Boolean
)

data class UpdateProfileRequest(
    val name: String,
    val upiId: String? = null
)

data class UserResponse(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    val upiId: String?,
    val createdAt: LocalDateTime
)

// ===== Group DTOs =====
data class CreateGroupRequest(
    val name: String,
    val members: List<MemberInput> = emptyList()
)

data class MemberInput(
    val phoneNumber: String,
    val name: String? = null
)

data class AddMemberRequest(
    val phoneNumber: String,
    val name: String? = null
)

data class GroupResponse(
    val id: String,
    val name: String,
    val createdBy: String,
    val members: List<MemberInfo>,
    val createdAt: LocalDateTime
)

data class MemberInfo(
    val userId: String,
    val name: String?,
    val phoneNumber: String,
    val upiId: String? = null
)

// ===== Expense DTOs =====
data class CreateExpenseRequest(
    val amount: Double,
    val description: String,
    val paidBy: String,                          // userId
    val splitEqually: Boolean = true,
    val customSplits: List<ExpenseSplit>? = null, // used if splitEqually = false
    val billImageUrl: String? = null,
    val billId: String? = null
)

data class ExpenseResponse(
    val id: String,
    val groupId: String,
    val paidBy: MemberInfo,
    val amount: Double,
    val description: String,
    val splits: List<SplitDetail>,
    val billImageUrl: String? = null,
    val billId: String? = null,
    val date: LocalDateTime
)

data class SplitDetail(
    val user: MemberInfo,
    val amount: Double
)

// ===== Balance DTOs =====
data class BalanceEntry(
    val from: MemberInfo,
    val to: MemberInfo,
    val amount: Double
)

// ===== Settlement DTOs =====
data class CreateSettlementRequest(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)

data class SettlementResponse(
    val id: String,
    val groupId: String,
    val from: MemberInfo,
    val to: MemberInfo,
    val amount: Double,
    val settledAt: LocalDateTime
)

// ===== Bill Analysis DTOs =====
data class BillAnalysisRequest(val pages: List<String>)

data class BillAnalysisResponse(
    val items: List<BillItem>,
    val taxes: List<TaxInfo>,
    val subtotal: Double,
    val totalAmount: Double
)

data class BillItem(
    val name: String,
    val price: Double,
    val qty: Int,
    val unitPrice: Double
)

data class TaxInfo(
    val name: String,
    val amount: Double
)

data class BillSplitRequest(
    val paidBy: String,
    val itemAssignments: Map<String, List<String>> // itemIndex as string key for easier JSON mapping if needed, or Int
)
