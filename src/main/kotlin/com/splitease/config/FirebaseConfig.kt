package com.splitease.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.io.FileNotFoundException

@Configuration
class FirebaseConfig(
    @Value("\${firebase.service-account-path}") private val serviceAccountPath: String,
    @Value("\${firebase.service-account-json:}") private val serviceAccountJson: String
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp? {
        return try {
            if (FirebaseApp.getApps().isNotEmpty()) {
                return FirebaseApp.getInstance()
            }

            val credentials = if (serviceAccountJson.isNotBlank()) {
                logger.info("Initializing Firebase using service account JSON from environment variable")
                GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
            } else {
                logger.info("Initializing Firebase using service account file at: {}", serviceAccountPath)
                val serviceAccount = FileInputStream(serviceAccountPath)
                GoogleCredentials.fromStream(serviceAccount)
            }

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            FirebaseApp.initializeApp(options)
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase: {}", e.message)
            null
        }
    }
}

