package com.splitease.repository

import com.splitease.model.User
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface UserRepository : MongoRepository<User, String> {
    fun findByFirebaseUid(firebaseUid: String): Optional<User>
    fun findByPhoneNumber(phoneNumber: String): Optional<User>
    fun findByPhoneNumberIn(phoneNumbers: List<String>): List<User>
}
