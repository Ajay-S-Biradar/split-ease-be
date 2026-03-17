package com.splitease.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryService(
    @Value("\${cloudinary.url}") private val cloudinaryUrl: String
) {
    private val cloudinary = Cloudinary(cloudinaryUrl)

    fun uploadImage(file: MultipartFile): String {
        val uploadResult = cloudinary.uploader().upload(file.bytes, ObjectUtils.emptyMap())
        return uploadResult["secure_url"] as String
    }
}
