# FitMate 알림 기능 REST API 설계 문서

상태: 설계 완료 (코드 미작성)
참고 문서
1. FitMate 알림 기능 설계 문서 — 알림 도메인 규칙, 데이터 모델, 좋아요 그룹핑 정책의 근거
2. FitMate 알림 기능 SSE API 명세 (완료) — 알림 종류 값(`like`/`comment`/`blind`), `unreadCount` 등 필드 네이밍만 참조. SSE 자체는 재설계하지 않음.

이 문서는 아래 네 개 API만 다룬다.
1. 안 읽은 알림 개수 조회
2. 알림 목록 조회
3. 알림 읽음 처리
4. 알림 삭제

---

## 0. 공통 사항

- Base path: `/notifications` (SSE 구독 엔드포인트 `GET /notifications/subscribe`와 리소스를 공유)
- 인증: 기존 REST API와 동일하게 `Authorization: Bearer <accessToken>` (SSE 전용 httpOnly 쿠키와는 별개). 인증 실패는 `JWTFilter` → 전역 401 처리로 기존 컨벤션을 그대로 따른다.
- 성공 응답: `ApiResponse<T>` (`{ message, data }`) — `backend/.../response/ApiResponse.java` 그대로 사용.
- 에러 응답: `ErrorResponse` (`{ message, field? }`, 도메인 예외) / `ValidErrorResponse` (`{ message: string[] }`, `@Valid` 검증 실패). 새 예외는 만들지 않고 기존 `NotFoundException`(404), `UnauthorizedException`(401)만 재사용한다.
- 알림 종류 값은 SSE 명세와 동일하게 `like` / `comment` / `blind` 문자열을 그대로 쓴다. 엔티티에서는 Java enum(`NotificationType.LIKE/COMMENT/BLIND`)으로 관리하고, JSON 직렬화 시 소문자로 노출한다.
- 안 읽은 개수 필드명은 SSE의 `unreadCount`와 동일하게 `unreadCount`로 통일한다.

---

## 1. 안 읽은 알림 개수 조회

**`GET /notifications/unread-count`**

### 요청
파라미터 없음. 인증된 사용자(`@AuthenticationPrincipal`)의 `userId` 기준으로 본인 데이터만 조회.

### 응답 — `200 OK`
```json
{
  "message": "안 읽은 알림 개수 조회 성공",
  "data": {
    "unreadCount": 5
  }
}
```

### 서비스 설계 (요구사항 6)
`NotificationService.getUnreadCount(Long userId)` 단일 메서드로 카운트 쿼리(`countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull`)를 수행하고, 이 메서드를 두 곳에서 그대로 재사용한다.
- 이 컨트롤러
- SSE 쪽 알림 생성 리스너(`@TransactionalEventListener`)가 `like`/`comment`/`blind` 이벤트를 push하기 직전 `data.unreadCount`를 계산할 때

즉 REST 응답값과 SSE로 push되는 `unreadCount`는 항상 같은 소스에서 계산되며, 카운트 로직이 두 곳에 중복 구현되지 않는다.

### 검증 및 에러 케이스
| 상황 | 응답 |
|---|---|
| 인증 토큰 없음/만료/위조 | 401 (JWTFilter 공통 처리) |
| 그 외 | 별도 검증 없음 — 파라미터가 없고 본인 데이터만 조회하므로 |

---

## 2. 알림 목록 조회

**`GET /notifications`**

### 요청 파라미터 (query string)
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `cursorCreatedAt` | ISO-8601 datetime string | X | 없음(최초 조회) | 마지막으로 받은 알림의 `createdAt` |
| `cursorId` | Long | `cursorCreatedAt`이 있으면 필수 | 없음 | 동시각 tie-break용, 마지막으로 받은 알림의 `notificationId` |
| `limit` | int | X | `10` | Post 목록 API(`PostController.listPost`)와 동일한 기본값 |
| `unreadOnly` | boolean | X | `false` | 안 읽은 알림만 필터링 |

