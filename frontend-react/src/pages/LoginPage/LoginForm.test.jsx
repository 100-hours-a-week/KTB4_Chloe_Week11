import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginForm from './LoginForm';

describe('LoginForm', () => {
  it('초기 렌더링 시 이메일/비밀번호 입력창이 비어있고 로그인 버튼이 비활성화되어 있다', () => {
    // Given: 로그인 화면이 초기상태일 때
    const handleSubmit = vi.fn();

    // When: 화면이 렌더링 되면
    render(<LoginForm onSubmit={handleSubmit} />);

    // Then: 입력창이 모두 비어있고 로그인 버튼이 비활성화
    const emailInput = screen.getByLabelText('이메일');
    const passwordInput = screen.getByLabelText('비밀번호');
    expect(emailInput).toHaveValue('');
    expect(passwordInput).toHaveValue('');

    const loginButton = screen.getByRole('button', { name: '로그인' });
    expect(loginButton).toBeDisabled();
  });

  it('로그인 화면에서	유효한 형식의 이메일을 입력하면	이메일 관련 helperText가 뜨지 않는다', async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);

    //when - 유효한 형식의 이메일을 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('이메일'), 'test@example.com');

    //then - 이메일 관련 helperText가 뜨지 않음 
    expect(screen.queryByText('유효한 이메일 주소를 입력해주세요.')).not.toBeInTheDocument();
  });

  it('로그인 화면에서	유효하지 않은 형식의 이메일을 입력하면	이메일 관련 helperText가 뜬다', async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);
    
    //when - 유효하지 않은 형식의 이메일을 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('이메일'), 'testexample.com');

    //then - 이메일 관련 helperText가 뜸
    expect(screen.queryByText('유효한 이메일 주소를 입력해주세요.')).toBeInTheDocument();
  });

  it('로그인 화면에서	유효한 형식의 비밀번호를 입력하면	비밀번호 관련 helperText가 뜨지 않는다', async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);
    
    //when - 유효한 형식의 비밀번호을 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('비밀번호'), 'Test1234**');

    //then - 비밀번호 관련 helperText가 뜨지 않음 
    expect(screen.queryByText('비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.')).not.toBeInTheDocument();
  });

  it('로그인 화면에서	유효한하지 않은형식의 비밀번호를 입력하면	비밀번호 관련 helperText가 뜬다', async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);
  
    //when - 유효하지 않은 형식의 비밀번호을 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('비밀번호'), 'test1234');

    //then - 비밀번호 관련 helperText가 뜸
    expect(screen.queryByText('비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.')).toBeInTheDocument();
  });

  it('로그인 화면에서	이메일과 비밀번호 둘 다 유효한 형식으로 입력하면 로그인 버튼이 활성화된다',async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);

    //when - 유효한 형식의 이메일과 비밀번호를 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('이메일'), 'test@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'Test1234**');

    //then - 로그인 버튼이 활성화됨
    const loginButton = screen.getByRole('button', { name: '로그인' });
    expect(loginButton).not.toBeDisabled();
  });

  it('로그인 화면에서	이메일과 비밀번호 중 하나라도 이상 유효하지 않은 형식으로 입력하면 로그인 버튼이 비활성화된다',async () => {
    //given - 로그인 화면에서 
    const handleSubmit = vi.fn();
    render(<LoginForm onSubmit={handleSubmit} />);

    

    //when - 유효하지 않은 형식의 이메일과 비밀번호를 입력 
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('이메일'), 'testexample.com');
    await user.type(screen.getByLabelText('비밀번호'), 'test1234');

    //then - 로그인 버튼이 비활성화됨
    const loginButton = screen.getByRole('button', { name: '로그인' });
    expect(loginButton).toBeDisabled();
  });
});
