package com.splitease

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SplitEaseApplication

fun main(args: Array<String>) {
    runApplication<SplitEaseApplication>(*args)
}
