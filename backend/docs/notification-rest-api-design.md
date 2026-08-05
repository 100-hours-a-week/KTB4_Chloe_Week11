# FitMate 알림 기능 REST API 설계 문서

상태: 설계 완료 (코드 미작성)

참고 문서
1. `FitMate_알림기능_설계문서.md` — 알림 도메인 규칙, 데이터 모델(11개 필드), 좋아요 그룹핑 정책, 모달 표시 규칙의 근거
2. `FitMate_알림_SSE_API_명세.md` (완료) — SSE 자체는 재설계하지 않고, 알림 종류 값(`like`/`comment`/`blind`), `unreadCount` 필드명, 그리고 "알림 생성 일시를 SSE id·커서 양쪽에 쓴다"는 결정만 그대로 가져와 REST 쪽과 맞춘다.

작성 대상은 아래 네 개 API다.
1. 안 읽은 알림 개수 조회 (신규 엔드포인트 없음 — 로그인 API 응답 확장)
2. 알림 목록 조회
3. 알림 읽음 처리
4. 알림 삭제

---

## 0. 실제 코드 확인 결과


| 상황 | 실제 응답 바디 | 비고 |
|---|---|---|
| 성공 | `{ "message": string, "data": T }` | `ApiResponse.of(message, data)` |
| 비즈니스 예외 (400/401/403/404/409 ...) | `{ "message": string, "field": string \| null }` | `ErrorResponse`. `field`는 `DuplicateResourceException`만 값이 있고, 나머지는 `null`이 그대로 직렬화된다 (Jackson null 제외 설정 없음). |
| `@Valid` 검증 실패 (400) | `{ "message": string[], "data": null }` | `ValidErrorResponse` |
| 예기치 못한 서버 오류 (500) | `{ "message": string, "field": null }` | **비즈니스 예외와 동일한 `ErrorResponse` 형태다.** 500만 다른 필드 구성(`ok/status/message/data`)을 쓰지 않는다 — `GlobalExceptionHandler`의 `Exception.class` 핸들러도 `ErrorResponse.of(exception.getMessage())`를 그대로 쓴다. |

→ HTTP status는 응답 바디가 아니라 `ResponseEntity.status(...)`로만 표현된다. 이 문서의 "Response status code" 컬럼은 그 HTTP 상태 코드를 뜻한다.

새로 필요한 예외 하나: query parameter 검증 실패(잘못된 커서, `limit` 값 등)에 쓸 400 전용 예외가 현재 없다 (`BusinessException`을 상속한 게 `NotFoundException`/`UnauthorizedException`/`ForbiddenException`/`DuplicateResourceException`/`TooManyRequestsException`뿐). `InvalidRequestException(message) → 400`을 하나 추가해야 하며, 응답 바디 형태는 기존 `ErrorResponse`를 그대로 재사용한다(새 포맷 아님).

---

## 1. 알림 데이터 모델 (신규 `Notification` 엔티티, 설계안)

설계 문서 3장의 11개 필드를 그대로 반영한다.

| # | 필드 (엔티티) | 컬럼 | 타입 | 값이 채워지는 대상 | 용도 |
|---|---|---|---|---|---|
| 1 | `notificationId` | `notification_id` | `Long` (PK, IDENTITY) | 전체 | REST 리소스 식별자 (`/notifications/{notification_id}`) |
| 2 | `receiver` | `user_id` | `User` (ManyToOne) | 전체 | 알림 수신자 |
| 3 | `type` | `notification_type` | Enum(`LIKE`/`COMMENT`/`BLIND`) | 전체 | API에는 소문자(`like`/`comment`/`blind`)로 노출 — SSE와 동일 |
| 4 | `post` | `post_id` | `Post` (ManyToOne) | 전체 | 관련 게시글. 좋아요·댓글은 응답에 `postId` 노출, 블라인드는 이동하지 않으므로 미노출 |
| 5 | `actor` | `actor_user_id` | `User` (ManyToOne, nullable) | 좋아요(그룹 내 최근 행위자)·댓글 | 블라인드는 `null` |
| 6 | `comment` | `comment_id` | `Comment` (ManyToOne, nullable) | 댓글만 | 응답에는 노출 안 함 (모달이 댓글 내용을 안 보여줌) |
| 7 | `likeGroupCount` | `like_group_count` | `Integer` | 좋아요만 갱신, 댓글·블라인드는 1 고정 | 좋아요 모달의 "그 사용자 외 N명" 계산용 |
| 8 | `isRead` | `is_read` | `Boolean` | 전체 | 읽음 처리 API의 대상 |
| 9 | `groupCreatedAt` | `group_created_at` | `LocalDateTime` (nullable) | **좋아요에만** 값이 채워짐 | 4시간 안전장치 판정 전용, 정렬/커서에는 절대 쓰지 않음 |
| 10 | `createdAt` | `created_at` | `LocalDateTime` | 전체 | **알림 생성 일시.** 좋아요 그룹에 새 좋아요가 들어올 때마다 이 값이 갱신됨. 목록 정렬 기준 + SSE 이벤트 `id` + REST 커서 값의 원본이 전부 이 컬럼 하나다 |
| 11 | `deletedAt` | `deleted_at` | `LocalDateTime` (nullable) | 전체 | 소프트 삭제, 목록 조회 시 항상 제외 |

