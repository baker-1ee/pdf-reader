package com.reader.pdf.infra.pdf

import com.google.cloud.vision.v1.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.protobuf.ByteString
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

@Component
class PdfExtractor {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${google.cloud.credentials.location}")
    private lateinit var credentialsPath: String

    fun extract(inputStream: InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            try {
                log.info("PDF 문서 처리 시작 (총 ${document.numberOfPages}페이지)")
                extractText(document)
            } catch (e: Exception) {
                log.error("PDF 텍스트 추출 실패", e)
                throw PdfExtractionException("PDF 텍스트 추출 중 오류가 발생했습니다", e)
            }
        }
    }

    private fun extractText(document: PDDocument): String {
        val normalExtractedText = extractNormalText(document)
        return if (shouldUseOcr(normalExtractedText)) {
            log.info("일반 텍스트 추출 결과 불충분, OCR 시도")
            extractWithOcr(document)
        } else {
            normalExtractedText
        }
    }

    private fun shouldUseOcr(text: String): Boolean =
        text.isBlank() || text.length < 50

    private fun extractNormalText(document: PDDocument): String =
        PDFTextStripper().apply {
            startPage = 1
            endPage = document.numberOfPages
        }.getText(document)

    private fun extractWithOcr(document: PDDocument): String {
        val renderer = PDFRenderer(document)
        val stringBuilder = StringBuilder()

        createVisionClient().use { client ->
            for (pageIndex in 0 until document.numberOfPages) {
                processPage(pageIndex, renderer, client, stringBuilder)
            }
        }

        return formatExtractedText(stringBuilder.toString())
    }

    private fun createVisionClient(): ImageAnnotatorClient {
        val credentials = ClassPathResource(credentialsPath).inputStream.use {
            GoogleCredentials.fromStream(it)
        }
        val settings = ImageAnnotatorSettings.newBuilder()
            .setCredentialsProvider { credentials }
            .build()
        return ImageAnnotatorClient.create(settings)
    }

    private fun processPage(
        pageIndex: Int,
        renderer: PDFRenderer,
        client: ImageAnnotatorClient,
        stringBuilder: StringBuilder
    ) {
        log.info("OCR 처리 중: 페이지 ${pageIndex + 1}")
        val imageBytes = convertPdfPageToImage(renderer, pageIndex)
        val response = detectText(client, imageBytes)

        response.textAnnotationsList.firstOrNull()?.let { annotation ->
            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append("\n\n")
            }
            stringBuilder.append(annotation.description)
            log.debug("OCR 완료: 페이지 ${pageIndex + 1}")
        } ?: log.warn("텍스트 검출 실패: 페이지 ${pageIndex + 1}")
    }

    private fun convertPdfPageToImage(renderer: PDFRenderer, pageIndex: Int): ByteArray {
        val image = renderer.renderImageWithDPI(pageIndex, 300f)
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "PNG", output)
            output.toByteArray()
        }
    }

    private fun detectText(client: ImageAnnotatorClient, imageBytes: ByteArray): AnnotateImageResponse {
        val visionImage = Image.newBuilder()
            .setContent(ByteString.copyFrom(imageBytes))
            .build()

        val request = AnnotateImageRequest.newBuilder()
            .addFeatures(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION))
            .setImage(visionImage)
            .build()

        val response = client.batchAnnotateImages(listOf(request))
        val firstResponse = response.responsesList[0]

        if (firstResponse.error != null && firstResponse.error.code != 0) {
            throw VisionApiException("Vision API 오류: ${firstResponse.error.message}")
        }

        return firstResponse
    }

    private fun formatExtractedText(text: String): String =
        text.replace("\\s*\\n\\s*".toRegex(), "\n")
            .replace("\\n{3,}".toRegex(), "\n\n")
            .trim()
}

class PdfExtractionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class VisionApiException(message: String) : RuntimeException(message)
