package com.example.pdfreader

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@RestController
@RequestMapping("/api")
class PdfController {
    @PostMapping("/upload")
    fun uploadPdf(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        return try {
            val text = extractTextFromPdf(file.inputStream)
            ResponseEntity.ok(text)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("PDF 처리 중 오류 발생: ${e.message}")
        }
    }

    private fun extractTextFromPdf(inputStream: InputStream): String {
        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        }
    }
}