행위자·게시글 정보는 설계 문서 지침대로 페치 조인(`JOIN FETCH`)으로 함께 가져와 N+1을 피한다(`Post/repository/PostRepository.findLatestPosts` 패턴과 동일).

### 안 읽은 개수 계산 — 공유 서비스 메서드
```
NotificationService.getUnreadCount(Long userId)
  = notificationRepository.countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull(userId)
```
이 메서드 하나를 아래 세 곳에서 재사용한다(로직 중복 구현 없음).
- **로그인 응답** (API 1) — `AuthService.LoginUser`에서 호출
- **SSE push** — `like`/`comment`/`blind` 이벤트를 보내는 `@TransactionalEventListener`가 `data.unreadCount` 계산 시 호출 (SSE 명세 3-5)
- **읽음 처리 응답** (API 3) — 아래 2-3 참고

---

## 2. API 명세

### 2-1. 안 읽은 알림 개수 조회 → 로그인 API 확장 (신규 엔드포인트 없음)

기존 `AuthController.LoginUser` / `LoginResponseDto`를 그대로 쓰고 `unreadCount` 필드만 추가한다. 요청/에러 로직은 손대지 않는다.

| Request method | url | body | 설명 |
|---|---|---|---|
| POST | `/auth/login` | `{ "email": string, "password": string }` (기존과 동일, 변경 없음) | 로그인. 응답에 `unreadCount` 추가 |

| Response status code | message |
|---|---|
| 200 | `{ "message": "로그인 성공", "data": { "jwtToken": {...}, "profileImage": "https://.../profile.png", "unreadCount": 5 } }` |
| 401 | `{ "message": "이메일 또는 비밀번호가 일치하지 않습니다.", "field": null }` (기존 `UnauthorizedException`, 변경 없음) |
| 404 | `{ "message": "존재하지 않는 사용자입니다.", "field": null }` (기존 `NotFoundException`, 변경 없음) |
| 400 | `{ "message": ["이메일 형식이 맞지 않습니다.", "비밀번호는 필수값입니다."], "data": null }` (기존 `@Valid` 검증, 변경 없음) |

`LoginResponseDto`에 `unreadCount` 필드를 추가하고, `AuthService.LoginUser`가 `user` 조회 직후 `notificationService.getUnreadCount(user.getUserId())`를 호출해 채워 넣는다. `AuthService`가 `NotificationService`를 새로 의존하게 된다.

---

### 2-2. 알림 목록 조회

| Request method | url | query parameter | 설명 |
|---|---|---|---|
| GET | `/notifications` | `cursor` (string, optional), `limit` (int, default 10), `unreadOnly` (boolean, default false) | 커서 기반 무한 스크롤. 정렬은 `createdAt` DESC |

**`cursor` 설계 — base64 opaque 문자열**
- 서버가 응답에 실어 보낸 `nextCursor` 값을 클라이언트는 그대로 다음 요청의 `cursor`로 넘기기만 한다. 내부 값을 파싱하거나 직접 만들 수 없다.
- 인코딩: 마지막 항목의 `createdAt`(ISO-8601, 예: `2026-08-04T13:20:00.123456`)을 UTF-8 바이트로 만들고 `Base64` 인코딩한다. 서명(HMAC)은 지금 단계에서는 넣지 않는다 — 5장 "추후 과제" 참고.
- 서버는 `cursor`를 디코딩해서 `LocalDateTime`으로 파싱한 뒤 `WHERE created_at < :decoded` 조건에만 쓴다. 설계 문서 3장 지침대로 보조 tie-break 컬럼(id 등)은 두지 않는다 — SSE의 Last-Event-ID 비교도 동일하게 `created_at` 단독 비교라 REST 쪽만 별도 규칙을 만들 이유가 없다.
- `cursor`가 base64로 디코딩되지 않거나 디코딩 결과가 유효한 날짜 형식이 아니면 400.

