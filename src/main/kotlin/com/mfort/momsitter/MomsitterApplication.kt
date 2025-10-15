package com.mfort.momsitter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class MomsitterApplication

fun main(args: Array<String>) {
	runApplication<MomsitterApplication>(*args)
}
