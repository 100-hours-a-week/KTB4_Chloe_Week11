const BASE_URL = import.meta.env.VITE_API_URL;

const NO_AUTH_PATHS = ['/auth/login', '/users/signup'];

function needsAuth(path) {
  return !NO_AUTH_PATHS.some((noAuthPath) => path.startsWith(noAuthPath));
}

async function request(path, method, data = null) {
  const isFormData = data instanceof FormData;

  const headers = {
    ...(needsAuth(path) && {
      Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
    }),
    ...(!isFormData && data !== null && { 'Content-Type': 'application/json' }),
  };

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    ...(data !== null && { body: isFormData ? data : JSON.stringify(data) }),
  });

  return handleResponse(response,path);
}

async function handleResponse(response,path) {
  const { status } = response;

  if (status === 200 || status === 201) {
    return response.json();
  }
  if (status === 204) {
    return null;
  }

  const errorBody = await response.json().catch(() => ({}));
  const { message, field } = errorBody;

  switch (status) {
    case 401:
      if (!needsAuth(path)) {
        console.error('로그인&회원가입 실패', message)
      } else {
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
      break;
    case 403:
      console.error('인가 실패:', message);
      break;
    case 404:
      console.error('리소스를 찾을 수 없습니다:', message);
      break;
    case 409:
      console.error(`중복된 값 (${field}):`, message);
      break;
    case 400:
      console.error('잘못된 요청:', message);
      break;
    case 500:
      console.error('서버 에러:', message);
      break;
    default:
      console.error('알 수 없는 에러:', message);
  }

  const error = new Error(message ?? '요청 처리 중 오류가 발생했습니다.');
  error.status = status;
  error.field = field ?? null;
  throw error;
}

export default request;
