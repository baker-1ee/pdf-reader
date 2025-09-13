package com.reader.pdf.domain.document.service

import com.reader.pdf.infra.pdf.PdfExtractor
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class DocumentService(
    private val pdfExtractor: PdfExtractor
) {
    fun extractTextFromPdf(file: MultipartFile): String {
        return pdfExtractor.extract(file.inputStream)
    }
}
