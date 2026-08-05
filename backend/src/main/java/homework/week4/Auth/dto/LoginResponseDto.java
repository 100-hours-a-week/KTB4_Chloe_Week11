package homework.week4.Auth.dto;

import homework.week4.Security.JWT.JwtToken;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginResponseDto {
    private JwtToken jwtToken;
    private String profileImage;

    public LoginResponseDto(
            JwtToken jwtToken,
            String profileImage
    ) {

        this.jwtToken = jwtToken;
        this.profileImage = profileImage;
    }
}
