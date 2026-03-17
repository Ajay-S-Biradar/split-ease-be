package com.splitease.controller

import com.splitease.service.CloudinaryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/upload")
class UploadController(private val cloudinaryService: CloudinaryService) {

    @PostMapping
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        return try {
            val url = cloudinaryService.uploadImage(file)
            ResponseEntity.ok(mapOf("url" to url))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to (e.message ?: "Upload failed")))
        }
    }
}
