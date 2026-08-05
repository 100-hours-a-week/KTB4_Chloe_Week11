package homework.week4.User.dto;

import lombok.Getter;

@Getter
public class UserDeleteResponseDto {
    private String nickname;
    private Boolean is_member;

    public UserDeleteResponseDto(String nickname, Boolean isMember) {
        this.nickname = nickname;
        this.is_member = isMember;
    }
}