**커서를 두 값(`cursorCreatedAt` + `cursorId`)으로 나눈 이유**: Post 목록 API는 정렬 기준과 PK(`postId`)가 같은 방향으로 단조 증가하기 때문에 `cursor` 하나(Long)로 충분하다. 하지만 알림 목록의 정렬 기준인 `created_at`(필드 10)은 좋아요 그룹에 새 좋아요가 들어올 때마다 갱신되는 값이라, PK(`notificationId`)와 증가 방향이 다를 수 있다 — 오래전에 생성된 낮은 id의 좋아요 그룹이 새 좋아요를 받으면 `created_at`만 최신화되고 id는 그대로다. 그래서 `notificationId` 단독 커서로는 정렬이 깨질 수 있어, `(created_at, notificationId)` 복합 커서로 tie-break한다.

### 정렬/조회 쿼리 방향 (Post의 `findLatestPosts` 패턴 응용)
```
WHERE receiver_id = :userId
  AND deleted_at IS NULL
  AND (:unreadOnly = false OR is_read = false)
  AND (
    :cursorCreatedAt IS NULL
    OR created_at < :cursorCreatedAt
    OR (created_at = :cursorCreatedAt AND notification_id < :cursorId)
  )
ORDER BY created_at DESC, notification_id DESC
LIMIT :limit
```
`group_created_at`(4시간 안전장치 전용, 좋아요만 값 있음)은 이 쿼리에 전혀 등장하지 않는다. 행위자 정보는 설계 문서 3장 지침대로 페치 조인으로 함께 가져와 N+1을 피한다.

### 응답 — `200 OK`
```json
{
  "message": "알림 목록 조회 성공",
  "data": [
    {
      "notificationId": 42,
      "type": "like",
      "isRead": false,
      "createdAt": "2026-08-04T13:20:00",
      "actorNickname": "예원",
      "actorProfileImage": "https://.../profile.png",
      "likeGroupCount": 3
    },
    {
      "notificationId": 41,
      "type": "comment",
      "isRead": true,
      "createdAt": "2026-08-04T12:05:00",
      "actorNickname": "재현",
      "actorProfileImage": "https://.../profile2.png",
      "likeGroupCount": null
    },
    {
      "notificationId": 40,
      "type": "blind",
      "isRead": false,
      "createdAt": "2026-08-04T10:00:00",
      "actorNickname": null,
      "actorProfileImage": null,
      "likeGroupCount": null
    }
  ]
}
```

**필드가 이것만 있는 이유 (요구사항 2)**: 설계 문서 9장의 모달 내용 요건을 그대로 매핑한 것이 전부다.
- 좋아요 모달: "최근 행위자 + 그룹 카운트" → `actorNickname`, `actorProfileImage`, `likeGroupCount` (카운트 1이면 그 사용자만, 2 이상이면 FE가 `likeGroupCount - 1`로 "외 N명" 계산)
- 댓글 모달: "어떤 사용자가 댓글을 남겼다는 사실만" → `actorNickname`, `actorProfileImage`만 필요, 댓글 내용은 응답에 넣지 않는다
- 블라인드 모달: "신고 누적으로 블라인드 처리되었다"는 문구는 매번 동일한 고정 문구라 서버가 값을 보낼 필요가 없다 — `type: "blind"`만으로 FE가 정적 문구를 렌더링

`postId`, `commentId`는 모달이 게시글로 이동하지 않으므로(요구사항 2, 상세 조회 API 미생성) 응답에 포함하지 않는다. 서버 내부적으로는 엔티티에 계속 저장해 둔다(연관 게시글 추적용).

### 다음 페이지 존재 여부
별도의 `hasNext`/`nextCursor` 메타 필드를 두지 않는다 — Post 목록 API와 동일한 컨벤션. 응답 리스트 길이가 `limit`과 같으면 다음 페이지가 있을 수 있다고 간주하고, 마지막 항목의 `createdAt`/`notificationId`를 다음 요청의 `cursorCreatedAt`/`cursorId`로 넘긴다.

### 검증 및 에러 케이스
| 상황 | 응답 |
|---|---|
| `cursorCreatedAt` 형식이 ISO-8601이 아님(파싱 실패) | 400 |
| `cursorCreatedAt`만 있고 `cursorId`가 없음 (또는 반대) | 400 |
| `limit <= 0` | 400 |
| 인증 없음 | 401 |

> `limit` 검증은 현재 `PostController.listPost`에는 없는 방어 로직이다(0 이하 값이 오면 `PageRequest.of`에서 미확인 예외로 500이 난다). 이번 API는 처음부터 검증을 넣어 방지하고, 필요하면 Post 쪽도 같은 방식으로 보강할지 별도로 논의할 수 있다.

---

## 3. 알림 읽음 처리

