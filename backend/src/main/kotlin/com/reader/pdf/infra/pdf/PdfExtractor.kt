package com.reader.pdf.infra.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class PdfExtractor {
    fun extract(inputStream: InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            stripper.getText(document)
        }
    }
}
