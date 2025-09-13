package com.reader.pdf.infra.pdf

import net.sourceforge.tess4j.Tesseract
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import javax.imageio.ImageIO
import java.io.File

@Component
class PdfExtractor {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val tesseract = Tesseract().apply {
        setLanguage("kor+eng")  // 한글과 영어 모두 지원
    }

    fun extract(inputStream: InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            try {
                log.info("PDF 문서 로드됨. 페이지 수: ${document.numberOfPages}")

                // 먼저 일반적인 텍스트 추출 시도
                val normalExtractedText = extractNormalText(document)

                // 추출된 텍스트가 없거나 매우 적은 경우 OCR 시도
                if (normalExtractedText.isBlank() || normalExtractedText.length < 50) {
                    log.info("일반 텍스트 추출 실패. OCR 시도 중...")
                    extractWithOcr(document)
                } else {
                    normalExtractedText
                }
            } catch (e: Exception) {
                log.error("PDF 텍스트 추출 중 오류 발생", e)
                throw e
            }
        }
    }

    private fun extractNormalText(document: PDDocument): String {
        val stripper = PDFTextStripper().apply {
            startPage = 1
            endPage = document.numberOfPages
        }
        return stripper.getText(document)
    }

    private fun extractWithOcr(document: PDDocument): String {
        val renderer = PDFRenderer(document)
        val stringBuilder = StringBuilder()

        for (pageIndex in 0 until document.numberOfPages) {
            log.info("페이지 ${pageIndex + 1} OCR 처리 중...")

            // PDF 페이지를 이미지로 변환
            val image = renderer.renderImageWithDPI(pageIndex, 300f) // 300 DPI로 렌더링

            // 임시 이미지 파일 생성
            val tempFile = File.createTempFile("pdf-page-$pageIndex", ".png")
            ImageIO.write(image, "png", tempFile)

            try {
                // OCR 수행
                val pageText = tesseract.doOCR(tempFile)
                stringBuilder.append(pageText).append("\n")
                log.debug("페이지 ${pageIndex + 1} OCR 결과 일부: ${pageText.take(100)}...")
            } finally {
                tempFile.delete() // 임시 파일 삭제
            }
        }

        return stringBuilder.toString()
    }
}
