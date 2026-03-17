package com.splitease.repository

import com.splitease.model.Bill
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface BillRepository : MongoRepository<Bill, String> {
    fun findByGroupId(groupId: String): List<Bill>
}
