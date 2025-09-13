package com.reader.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Test
import java.io.File
import org.assertj.core.api.Assertions.assertThat

class TestPdfGenerator {

    @Test
    fun generateTestPdf() {
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)

            PDPageContentStream(document, page).use { contentStream ->
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
                contentStream.newLineAtOffset(100f, 700f)
                contentStream.showText("Hello, PDF Reader!")
                contentStream.newLineAtOffset(0f, -20f)
                contentStream.showText("This is a test PDF file.")
                contentStream.endText()
            }

            val outputFile = File("src/test/resources/generated-test.pdf")
            outputFile.parentFile.mkdirs()
            document.save(outputFile)

            assertThat(outputFile).exists()
            println("Test PDF generated at: ${outputFile.absolutePath}")
        }
    }
}
