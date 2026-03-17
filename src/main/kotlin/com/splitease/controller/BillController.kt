package com.splitease.controller

import com.splitease.model.*
import com.splitease.repository.BillRepository
import com.splitease.repository.ExpenseRepository
import com.splitease.service.CloudinaryService
import com.splitease.service.GeminiService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/bills")
class BillController(
    private val geminiService: GeminiService,
    private val billRepository: BillRepository,
    private val cloudinaryService: CloudinaryService,
    private val expenseRepository: ExpenseRepository
) {

    @PostMapping("/upload")
    fun uploadBill(
        @RequestParam("groupId") groupId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        return try {
            val userId = SecurityContextHolder.getContext().authentication.name
            val imageUrl = cloudinaryService.uploadImage(file)
            val bill = Bill(
                groupId = groupId,
                uploadedBy = userId,
                imageUrl = imageUrl
            )
            val savedBill = billRepository.save(bill)
            ResponseEntity.ok(savedBill)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }

    @PostMapping("/{billId}/analyse")
    fun analyseBill(
        @PathVariable billId: String,
        @RequestBody request: BillAnalysisRequest
    ): ResponseEntity<*> {
        return try {
            val bill = billRepository.findById(billId).orElseThrow { Exception("Bill not found") }
            val joinedText = request.pages.joinToString("\n--- PAGE BREAK ---\n")
            val response = geminiService.analyseBillText(joinedText)
            
            val updatedBill = bill.copy(
                items = response.items,
                taxes = response.taxes,
                subtotal = response.subtotal,
                totalAmount = response.totalAmount,
                status = BillStatus.ANALYSED,
                rawText = joinedText
            )
            billRepository.save(updatedBill)
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(mapOf("error" to (e.message ?: "Analysis failed")))
        }
    }

    @PutMapping("/{billId}/items")
    fun updateItems(
        @PathVariable billId: String,
        @RequestBody items: List<BillItem>
    ): ResponseEntity<*> {
        return try {
            val bill = billRepository.findById(billId).orElseThrow { Exception("Bill not found") }
            val updatedBill = bill.copy(
                items = items,
                subtotal = items.sumOf { it.price },
                totalAmount = items.sumOf { it.price } + bill.taxes.sumOf { it.amount }
            )
            val savedBill = billRepository.save(updatedBill)
            ResponseEntity.ok(savedBill)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(mapOf("error" to (e.message ?: "Update failed")))
        }
    }

    @PostMapping("/{billId}/split")
    fun splitBill(
        @PathVariable billId: String,
        @RequestBody request: BillSplitRequest
    ): ResponseEntity<*> {
        return try {
            val bill = billRepository.findById(billId).orElseThrow { Exception("Bill not found") }
            
            val userTotals = mutableMapOf<String, Double>()
            
            request.itemAssignments.forEach { (indexStr, userIds) ->
                val index = indexStr.toIntOrNull() ?: return@forEach
                if (index < bill.items.size && userIds.isNotEmpty()) {
                    val item = bill.items[index]
                    val partAmount = item.price / userIds.size
                    userIds.forEach { userId ->
                        userTotals[userId] = (userTotals[userId] ?: 0.0) + partAmount
                    }
                }
            }
            
            val taxpayers = userTotals.keys
            if (taxpayers.isNotEmpty()) {
                val taxPerPerson = bill.taxes.sumOf { it.amount } / taxpayers.size
                taxpayers.forEach { userId ->
                    userTotals[userId] = (userTotals[userId] ?: 0.0) + taxPerPerson
                }
            }

            val existingExpense = if (bill.expenseId != null) {
                expenseRepository.findById(bill.expenseId!!).orElse(null)
            } else {
                // Fallback: search by billId in case bill.expenseId wasn't updated
                expenseRepository.findAll().find { it.billId == billId }
            }

            val itemsDescription = if (bill.items.isNotEmpty()) {
                val firstItems = bill.items.take(2).joinToString(", ") { it.name }
                if (bill.items.size > 2) "$firstItems +${bill.items.size - 2} more" else firstItems
            } else {
                "Bill Split"
            }

            val expense = if (existingExpense != null) {
                existingExpense.copy(
                    paidBy = request.paidBy,
                    amount = bill.totalAmount,
                    description = "Split: $itemsDescription",
                    splits = userTotals.map { ExpenseSplit(it.key, it.value) },
                    billImageUrl = bill.imageUrl,
                    billId = billId
                )
            } else {
                Expense(
                    groupId = bill.groupId,
                    paidBy = request.paidBy,
                    amount = bill.totalAmount,
                    description = "Split: $itemsDescription",
                    splits = userTotals.map { ExpenseSplit(it.key, it.value) },
                    billImageUrl = bill.imageUrl,
                    billId = billId
                )
            }
            
            val savedExpense = expenseRepository.save(expense)
            
            val updatedBill = bill.copy(
                itemAssignments = request.itemAssignments,
                status = BillStatus.SPLIT,
                expenseId = savedExpense.id
            )
            billRepository.save(updatedBill)
            
            ResponseEntity.ok(savedExpense)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(mapOf("error" to (e.message ?: "Split failed")))
        }
    }

    @GetMapping("/group/{groupId}")
    fun getGroupBills(@PathVariable groupId: String): ResponseEntity<List<Bill>> {
        return ResponseEntity.ok(billRepository.findByGroupId(groupId))
    }

    @GetMapping("/{billId}")
    fun getBill(@PathVariable billId: String): ResponseEntity<Bill> {
        return billRepository.findById(billId)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }
}
