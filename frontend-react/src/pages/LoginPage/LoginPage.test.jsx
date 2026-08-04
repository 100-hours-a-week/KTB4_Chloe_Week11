import { describe, expect, it, vi,test } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../mocks/server.js';
import { AuthProvider } from '../../context/AuthContext/AuthProvider.jsx';
import LoginPage from './LoginPage';

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const LOGIN_URL = 'http://localhost:8080/auth/login';

describe('LoginPage', () => {
  it('이메일/비밀번호를 입력하고 로그인 API가 200을 응답하면 게시글 목록(/board)으로 이동한다', async () => {
    // given
    // 이메일 & 비밀번호 입력하고, 로그인 API가 성공(200)을 응답하도록 세팅
    server.use(
      http.post(LOGIN_URL, () => {
        return HttpResponse.json(
          {
            message: '로그인 성공',
            data: {
              jwtToken: { accessToken: 'test-access-token' },
              profileImage: null,
            },
          },
          { status: 200 },
        );
      }),
    );

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('이메일'), 'test@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'Test1234**');

    // when
    // 로그인 버튼을 누르면
    const loginButton = screen.getByRole('button', { name: '로그인' });
    await user.click(loginButton);

    // then
    // 게시글 목록 화면(/board)으로 이동
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/board');
    });
  });

  test.skip('이메일/비밀번호를 입력하고 로그인 API가 401을 응답하는 상태에서 로그인 버튼을 누르면 "이메일 또는 비밀번호가 일치하지 않습니다"는 문구를 화면에 보여준다', async () => {  
      //given
      //응답이 401로 오도록
      server.use(
        http.post(LOGIN_URL, () => {
          return HttpResponse.json(
            {
              message: '이메일 또는 비밀번호가 일치하지 않습니다',
            },
            { status: 401 },
          );
        }),
      );

      const user = userEvent.setup();

      render(
        <MemoryRouter>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </MemoryRouter>,
      );

      //이메일 & 비밀번호 입력
      await user.type(screen.getByLabelText('이메일'), 'test@example.com');
      await user.type(screen.getByLabelText('비밀번호'), 'Test1234**');

      //when
      //로그인 버튼을 누르면
      const loginButton = screen.getByRole('button', { name: '로그인' });
      await user.click(loginButton);

      //then
      // "이메일 또는 비밀번호가 일치하지 않습니다"는 메세지를 화면에 보여준다
      await waitFor(() => {
        expect(screen.getByText('이메일 또는 비밀번호가 일치하지 않습니다')).toBeInTheDocument();
      });

  });

  test.skip('이메일 입력 & 비밀번호 입력하고, 로그인 API가 실패(500)을 응답하도록 세팅된 상태에서 로그인 버튼을 누르면 "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요"라는 문구를 화면에 보여준다', async () => {  
      //given
      //응답이 500로 오도록
      server.use(
        http.post(LOGIN_URL, () => {
          return HttpResponse.json(
            null,
            { status: 500 },
          );
        }),
      );

      const user = userEvent.setup();

      render(
        <MemoryRouter>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </MemoryRouter>,
      );

      //이메일 & 비밀번호 입력
      await user.type(screen.getByLabelText('이메일'), 'test@example.com');
      await user.type(screen.getByLabelText('비밀번호'), 'Test1234**');

      //when
      //로그인 버튼을 누르면
      const loginButton = screen.getByRole('button', { name: '로그인' });
      await user.click(loginButton);

      //then
      // "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요"는 메세지를 화면에 보여준다
      await waitFor(() => {
        expect(screen.getByText('일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요')).toBeInTheDocument();
      });

  });
});
