package com.splitease.service

import com.splitease.model.*
import com.splitease.repository.SettlementRepository
import org.springframework.stereotype.Service

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val groupService: GroupService,
    private val authService: AuthService
) {

    fun settleDebt(groupId: String, request: CreateSettlementRequest): SettlementResponse {
        val group = groupService.getGroupById(groupId)

        // Validate both users are group members
        require(group.members.contains(request.fromUserId)) {
            "User ${request.fromUserId} is not a member of this group"
        }
        require(group.members.contains(request.toUserId)) {
            "User ${request.toUserId} is not a member of this group"
        }
        require(request.fromUserId != request.toUserId) {
            "Cannot settle debt with yourself"
        }
        require(request.amount > 0) {
            "Settlement amount must be positive"
        }

        val settlement = Settlement(
            groupId = groupId,
            fromUserId = request.fromUserId,
            toUserId = request.toUserId,
            amount = request.amount
        )

        val saved = settlementRepository.save(settlement)
        return toSettlementResponse(saved)
    }

    fun getSettlementsForGroup(groupId: String): List<SettlementResponse> {
        return settlementRepository.findByGroupId(groupId)
            .map { toSettlementResponse(it) }
    }

    private fun toSettlementResponse(settlement: Settlement): SettlementResponse {
        val fromUser = authService.getUserById(settlement.fromUserId)
        val toUser = authService.getUserById(settlement.toUserId)

        return SettlementResponse(
            id = settlement.id!!,
            groupId = settlement.groupId,
            from = authService.toMemberInfo(fromUser),
            to = authService.toMemberInfo(toUser),
            amount = settlement.amount,
            settledAt = settlement.settledAt
        )
    }
}
