package com.mfort.momsitter.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.mfort.momsitter.api.dto.CreateReservationRequest
import com.mfort.momsitter.api.dto.CursorPageResponse
import com.mfort.momsitter.api.dto.ScheduledMessageResponse
import com.mfort.momsitter.application.ReservationService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(ReservationController::class)
class ReservationControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockitoBean
    private lateinit var reservationService: ReservationService

    @Test
    fun `예약 생성 성공 - 200 OK`() {
        // given
        val request = CreateReservationRequest(
            phoneNumber = "01012345678",
            title = "예약 알림",
            contents = "오늘 5시에 픽업 예약이 있습니다.",
            dueAt = Instant.now().plusSeconds(600).toString()
        )
        val reservationId = 10L


        `when`(reservationService.createReservation(request)).thenReturn(reservationId)

        // when & then
        mockMvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(reservationId.toString()))
    }

    @Test
    fun `유효성 검증 실패 - 잘못된 전화번호`() {
        val request = CreateReservationRequest(
            phoneNumber = "1234", // 잘못된 패턴
            title = "예약 알림",
            contents = "내용",
            dueAt = Instant.now().plusSeconds(600).toString()
        )

        mockMvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `예약 목록 조회 - 데이터가 있을 때`() {
        // given
        val expectedItem = ScheduledMessageResponse(
            reservationId = 10L,
            messageId = 1001L,
            phoneNumber = "01012345678",
            title = "예약 알림",
            contents = "테스트 예약",
            dueAtKst = "2025-10-02T10:30:00",
            status = "READY",
            updatedAtKst = "2025-10-02T09:00:00"
        )
        val response = CursorPageResponse(
            items = listOf(expectedItem),
            nextCursor = "20"
        )

        `when`(reservationService.listByCursor(any(), anyOrNull(), any()))
            .thenReturn(response)

        // when & then
        mockMvc.perform(
            get("/api/v1/reservations")
                .param("status", "READY")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].reservationId").value(expectedItem.reservationId))
            .andExpect(jsonPath("$.items[0].messageId").value(expectedItem.messageId))
            .andExpect(jsonPath("$.items[0].phoneNumber").value(expectedItem.phoneNumber))
            .andExpect(jsonPath("$.items[0].title").value(expectedItem.title))
            .andExpect(jsonPath("$.items[0].contents").value(expectedItem.contents))
            .andExpect(jsonPath("$.items[0].dueAtKst").value(expectedItem.dueAtKst))
            .andExpect(jsonPath("$.items[0].status").value(expectedItem.status))
            .andExpect(jsonPath("$.items[0].updatedAtKst").value(expectedItem.updatedAtKst))
            .andExpect(jsonPath("$.nextCursor").value(response.nextCursor))
    }

    @Test
    fun `예약 목록 조회 - 데이터가 없을 때`() {
        // given
        val response = CursorPageResponse(
            items = emptyList<ScheduledMessageResponse>(),
            nextCursor = null
        )

        `when`(reservationService.listByCursor(any(), anyOrNull(), any()))
            .thenReturn(response)

        // when & then
        mockMvc.perform(
            get("/api/v1/reservations")
                .param("status", "READY")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.nextCursor").doesNotExist())
    }
}