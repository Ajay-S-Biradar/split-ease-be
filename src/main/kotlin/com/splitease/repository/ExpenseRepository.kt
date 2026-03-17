package com.splitease.repository

import com.splitease.model.Expense
import org.springframework.data.mongodb.repository.MongoRepository

interface ExpenseRepository : MongoRepository<Expense, String> {
    fun findByGroupId(groupId: String): List<Expense>
}
