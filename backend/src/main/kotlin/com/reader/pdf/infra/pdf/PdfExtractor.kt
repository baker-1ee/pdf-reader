package com.reader.pdf.infra.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class PdfExtractor {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun extract(inputStream: InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            try {
                log.info("PDF 문서 로드됨. 페이지 수: ${document.numberOfPages}")
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = document.numberOfPages
                }
                val text = stripper.getText(document)
                log.info("추출된 텍스트 길이: ${text.length}")
                if (text.isBlank()) {
                    log.warn("추출된 텍스트가 비어있습니다.")
                } else {
                    log.debug("추출된 텍스트 일부: ${text.take(100)}...")
                }
                text
            } catch (e: Exception) {
                log.error("PDF 텍스트 추출 중 오류 발생", e)
                throw e
            }
        }
    }
}
