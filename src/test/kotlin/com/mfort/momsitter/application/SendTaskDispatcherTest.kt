package com.mfort.momsitter.application

import com.mfort.momsitter.domain.Message
import com.mfort.momsitter.fixture.Fixture
import com.mfort.momsitter.infra.NotificationClient
import com.mfort.momsitter.infra.SendResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.concurrent.CompletableFuture

class SendTaskDispatcherTest {
    private val notificationClient = mock(NotificationClient::class.java)
    private val dispatcher = SendTaskDispatcher(notificationClient)

    private val messageId = 1L
    private val reservationId = 10L

    @Test
    fun `예약과 메시지를 정상적으로 비동기 전송`() {
        // given
        val message = Fixture.getMessage(messageId)
        val reservation = Fixture.getReservation(messageId, reservationId)

        `when`(notificationClient.sendAsync(reservation, message))
            .thenReturn(CompletableFuture.completedFuture(SendResult.Success(reservation.id!!)))

        // when
        val results = dispatcher.dispatch(listOf(reservation), mapOf(message.id!! to message))

        // then
        assertThat(results).hasSize(1)
        assertThat(results.first().get()).isNotNull
    }

    @Test
    fun `messageMap에 해당 메시지가 없으면 아무것도 실행하지 않음`() {
        // given
        val reservation = Fixture.getReservation(messageId, reservationId)
        val messageMap = emptyMap<Long, Message>() // 메시지가 없음

        // when
        val results = dispatcher.dispatch(listOf(reservation), messageMap)

        // then
        assertThat(results).isEmpty()
        verifyNoInteractions(notificationClient) // client 호출 안 했는지도 검증
    }

}