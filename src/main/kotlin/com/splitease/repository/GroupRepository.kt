package com.splitease.repository

import com.splitease.model.Group
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupRepository : MongoRepository<Group, String> {
    fun findByMembersContaining(userId: String): List<Group>
}
