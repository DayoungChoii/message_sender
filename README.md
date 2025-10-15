# Momsitter Message Reservation System

메시지 예약 발송 시스템 과제 구현물입니다.  
Java 21, Kotlin, Spring Boot 3, H2 Database 기반으로 개발되었습니다.

---

## 실행 방법

```bash
./gradlew bootRun
```

- 기본 포트: **8080**
- H2 콘솔: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

---

## 데이터 모델

- **Message ↔ Reservation: 1:1 관계**

### message
```sql
CREATE TABLE message (
                        id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                        phone_number        VARCHAR(20)  NOT NULL,
                        title               VARCHAR(100) NOT NULL,
                        contents            VARCHAR(500) NOT NULL,
                        client_id           VARCHAR(64),
                        external_request_id VARCHAR(128),
                        created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### reservation
```sql
CREATE TABLE reservation (
                            id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                            message_id      BIGINT NOT NULL,
                            due_at          TIMESTAMP NOT NULL,
                            next_attempt_at TIMESTAMP NOT NULL,
                            status          VARCHAR(20) NOT NULL,  -- READY|RETRY_READY|SENDING|DONE|FAILED|CANCELED
                            retry_count     INT NOT NULL DEFAULT 0,
                            canceled_at     TIMESTAMP,
                            done_at         TIMESTAMP,
                            failed_at       TIMESTAMP,
                            created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_res_pick  ON reservation (status, next_attempt_at, id);
CREATE INDEX idx_res_msgid ON reservation (message_id);
```

인덱스:
- `(status, next_attempt_at, id)` → 스케줄러 픽업 최적화
- `(message_id)` → 조인 최적화

---

## 시스템 흐름

1. **예약 생성 (API 호출)**
    - 클라이언트가 `POST /api/v1/reservations` 요청으로 메시지와 예약 시간을 등록합니다.
    - 예약은 최대 +2시간까지만 가능합니다.

2. **스케줄러 실행 (`@Scheduled`)**
    - 30초 주기로 동작.
    - 예약 테이블에서 `READY` 또는 `RETRY_READY` 상태 중 실행 시점이 지난 건을 가져옵니다.
    - 비관적 락(`FOR UPDATE SKIP LOCKED`)으로 다중 서버 환경에서도 중복 실행을 방지합니다.

3. **메시지 발송 (비동기)**
    - `WebClient`를 통해 Mock 메시지 서버로 병렬 전송합니다.
    - `CompletableFuture` 기반 비동기 처리로 레이턴시를 줄였습니다.

4. **결과 반영**
    - 응답 코드에 따라 상태를 업데이트합니다.
        - 2xx → `DONE`
        - 5xx → `RETRY_READY` (retryCount++, nextAttemptAt +30초)
---

## 동작 확인 로그 예시 
동작을 확인할 수 있게 info 로그를 추가했습니다.
```
[Scheduler] 30초 주기 실행 → 예약 5건 로드
[Dispatcher] Reservation 1 → 전송 시작
[Dispatcher] Reservation 2 → 전송 시작
[ResultApplier] Reservation 1 → DONE
[ResultApplier] Reservation 2 → RETRY_READY (retryCount=1)
```

- **예약 → 발송 → 상태 반영** 전 과정을 로그로 확인할 수 있습니다.
---

## API

### 예약 생성
`POST /api/v1/reservations`

Request Body:
```json
{
  "phoneNumber": "01012345678",
  "title": "예약 알림",
  "contents": "오늘 5시에 픽업 예약이 있습니다.",
  "dueAt": "2025-10-02T10:30:00Z"
}
```

Response Body:
```json
10 //reservationId
```

### 예약 조회
`GET /api/v1/reservations?status=READY&cursor=10&limit=20`

Response Body:
```json
{
  "items": [
    {
      "reservationId": 10,
      "messageId": 1001,
      "phoneNumber": "01012345678",
      "title": "예약 알림",
      "contents": "테스트 예약",
      "dueAtKst": "2025-10-02T10:30:00",
      "status": "READY",
      "updatedAtKst": "2025-10-02T09:00:00"
    }
  ],
  "nextCursor": "20"
}
```

---

## 구현 범위

- [x] 예약 생성/조회 API
- [x] 예약 스케줄링 & 발송
- [x] 비관적 락 기반 동시성 제어
- [x] 발송 실패 시 자동 재시도
- [x] 단위/통합 테스트 작성
- [ ] 예약 취소 API (미구현)
- [ ] 인증/인가 (미구현)
- [ ] 메트릭 수집 (미구현)

---

## 시스템 개요

### 주요 설계 포인트

1. **Message ↔ Reservation: 1:1 관계**
   - 하나의 메시지에 대해 하나의 예약이 존재.

2. **비관적 락 기반 동시성 제어**
   - 여러 서버에서 동시에 스케줄링 시 중복 처리를 방지.

3. **병렬 전송 + 순차 반영**
   - WebClient 호출은 비동기 병렬 실행.
   - 결과는 모아 순차적으로 DB에 반영.

4. **효율적인 조회**
   - Cursor 기반 페이지네이션 도입.

5. **테스트 가능 구조**
   - Loader / Dispatcher / Applier 분리.
     - **ReservationLoader**: 예약 + 메시지 로드 (비관적 락 기반 조회)
     - **SendTaskDispatcher**: 병렬 비동기 전송 (CompletableFuture)
     - **SendResultApplier**: 결과 반영 (상태 업데이트, 재시도 관리)
     - **ReservedMessageScheduler**: 전체 오케스트레이션 (30초 주기 실행)
   - 단위 테스트 및 통합 테스트 작성.

---

## 성능 및 제한사항

- **분당 예약 발송 가능 건수**는 아래 요소들에 의해 결정됩니다.  
  (현재 설정 기준: **1분 = 2회 사이클 × 50건 = 최대 100건 처리 가능**)

  1. **스케줄러 주기** (기본 30초)
     - 더 짧게 설정하면 예약 픽업이 빨라지지만 DB/네트워크 부하 증가.

  2. **동시 처리 스레드 수** (`AsyncConfig`의 ThreadPoolTaskExecutor)
     - 기본: core=10, max=20
     - 트래픽 증가 시 스레드풀 크기 조정 가능.

  3. **픽업 단위 (limit)**
     - 예약 조회 시 한 번에 불러오는 최대 row 개수.
     - 기본값: `limit=50`.

- 따라서 실제 서비스에서는 **분당 발송 처리량 요구사항**에 맞추어
   - 스케줄러 주기,
   - 스레드풀 사이즈,
   - limit 값을 **운영 환경에 맞게 조정**해야 합니다.
---

## 기타사항
- 과제 성격상 **local 환경만 제공**, 운영 환경 분리는 하지 않았습니다.
- H2 DB 사용으로 yml의 secret을 따로 설정하지 않았습니다.
