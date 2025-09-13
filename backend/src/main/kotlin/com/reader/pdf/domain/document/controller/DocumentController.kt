package com.reader.pdf.domain.document.controller

import com.reader.pdf.domain.document.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentService: DocumentService
) {
    @PostMapping("/extract")
    fun extractText(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        return try {
            val extractedText = documentService.extractTextFromPdf(file)
            ResponseEntity.ok(extractedText)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("PDF 처리 중 오류 발생: ${e.message}")
        }
    }
}
