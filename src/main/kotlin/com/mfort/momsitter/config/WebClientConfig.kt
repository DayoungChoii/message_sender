package com.mfort.momsitter.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    @Value("\${external.message-server.url}")
    private val messageServerUrl: String,
) {
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        return builder
            .baseUrl("http://localhost:8010") // 메시징 서버 주소
            .build()
    }
}