package com.mfort.momsitter.infra

import com.mfort.momsitter.domain.Message
import com.mfort.momsitter.domain.Reservation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CompletableFuture

@Component
class NotificationClient(
    @Value("\${external.message-server.endpoints.send}")
    private val sendEndpoint: String,
    private val client: WebClient,
) {
    @Async
    fun sendAsync(reservation: Reservation, message: Message): CompletableFuture<SendResult> {
        return CompletableFuture.supplyAsync {
            try {
                client.post()
                    .uri(sendEndpoint)
                    .bodyValue(
                        mapOf(
                            "phoneNumber" to message.phoneNumber,
                            "title" to message.title,
                            "contents" to message.contents
                        )
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError) { _ -> throw Retryable() }
                    .toBodilessEntity()
                    .block()

                SendResult.Success(reservation.id!!)
            } catch (ex: NonRetryable) {
                SendResult.Failure(reservation.id!!, retryable = false)
            } catch (ex: Retryable) {
                SendResult.Failure(reservation.id!!, retryable = true)
            } catch (ex: Exception) {
                SendResult.Failure(reservation.id!!, retryable = true)
            }
        }
    }
}

class Retryable() : RuntimeException()
class NonRetryable() : RuntimeException()

sealed class SendResult(open val reservationId: Long) {
    data class Success(override val reservationId: Long) : SendResult(reservationId)
    data class Failure(override val reservationId: Long, val retryable: Boolean) : SendResult(reservationId)
}
