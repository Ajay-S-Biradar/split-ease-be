package com.splitease.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "groups")
data class Group(
    @Id val id: String? = null,
    val name: String,
    val createdBy: String,          // userId
    val members: MutableList<String> = mutableListOf(),  // list of userIds
    val createdAt: LocalDateTime = LocalDateTime.now()
)
