package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterWithOtpRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 30, message = "Username phải từ 3-30 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String displayName;

    @NotBlank(message = "OTP không được để trống")
    private String otpCode;
}

