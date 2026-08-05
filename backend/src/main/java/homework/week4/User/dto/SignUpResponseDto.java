package homework.week4.User.dto;

import lombok.Getter;

@Getter
public class SignUpResponseDto {
    private Long user_id;

    public SignUpResponseDto(Long userId) {
        this.user_id = userId;
    }
}
