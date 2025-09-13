package com.reader.pdf

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PdfReaderApplication

fun main(args: Array<String>) {
    runApplication<PdfReaderApplication>(*args)
}

