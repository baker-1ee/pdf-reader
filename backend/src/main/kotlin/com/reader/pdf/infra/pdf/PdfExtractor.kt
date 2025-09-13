package com.reader.pdf.infra.pdf

import com.google.cloud.vision.v1.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

@Component
class PdfExtractor {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${google.cloud.project-id}")
    private lateinit var projectId: String

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

        ImageAnnotatorClient.create().use { client ->
            for (pageIndex in 0 until document.numberOfPages) {
                log.info("페이지 ${pageIndex + 1} OCR 처리 중...")

                // PDF 페이지를 이미지로 변환
                val image = renderer.renderImageWithDPI(pageIndex, 300f)
                val imageBytes = convertImageToBytes(image)

                // Google Cloud Vision API 호출
                val visionImage = Image.newBuilder()
                    .setContent(com.google.protobuf.ByteString.copyFrom(imageBytes))
                    .build()

                val request = AnnotateImageRequest.newBuilder()
                    .addFeatures(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION))
                    .setImage(visionImage)
                    .build()

                val response = client.batchAnnotateImages(listOf(request))

                // OCR 결과 처리
                val textAnnotation = response.responsesList[0].textAnnotationsList.firstOrNull()
                if (textAnnotation != null) {
                    if (stringBuilder.isNotEmpty()) {
                        stringBuilder.append("\n\n")
                    }
                    stringBuilder.append(textAnnotation.description)
                    log.debug("페이지 ${pageIndex + 1} OCR 결과 일부: ${textAnnotation.description.take(200)}...")
                } else {
                    log.warn("페이지 ${pageIndex + 1}에서 텍스트가 추출되지 않았습니다.")
                }
            }
        }

        return stringBuilder.toString()
            .replace("\\s*\\n\\s*".toRegex(), "\n")  // 줄바꿈 주변 공백 정리
            .replace("\\n{3,}".toRegex(), "\n\n")    // 과도한 줄바꿈 정리
            .trim()
    }

    private fun convertImageToBytes(image: BufferedImage): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "PNG", output)
            output.toByteArray()
        }
    }
}
