package com.reader.pdf.infra.pdf

import com.google.cloud.vision.v1.*
import com.google.auth.oauth2.GoogleCredentials
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
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

    @Value("\${google.cloud.credentials.location}")
    private lateinit var credentialsPath: String

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

        // Load credentials from the classpath resource
        val credentials = ClassPathResource(credentialsPath).inputStream.use { credentialsStream ->
            GoogleCredentials.fromStream(credentialsStream)
        }

        // Create ImageAnnotatorClient with explicit credentials
        val settings = ImageAnnotatorSettings.newBuilder()
            .setCredentialsProvider { credentials }
            .build()

        ImageAnnotatorClient.create(settings).use { client ->
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

                // Response 디버깅을 위한 로그 추가
                log.debug("Vision API 응답: {}", response)

                val firstResponse = response.responsesList[0]
                log.debug("첫 번째 응답 상태: {}", firstResponse.error)

                if (firstResponse.error != null && firstResponse.error.code != 0) {
                    log.error("페이지 ${pageIndex + 1}: Vision API 오류 - {}", firstResponse.error.message)
                    continue
                }

                if (firstResponse.textAnnotationsCount == 0) {
                    log.warn("페이지 ${pageIndex + 1}: 텍스트 검출 결과가 없습니다")
                    continue
                }

                val textAnnotation = firstResponse.textAnnotationsList.firstOrNull()
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
            .replace("\\s*\\n\\s*".toRegex(), "\n")
            .replace("\\n{3,}".toRegex(), "\n\n")
            .trim()
    }

    private fun convertImageToBytes(image: BufferedImage): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "PNG", output)
            output.toByteArray()
        }
    }
}
