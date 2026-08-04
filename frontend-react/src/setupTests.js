import '@testing-library/jest-dom';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './mocks/server.js';

// beforeAll: 이 파일이 로드된 후, 전체 테스트가 시작되기 "한 번" 실행됨.
// 여기서 가짜 서버(MSW)를 켠다. 이후 테스트 코드에서 fetch를 호출하면
// 실제 네트워크로 나가지 않고 이 가짜 서버가 요청을 가로챈다.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// afterEach: 테스트 "하나가 끝날 때마다" 실행됨.
// 어떤 테스트에서 server.use()로 추가한 임시 핸들러가 다음 테스트에
// 영향을 주지 않도록 원래 상태(빈 서버)로 초기화한다.
afterEach(() => server.resetHandlers());

// afterAll: 전체 테스트가 다 끝난 후 "한 번" 실행됨. 가짜 서버를 끈다.
afterAll(() => server.close());