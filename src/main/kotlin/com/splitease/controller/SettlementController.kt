package com.splitease.controller

import com.splitease.model.CreateSettlementRequest
import com.splitease.model.SettlementResponse
import com.splitease.service.SettlementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups/{groupId}")
@Tag(name = "Settlements", description = "Mark debts as paid")
class SettlementController(
    private val settlementService: SettlementService
) {

    @PostMapping("/settle")
    @Operation(summary = "Settle a debt", description = "Record a payment from one member to another. Updates the group's balance calculations.")
    fun settleDebt(
        @PathVariable groupId: String,
        @RequestBody request: CreateSettlementRequest
    ): ResponseEntity<SettlementResponse> {
        return ResponseEntity.ok(settlementService.settleDebt(groupId, request))
    }

    @GetMapping("/settlements")
    @Operation(summary = "Get settlements", description = "Returns all settlement records for the group.")
    fun getSettlements(@PathVariable groupId: String): ResponseEntity<List<SettlementResponse>> {
        return ResponseEntity.ok(settlementService.getSettlementsForGroup(groupId))
    }
}
