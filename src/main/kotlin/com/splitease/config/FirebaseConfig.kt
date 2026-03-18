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
class FirebaseConfig {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Value("\${firebase.service-account-path:firebase-service-account.json}")
    private lateinit var serviceAccountPath: String

    @Value("\${firebase.service-account-json:}")
    private lateinit var serviceAccountJson: String

    @Bean
    fun firebaseApp(): FirebaseApp? {
        logger.info("Starting Firebase initialization...")
        
        try {
            if (FirebaseApp.getApps().isNotEmpty()) {
                val existingApp = FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                if (existingApp != null) {
                    logger.info("Using existing default FirebaseApp: {}", existingApp.name)
                    return existingApp
                }
            }

            val credentials = if (serviceAccountJson.isNullOrBlank()) {
                logger.info("FIREBASE_SERVICE_ACCOUNT_JSON is not set, trying file: {}", serviceAccountPath)
                val file = java.io.File(serviceAccountPath)
                if (!file.exists()) {
                    logger.warn("Firebase credentials not found (env var empty and file {} missing).", serviceAccountPath)
                    return null
                }
                FileInputStream(file).use { GoogleCredentials.fromStream(it) }
            } else {
                logger.info("Initializing Firebase from FIREBASE_SERVICE_ACCOUNT_JSON env var (length: {})", serviceAccountJson.length)
                try {
                    GoogleCredentials.fromStream(serviceAccountJson.trim().byteInputStream(Charsets.UTF_8))
                } catch (e: Exception) {
                    logger.error("FAILED to parse service account JSON string: {}", e.message)
                    throw e
                }
            }

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            val app = FirebaseApp.initializeApp(options)
            logger.info("FirebaseApp '{}' initialized successfully.", app.name)
            return app
        } catch (e: Exception) {
            logger.error("CRITICAL: Firebase initialization error", e)
            // Still returning null instead of throwing to allow app to start if Firebase is optional
            return null
        }
    }
}

}