**`PATCH /notifications/{notification_id}/read`**

기존 `PATCH` 사용 사례(`UserController.changePassword`)와 동일하게, 값을 반환할 필요 없는 순수 상태 변경에 `PATCH` + `204`를 쓴다.

### 요청
경로 파라미터 `notification_id`(Long). 바디 없음.

### 응답 — `204 No Content`
바디 없음.

**`unreadCount`를 반환하지 않는 이유**: 설계 문서 9장 5번 "읽음 처리가 끝나면 사이드바의 안 읽은 개수는 프론트에서 임의로 계산하지 않고, 서버에 다시 요청해서 받은 값으로 갱신한다"를 그대로 따른 것이다. FE는 이 API 호출 후 별도로 `GET /notifications/unread-count`를 재호출해서 최신값을 받는다.

**멱등성**: 이미 읽음 처리된 알림에 다시 호출해도 에러 없이 `204`를 반환한다(부작용 없음, 이미 읽음 상태 유지). 좋아요 그룹의 "읽음 처리 시 그룹 종료" 규칙(설계 문서 4장 3번)은 이 API가 아니라 *새 좋아요가 들어오는 시점*에 "기존 알림이 이미 읽음 상태인가"를 확인해서 판단하는 로직이므로, 이 API는 `is_read = true`로 갱신하는 것 외에 별도 처리가 필요 없다.

### 검증 및 에러 케이스
| 상황 | 응답 |
|---|---|
| `notification_id`에 해당하는 알림이 없음 | 404 |
| 알림은 존재하지만 본인(`receiver`) 소유가 아님 | 404 (403 대신 — 존재 여부를 노출하지 않기 위해 `Like` 취소 API 등 기존 코드와 동일하게 "없는 것처럼" 처리) |
| 이미 소프트 삭제된 알림 | 404 |
| 인증 없음 | 401 |

---

## 4. 알림 삭제

**`DELETE /notifications/{notification_id}`**

`PostController.deletePost`와 동일한 컨벤션: 소프트 삭제 + `204 No Content`.

### 요청
경로 파라미터 `notification_id`(Long). 바디 없음.

### 응답 — `204 No Content`
`deleted_at`에 현재 시각만 기록하고 실제 행은 지우지 않는다(설계 문서 10장 3번과 동일).

### 검증 및 에러 케이스 (요구사항 3)
아래 네 가지 상황을 **모두 동일하게 404**로 응답한다. 조회 쿼리를 `receiver_id`, `deleted_at IS NULL`, `is_read = true` 조건으로 한 번에 걸고, 결과가 없으면 무조건 `NotFoundException`을 던지는 방식으로 구현하면 자연스럽게 이렇게 된다.

| 상황 | 응답 |
|---|---|
| `notification_id`에 해당하는 알림이 없음 | 404 |
| 본인 소유가 아님 | 404 |
| 이미 소프트 삭제됨 | 404 |
| 아직 읽음 처리되지 않음(`is_read = false`) | 404 — 지침("읽음 상태가 아니면 404")에 따른 것 |
| 인증 없음 | 401 |

---

## 5. 이번 설계에서 다루지 않은 것 (추후 논의)

지시사항대로 아래 항목은 이번 문서 범위에서 제외했다. 필요하면 이어서 설계할 수 있다.
1. 소프트 삭제된 알림의 하드 삭제 배치 및 보존 기간
2. 알림 목록/개수 조회에 대한 캐싱 여부 및 전략

---

## 부록 — 참고한 기존 코드 컨벤션

| 항목 | 참고 위치 |
|---|---|
| 공통 응답 wrapper | `response/ApiResponse.java`, `response/ErrorResponse.java`, `response/ValidErrorResponse.java` |
| 커서 기반 페이지네이션 | `Post/controller/PostController.java`의 `listPost`, `Post/repository/PostRepository.java`의 `findLatestPosts` |
| 소프트 삭제 + 204 삭제 컨벤션 | `Post/service/PostService.java`의 `deletePost` |
| PATCH + 204(상태 변경 전용) 컨벤션 | `User/controller/UserController.java`의 `changePassword` |
| "존재하지 않는 것처럼" 404 처리 | `Post/service/PostService.java`의 `cancelLike` (`NotFoundException`) |
| 예외 → HTTP 상태 매핑 | `handler/GlobalExceptionHandler.java`, `exception/NotFoundException.java` |