**정렬/조회 쿼리 방향**
```
WHERE receiver_id = :userId
  AND deleted_at IS NULL
  AND (:unreadOnly = false OR is_read = false)
  AND (:decodedCursor IS NULL OR created_at < :decodedCursor)
ORDER BY created_at DESC
LIMIT :limit
```

| Response status code | message |
|---|---|
| 200 | 아래 예시 참고 |
| 400 | `{ "message": "cursor 형식이 올바르지 않습니다.", "field": null }` (신규 `InvalidRequestException`) |
| 400 | `{ "message": "limit 값이 올바르지 않습니다.", "field": null }` (신규 `InvalidRequestException`, `limit <= 0`) |
| 401 | `{ "message": "인증이 필요합니다.", "field": null }` |

**200 응답 예시** (Post 목록 API와 달리, opaque 커서를 클라이언트가 직접 만들 수 없으므로 `nextCursor`를 응답에 함께 내려줘야 한다 — 그래서 리스트만 반환하던 기존 컨벤션과 다르게 `notifications` + `nextCursor`로 한 단계 감쌌다):
```json
{
  "message": "알림 목록 조회 성공",
  "data": {
    "notifications": [
      {
        "notificationId": 42,
        "type": "like",
        "isRead": false,
        "createdAt": "2026-08-04T13:20:00.123456",
        "postId": 17,
        "postTitle": "오늘 3대 운동 인증합니다",
        "actorNickname": "예원",
        "actorProfileImage": "https://.../profile.png",
        "likeGroupCount": 3
      },
      {
        "notificationId": 41,
        "type": "comment",
        "isRead": true,
        "createdAt": "2026-08-04T12:05:00.000000",
        "postId": 12,
        "postTitle": "헬스장 PT 후기 남깁니다",
        "actorNickname": "재현",
        "actorProfileImage": "https://.../profile2.png",
        "likeGroupCount": null
      },
      {
        "notificationId": 40,
        "type": "blind",
        "isRead": false,
        "createdAt": "2026-08-04T10:00:00.000000",
        "postId": null,
        "postTitle": "운동 안 하고 놀러만 다닙니다",
        "actorNickname": null,
        "actorProfileImage": null,
        "likeGroupCount": null
      }
    ],
    "nextCursor": "MjAyNi0wOC0wNFQxMDowMDowMC4wMDAwMDA="
  }
}
```
다음 페이지가 없으면 `nextCursor: null` — 별도의 `hasNext` 불리언은 두지 않는다.

**필드 근거** (설계 문서 9장, 목록 카드와 모달이 같은 응답을 공유 — 별도 상세 조회 API 없음)
- `postTitle`: 9-8 "모든 모달 내용에는 게시글 제목이 필수로 들어가야 한다" → **세 종류 전부** 포함
- `postId`: 9-6 "댓글과 좋아요 모달은 클릭 시에 관련된 게시글로 이동한다" → 좋아요·댓글만 포함, 블라인드는 9-7("클릭해도 아무 반응 없음")에 따라 `null`
- `actorNickname` / `actorProfileImage`: 좋아요는 그룹 내 최근 행위자(9-1), 댓글은 작성자(9-2). 블라인드는 행위자 개념이 없어 `null`
- `likeGroupCount`: 좋아요 모달의 "카운트 1이면 그 사용자만, 2 이상이면 외 N명"(9-1) 계산용. FE가 `likeGroupCount - 1`로 나머지 인원수 계산
- `commentId`는 응답에 넣지 않는다 — 댓글 모달이 댓글 내용을 보여주지 않으므로(9-2) 필요 없음

---

### 2-3. 알림 읽음 처리

| Request method | url | body | 설명 |
|---|---|---|---|
| PATCH | `/notifications/{notification_id}/read` | 없음 | 읽음 처리. 응답에 갱신된 `unreadCount` 포함 |

