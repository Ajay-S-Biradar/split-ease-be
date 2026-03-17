package com.splitease.service

import com.splitease.model.*
import com.splitease.repository.GroupRepository
import com.splitease.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val authService: AuthService
) {

    fun createGroup(userId: String, request: CreateGroupRequest): GroupResponse {
        // Create the group with the creator as the first member
        val group = Group(
            name = request.name,
            createdBy = userId,
            members = mutableListOf(userId)
        )

        // Add initial members if provided
        request.members.forEach { memberInput ->
            val user = getOrCreateUser(memberInput.phoneNumber, memberInput.name)
            if (!group.members.contains(user.id)) {
                group.members.add(user.id!!)
            }
        }

        val saved = groupRepository.save(group)
        return toGroupResponse(saved)
    }

    fun getGroupsForUser(userId: String): List<GroupResponse> {
        return groupRepository.findByMembersContaining(userId)
            .map { toGroupResponse(it) }
    }

    fun getGroupById(groupId: String): Group {
        return groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("Group not found: $groupId") }
    }

    fun getGroupDetail(groupId: String): GroupResponse {
        val group = getGroupById(groupId)
        return toGroupResponse(group)
    }

    fun addMember(groupId: String, request: AddMemberRequest): GroupResponse {
        val group = getGroupById(groupId)
        val user = getOrCreateUser(request.phoneNumber, request.name)

        if (!group.members.contains(user.id)) {
            group.members.add(user.id!!)
            groupRepository.save(group)
        }

        return toGroupResponse(group)
    }

    private fun getOrCreateUser(phoneNumber: String, name: String?): User {
        val existingUser = userRepository.findByPhoneNumber(phoneNumber)
        return if (existingUser.isPresent) {
            val user = existingUser.get()
            // If user exists but has no name, and we provided one, update it
            if (user.name == null && name != null) {
                user.name = name
                userRepository.save(user)
            } else {
                user
            }
        } else {
            // Create a "shadow user" for someone who hasn't joined yet
            userRepository.save(
                User(
                    phoneNumber = phoneNumber,
                    name = name,
                    firebaseUid = null // They don't have a Firebase account yet
                )
            )
        }
    }

    private fun toGroupResponse(group: Group): GroupResponse {
        val memberInfos = group.members.mapNotNull { memberId ->
            try {
                val user = authService.getUserById(memberId)
                authService.toMemberInfo(user)
            } catch (e: Exception) {
                null
            }
        }

        return GroupResponse(
            id = group.id!!,
            name = group.name,
            createdBy = group.createdBy,
            members = memberInfos,
            createdAt = group.createdAt
        )
    }
}
