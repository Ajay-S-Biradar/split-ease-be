package com.splitease.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "users")
data class User(
    @Id val id: String? = null,
    @Indexed(unique = true) val firebaseUid: String? = null,
    var name: String? = null,
    @Indexed(unique = true) val phoneNumber: String,
    var upiId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
