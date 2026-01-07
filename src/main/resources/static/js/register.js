// register.js - gắn sự kiện cho trang đăng ký
document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('registerForm');
    const btnRegister = document.getElementById('btn-register');

    if (!registerForm) return;

    let isOtpSent = false;
    const otpSection = document.getElementById('otpSection');
    const otpInput = document.getElementById('otpCode');
    const otpError = document.getElementById('otpError');

    registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const emailInput = document.getElementById('email');
        const usernameInput = document.getElementById('username');
        const displayNameInput = document.getElementById('displayName');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');

        const emailError = document.getElementById('emailError');
        const usernameError = document.getElementById('usernameError');
        const passwordError = document.getElementById('passwordError');
        const confirmPasswordError = document.getElementById('confirmPasswordError');

        // Clear previous errors
        [emailError, usernameError, passwordError, confirmPasswordError, otpError].forEach(el => {
            if (el) el.textContent = '';
        });
        [emailInput, usernameInput, passwordInput, confirmPasswordInput, otpInput].forEach(el => {
            if (el) el.setAttribute('aria-invalid', 'false');
        });

        const email = (emailInput?.value || '').trim();
        const username = (usernameInput?.value || '').trim();
        const displayName = (displayNameInput?.value || username);
        const password = passwordInput?.value || '';
        const confirmPassword = confirmPasswordInput?.value || '';
        const otpCode = (otpInput?.value || '').trim();

        // Basic Validation
        let hasError = false;
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            if (emailError) emailError.textContent = 'Email không hợp lệ';
            if (emailInput) emailInput.setAttribute('aria-invalid', 'true');
            hasError = true;
        }
        if (!username) {
            if (usernameError) usernameError.textContent = 'Vui lòng nhập username';
            if (usernameInput) usernameInput.setAttribute('aria-invalid', 'true');
            hasError = true;
        }
        if (!password) {
            if (passwordError) passwordError.textContent = 'Vui lòng nhập mật khẩu';
            if (passwordInput) passwordInput.setAttribute('aria-invalid', 'true');
            hasError = true;
        }
        if (password !== confirmPassword) {
            if (confirmPasswordError) confirmPasswordError.textContent = 'Mật khẩu nhập lại không khớp';
            if (confirmPasswordInput) confirmPasswordInput.setAttribute('aria-invalid', 'true');
            hasError = true;
        }

        if (isOtpSent) {
            if (!otpCode) {
                if (otpError) otpError.textContent = 'Vui lòng nhập mã OTP';
                if (otpInput) otpInput.setAttribute('aria-invalid', 'true');
                hasError = true;
            }
        }

        if (hasError) return;

        btnRegister.disabled = true;

        try {
            if (!isOtpSent) {
                // STEP 1: Send OTP
                btnRegister.innerText = 'Đang gửi OTP...';
                await AuthService.sendOtp(email);

                await AuthService.showToastSuccess('Mã OTP đã được gửi đến email của bạn');

                // Show OTP input
                isOtpSent = true;
                if (otpSection) otpSection.style.display = 'block';
                btnRegister.innerText = 'Hoàn tất đăng ký';

                // Optional: Disable other inputs
                [emailInput, usernameInput, passwordInput, confirmPasswordInput, displayNameInput].forEach(el => {
                    if (el) el.readOnly = true;
                });

            } else {
                // STEP 2: Verify & Register
                btnRegister.innerText = 'Đang đăng ký...';

                await AuthService.registerWithOtp(username, email, password, displayName, otpCode);

                await AuthService.showToastSuccess('Đăng ký thành công');
                setTimeout(() => {
                    window.location.href = '/';
                }, 1000);
            }
        } catch (error) {
            console.error(error);
            if (!isOtpSent) {
                // Error sending OTP
                if (emailError) emailError.textContent = error.message;
                btnRegister.innerText = 'Tiếp tục';
            } else {
                // Error registering
                await AuthService.showErrorAlert(error.message, 'Đăng ký thất bại');
                btnRegister.innerText = 'Hoàn tất đăng ký';
            }
        } finally {
            btnRegister.disabled = false;
        }
    });

    // Helper: Allow editing email if user wants to change it before OTP? 
    // For now, let's keep it simple. If they made a mistake, they can refresh.
});