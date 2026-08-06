import request from './request';

/**
 * 알림 목록 조회
 * @param {object} [params]
 * @param {string} [params.cursor] - 다음 페이지 커서 (없으면 첫 페이지)
 * @param {number} [params.limit=10] - 조회 개수
 * @param {boolean} [params.unreadOnly=false] - 안 읽은 알림만 조회할지 여부
 */
export function getNotifications({ cursor, limit, unreadOnly } = {}) {
  const params = new URLSearchParams();
  if (cursor) params.set('cursor', cursor);
  if (limit !== undefined) params.set('limit', limit);
  if (unreadOnly !== undefined) params.set('unreadOnly', unreadOnly);

  const query = params.toString();
  return request(`/notifications${query ? `?${query}` : ''}`, 'GET');
}

/**
 * 알림 읽음 처리
 * @param {number} notificationId
 */
export function readNotification(notificationId) {
  return request(`/notifications/${notificationId}/read`, 'PATCH');
}

/**
 * 전체 알림 읽음 처리
 */
export function readAllNotifications() {
  return request('/notifications/read-all', 'PATCH');
}

/**
 * 알림 삭제
 * @param {number} notificationId
 */
export function deleteNotification(notificationId) {
  return request(`/notifications/${notificationId}`, 'DELETE');
}
