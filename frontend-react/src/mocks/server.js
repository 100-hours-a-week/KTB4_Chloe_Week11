import { setupServer } from 'msw/node';

// setupServer(): MSW가 Node(jsdom) 환경에서 fetch 요청을 가로챌 수 있도록
// "가짜 서버"를 만드는 함수. 인자로 넘긴 handler들이 기본(default) 핸들러가 된다.
// 지금은 프로젝트 전역에서 공통으로 쓸 기본 핸들러가 없어서 빈 배열로 시작하고,
// 각 테스트 파일에서 필요할 때마다 server.use()로 그때그때 핸들러를 추가한다.
export const server = setupServer();