신규 엔드포인트를 만들지 않기로 했기 때문에(요구사항 1), 설계 문서 9-5 "읽음 처리 후 사이드바 개수는 서버에 다시 요청해서 갱신"을 만족하려면 이 API 응답이 그 값을 실어 보내는 방법뿐이다. 그래서 `getUnreadCount`를 여기서도 재사용한다.

| Response status code | message |
|---|---|
| 200 | `{ "message": "알림 읽음 처리 완료", "data": { "unreadCount": 4 } }` |
| 404 | `{ "message": "알림을 찾을 수 없습니다.", "field": null }` (존재하지 않음 / 본인 소유 아님 / 이미 소프트 삭제됨 — 모두 동일 메시지) |
| 401 | `{ "message": "인증이 필요합니다.", "field": null }` |

멱등성: 이미 읽음 처리된 알림에 다시 호출해도 에러 없이 200과 현재 `unreadCount`를 반환한다. 좋아요 그룹의 "읽음 처리 시 그룹 종료" 규칙은 이 API가 아니라 *다음 좋아요가 들어오는 시점*에 "기존 그룹이 이미 읽음 상태인가"를 확인해서 처리하는 로직이므로, 이 API는 `is_read = true` 갱신 외에 별도 처리가 필요 없다.

---

### 2-4. 알림 삭제

| Request method | url | body | 설명 |
|---|---|---|---|
| DELETE | `/notifications/{notification_id}` | 없음 | 소프트 삭제 (`deleted_at`만 기록) |

| Response status code | message |
|---|---|
| 200 | `{ "message": "알림 삭제 완료", "data": null }` |
| 404 | `{ "message": "존재하지 않거나 아직 읽지 않은 알림입니다.", "field": null }` |
| 401 | `{ "message": "인증이 필요합니다.", "field": null }` |

**요구사항 반영**: "알림이 없음"과 "있지만 읽음 상태가 아님"을 서로 다른 메시지로 구분하지 않는다. 조회 쿼리를 `receiver_id`, `deleted_at IS NULL`, `is_read = true`, `notification_id` 조건으로 한 번에 걸고, 결과가 없으면 무조건 위 단일 메시지로 `NotFoundException`을 던지면 자연스럽게 두 상황이 같은 응답으로 합쳐진다.

---

## 3. 이번 설계에서 다루지 않은 것 / 추후 과제

1. **커서 서명(HMAC)** — 지금은 base64 인코딩만 하고 서명은 붙이지 않는다. 클라이언트가 디코딩해서 임의 날짜로 조작한 `cursor`를 보내도 현재 구조에서는 그대로 조회가 수행된다(본인 알림만 필터링되므로 다른 사용자 데이터가 노출되지는 않지만, 임의 시점부터 조회는 가능하다). 필요해지면 서버 비밀키로 HMAC 서명을 추가해 위변조를 막는다.
2. 소프트 삭제된 알림의 하드 삭제 배치 및 보존 기간.
3. 알림 목록/개수 조회에 대한 캐싱 여부 및 전략.
4. 여러 탭·기기 동시 접속, 서버 다중화 시 확장성 문제 (설계 문서 11장과 동일하게 범위 밖).

---

## 부록 — 참고한 기존 코드

| 항목 | 참고 위치 |
|---|---|
| 공통 응답 wrapper | `response/ApiResponse.java`, `response/ErrorResponse.java`, `response/ValidErrorResponse.java` |
| 예외 → HTTP 상태 매핑 | `handler/GlobalExceptionHandler.java`, `exception/BusinessException.java`, `exception/NotFoundException.java` |
| 로그인 API (확장 대상) | `Auth/controller/AuthController.java`, `Auth/service/AuthService.java`, `Auth/dto/LoginResponseDto.java` |
| 커서 페이지네이션 원형 | `Post/controller/PostController.java`의 `listPost`, `Post/repository/PostRepository.java`의 `findLatestPosts` (단, 여기 커서는 opaque가 아닌 평문 Long이라 이번 설계와 인코딩 방식은 다름) |
| 소프트 삭제 컨벤션 | `Post/service/PostService.java`의 `deletePost` |
| "존재하지 않는 것처럼" 404 처리 | `Post/service/PostService.java`의 `cancelLike` |
| 엔티티 컬럼 네이밍(`created_at`/`deleted_at` 등) | `Post/entity/Post.java`, `Comment/entity/Comment.java` |
