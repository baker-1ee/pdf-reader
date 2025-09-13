package com.reader.pdf.infra.pdf

import net.sourceforge.tess4j.Tesseract
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import java.io.File
import kotlin.math.min

@Component
class PdfExtractor {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val tesseract by lazy {
        System.setProperty("jna.library.path", "/opt/homebrew/lib")
        Tesseract().apply {
            setDatapath("/opt/homebrew/share/tessdata")
            setLanguage("kor")  // 한글만 우선 적용

            // OCR 최적화 설정
            setVariable("tessedit_pageseg_mode", "1")  // PSM_AUTO
            setVariable("preserve_interword_spaces", "1")
            setVariable("tessedit_ocr_engine_mode", "3")  // LSTM
            setVariable("debug_file", "/tmp/tesseract.log")

            // 이미지 전처리 관련 설정
            setVariable("textord_min_linesize", "2.5")  // 작은 텍스트 감지 개선
            setVariable("edges_max_children_per_outline", "40")  // 문자 윤곽선 감지 개선
            setVariable("tosp_threshold_bias2", "1")  // 자간 조정
        }
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

    private fun preprocessImage(image: BufferedImage): BufferedImage {
        val maxDimension = 3000  // 해상도 증가
        val scale = if (image.width > maxDimension || image.height > maxDimension) {
            min(maxDimension.toDouble() / image.width, maxDimension.toDouble() / image.height)
        } else {
            1.0
        }

        val scaledWidth = (image.width * scale).toInt()
        val scaledHeight = (image.height * scale).toInt()

        // 그레이스케일 이미지 생성
        val scaledImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY)
        val g = scaledImage.createGraphics()
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null)
        g.dispose()

        // 대비 향상
        val enhanced = enhanceContrast(scaledImage)
        // 노이즈 제거
        removeNoise(enhanced)

        return enhanced
    }

    private fun enhanceContrast(image: BufferedImage): BufferedImage {
        val raster = image.raster
        val pixels = raster.getPixels(0, 0, image.width, image.height, null as IntArray?)

        // 히스토그램 스트레칭을 위한 최소/최대값 찾기
        var min = 255
        var max = 0
        for (i in pixels.indices) {
            min = min.coerceAtMost(pixels[i])
            max = max.coerceAtLeast(pixels[i])
        }

        // 대비 향상
        val range = max - min
        if (range > 0) {
            for (i in pixels.indices) {
                pixels[i] = ((pixels[i] - min) * 255.0 / range).toInt().coerceIn(0, 255)
            }
        }

        raster.setPixels(0, 0, image.width, image.height, pixels)
        return image
    }

    private fun removeNoise(image: BufferedImage) {
        val width = image.width
        val height = image.height
        val raster = image.raster

        // 중간값 필터 적용
        val pixels = raster.getPixels(0, 0, width, height, null as IntArray?)
        val result = IntArray(pixels.size)
        val window = IntArray(9)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var idx = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        window[idx++] = pixels[(y + dy) * width + (x + dx)]
                    }
                }
                window.sort()
                result[y * width + x] = window[4]  // 중간값 선택
            }
        }

        raster.setPixels(0, 0, width, height, result)
    }

    private fun extractWithOcr(document: PDDocument): String {
        val renderer = PDFRenderer(document)
        val stringBuilder = StringBuilder()

        for (pageIndex in 0 until document.numberOfPages) {
            log.info("페이지 ${pageIndex + 1} OCR 처리 중...")

            val image = renderer.renderImageWithDPI(pageIndex, 400f)
            val processedImage = preprocessImage(image)
            val tempFile = File.createTempFile("pdf-page-$pageIndex", ".png")
            ImageIO.write(processedImage, "png", tempFile)

            try {
                val pageText = tesseract.doOCR(tempFile)
                    // 1. 기본 정리
                    .replace("[\\t\\f\\r]".toRegex(), "")

                    // 2. 특수문자 정규화
                    .replace("\\d{2,3}%".toRegex()) { match ->  // 퍼센트 표시 정규화
                        match.value.replace(" ", "")
                    }
                    .replace("[\\[\\(]\\s*[Hh]\\s*\\d+\\s*[\\]\\)]".toRegex()) { match ->  // [H22] 형태 정규화
                        match.value.replace(" ", "")
                    }

                    // 3. 한글 문장 처리
                    .replace("([가-힣]) +([가-힣])".toRegex()) { match ->
                        val prev = match.groupValues[1]
                        val next = match.groupValues[2]
                        // 의미 단위로 띄어쓰기 유지
                        if (isWordBoundary(prev, next)) {
                            "$prev $next"
                        } else {
                            "$prev$next"
                        }
                    }

                    // 4. 문장 부호 처리
                    .replace(" +([.,?!])".toRegex(), "$1")
                    .replace("([.,?!])(?=[가-힣a-zA-Z0-9])".toRegex(), "$1 ")

                    // 5. 줄바꿈 처리
                    .replace("\n{2,}".toRegex(), "\n")
                    .replace("([가-힣a-zA-Z0-9])\n([가-힣a-zA-Z0-9])".toRegex()) { match ->
                        val prev = match.groupValues[1]
                        val next = match.groupValues[2]
                        if (isSentenceEnd(prev)) {
                            "$prev\n\n$next"
                        } else {
                            "$prev $next"
                        }
                    }

                    // 6. 최종 정리
                    .replace(" {2,}".toRegex(), " ")
                    .replace("\\n{3,}".toRegex(), "\n\n")
                    .trim()

                if (pageText.isNotBlank()) {
                    if (stringBuilder.isNotEmpty()) {
                        stringBuilder.append("\n\n")
                    }
                    stringBuilder.append(pageText)
                }
            } catch (e: Exception) {
                log.error("페이지 ${pageIndex + 1} OCR 처리 중 오류 발생", e)
            } finally {
                tempFile.delete()
            }
        }

        return stringBuilder.toString()
            .replace("\\s*\\n\\s*".toRegex(), "\n")  // 줄바꿈 주변 공백 정리
            .replace("\\n{3,}".toRegex(), "\n\n")    // 과도한 줄바꿈 정리
            .trim()
    }

    private fun isWordBoundary(prev: String, next: String): Boolean {
        // 단어 경계에서 띄어쓰기를 유지할 조합들
        val boundaryWords = listOf(
            "장인", "정신", "원칙", "패턴", "기법", "경험", "지식", "실전", "이론",
            "자전거", "타기", "코드", "분석", "설계", "구현", "사례", "연구",
            "결정", "단계", "부분", "시간", "노력", "가치"
        )
        return boundaryWords.any { prev.endsWith(it) || next.startsWith(it) }
    }

    private fun isSentenceEnd(text: String): Boolean {
        return text.endsWith(".") || text.endsWith("?") || text.endsWith("!") ||
                text.endsWith("다.") || text.endsWith("까?") || text.endsWith("라.") ||
                text.endsWith("든다.") || text.endsWith("는다.") || text.endsWith("한다.")
    }

    private fun Char.isHangul(): Boolean {
        return this in '가'..'힣'
    }
}
