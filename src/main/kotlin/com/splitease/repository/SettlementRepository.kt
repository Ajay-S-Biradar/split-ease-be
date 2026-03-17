package com.splitease.repository

import com.splitease.model.Settlement
import org.springframework.data.mongodb.repository.MongoRepository

interface SettlementRepository : MongoRepository<Settlement, String> {
    fun findByGroupId(groupId: String): List<Settlement>
}
