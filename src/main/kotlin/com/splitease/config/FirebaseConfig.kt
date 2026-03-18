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
                val app = FirebaseApp.getInstance()
                logger.debug("Using existing Firebase app instance: {}", app.name)
                return app
            }

            // Prioritize JSON content from environment variable
            val credentials = if (serviceAccountJson.isNotBlank()) {
                logger.info("Initializing Firebase using FIREBASE_SERVICE_ACCOUNT_JSON content")
                try {
                    GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
                } catch (e: Exception) {
                    logger.error("Failed to parse Firebase JSON from environment variable: {}", e.message)
                    throw e
                }
            } else {
                logger.info("FIREBASE_SERVICE_ACCOUNT_JSON is empty, falling back to file: {}", serviceAccountPath)
                val serviceAccountFile = java.io.File(serviceAccountPath)
                if (!serviceAccountFile.exists()) {
                    logger.error("Firebase initialization failed: Neither FIREBASE_SERVICE_ACCOUNT_JSON env var nor file at {} exists", serviceAccountPath)
                    return null
                }
                FileInputStream(serviceAccountFile).use { 
                    GoogleCredentials.fromStream(it)
                }
            }

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            logger.info("Firebase Application initialized successfully.")
            FirebaseApp.initializeApp(options)
        } catch (e: Exception) {
            logger.error("CRITICAL: Failed to initialize Firebase App: ", e)
            null
        }
    }

}

