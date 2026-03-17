package com.splitease.service

import com.google.firebase.auth.FirebaseAuth
import com.splitease.model.*
import com.splitease.repository.UserRepository
import com.splitease.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun verifyFirebaseToken(request: VerifyTokenRequest): AuthResponse {
        // Verify the Firebase ID token
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.firebaseToken)
        val firebaseUid = decodedToken.uid
        val phoneNumber = decodedToken.claims["phone_number"] as? String
            ?: throw IllegalArgumentException("Phone number not found in Firebase token")

        // Check if user already exists by Firebase UID
        val existingByUid = userRepository.findByFirebaseUid(firebaseUid)

        val user: User
        val isNewUser: Boolean

        if (existingByUid.isPresent) {
            user = existingByUid.get()
            isNewUser = false
            logger.info("Existing user logged in: ${user.id}")
        } else {
            // Check if there is a shadow user with this phone number (e.g. added to a group before signup)
            val existingByPhone = userRepository.findByPhoneNumber(phoneNumber)
            
            if (existingByPhone.isPresent) {
                val shadowUser = existingByPhone.get()
                // Claim the shadow user by adding the Firebase UID
                user = userRepository.save(shadowUser.copy(firebaseUid = firebaseUid))
                isNewUser = shadowUser.name == null // They are only "new" if they haven't set a name yet
                logger.info("Shadow user claimed: ${user.id} for phone: $phoneNumber")
            } else {
                // Completely new user
                user = userRepository.save(
                    User(
                        firebaseUid = firebaseUid,
                        phoneNumber = phoneNumber
                    )
                )
                isNewUser = true
                logger.info("New user created: ${user.id}")
            }
        }

        val jwtToken = jwtUtil.generateToken(user.id!!)

        return AuthResponse(
            jwtToken = jwtToken,
            userId = user.id!!,
            name = user.name,
            isNewUser = isNewUser
        )
    }

    fun updateProfile(userId: String, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.name = request.name
        request.upiId?.let { user.upiId = it }
        val updated = userRepository.save(user)

        return UserResponse(
            id = updated.id!!,
            name = updated.name,
            phoneNumber = updated.phoneNumber,
            upiId = updated.upiId,
            createdAt = updated.createdAt
        )
    }

    fun getCurrentUser(userId: String): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return UserResponse(
            id = user.id!!,
            name = user.name,
            phoneNumber = user.phoneNumber,
            upiId = user.upiId,
            createdAt = user.createdAt
        )
    }

    fun getUserById(userId: String): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }
    }

    fun toMemberInfo(user: User): MemberInfo {
        return MemberInfo(
            userId = user.id!!,
            name = user.name,
            phoneNumber = user.phoneNumber,
            upiId = user.upiId
        )
    }
}
