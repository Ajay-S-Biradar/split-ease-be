package com.splitease.controller

import com.splitease.model.AuthResponse
import com.splitease.model.UpdateProfileRequest
import com.splitease.model.UserResponse
import com.splitease.model.VerifyTokenRequest
import com.splitease.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Firebase OTP verification and profile management")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/verify")
    @Operation(summary = "Verify Firebase token and get JWT", description = "Send the Firebase ID token after OTP verification. Returns a JWT for subsequent API calls.")
    fun verifyToken(@RequestBody request: VerifyTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyFirebaseToken(request)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Set or update the user's display name. Called after first OTP login.")
    fun updateProfile(
        authentication: Authentication,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val userId = authentication.principal as String
        val response = authService.updateProfile(userId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile.")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val userId = authentication.principal as String
        val response = authService.getCurrentUser(userId)
        return ResponseEntity.ok(response)
    }
}
