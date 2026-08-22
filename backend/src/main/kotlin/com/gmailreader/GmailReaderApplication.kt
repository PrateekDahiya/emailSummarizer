package com.gmailreader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class GmailReaderApplication

fun main(args: Array<String>) {
    runApplication<GmailReaderApplication>(*args)
}