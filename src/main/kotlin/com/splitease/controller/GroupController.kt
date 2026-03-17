package com.splitease.controller

import com.splitease.model.AddMemberRequest
import com.splitease.model.CreateGroupRequest
import com.splitease.model.GroupResponse
import com.splitease.service.GroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups", description = "Create and manage expense groups")
class GroupController(
    private val groupService: GroupService
) {

    @GetMapping
    @Operation(summary = "Get my groups", description = "Returns all groups the authenticated user belongs to.")
    fun getMyGroups(authentication: Authentication): ResponseEntity<List<GroupResponse>> {
        val userId = authentication.principal as String
        return ResponseEntity.ok(groupService.getGroupsForUser(userId))
    }

    @PostMapping
    @Operation(summary = "Create a group", description = "Create a new expense group. The creator is automatically added as a member.")
    fun createGroup(
        authentication: Authentication,
        @RequestBody request: CreateGroupRequest
    ): ResponseEntity<GroupResponse> {
        val userId = authentication.principal as String
        return ResponseEntity.ok(groupService.createGroup(userId, request))
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group details", description = "Returns group info including all members.")
    fun getGroupDetail(@PathVariable groupId: String): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(groupService.getGroupDetail(groupId))
    }

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Add member to group", description = "Add a user to the group by their phone number. The user must be registered.")
    fun addMember(
        @PathVariable groupId: String,
        @RequestBody request: AddMemberRequest
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(groupService.addMember(groupId, request))
    }
}
