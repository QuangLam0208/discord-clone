// register.js - gắn sự kiện cho trang đăng ký
document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('registerForm');
    const btnRegister = document.getElementById('btn-register');

    if (!registerForm) return;

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

        [emailError, usernameError, passwordError, confirmPasswordError].forEach(el => {
            if (el) el.textContent = '';
        });
        [emailInput, usernameInput, passwordInput, confirmPasswordInput].forEach(el => {
            if (el) el.setAttribute('aria-invalid', 'false');
        });

        const email = (emailInput?.value || '').trim();
        const username = (usernameInput?.value || '').trim();
        const displayName = (displayNameInput?.value || username);
        const password = passwordInput?.value || '';
        const confirmPassword = confirmPasswordInput?.value || '';

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
        if (hasError) return;

        btnRegister.innerText = 'Đang xử lý...';
        btnRegister.disabled = true;

        try {
            // Backend trả token + user và đã đăng nhập
            await AuthService.register(username, email, password, displayName);
            await AuthService.showToastSuccess('Đăng ký thành công');
            window.location.href = '/';
        } catch (error) {
            await AuthService.showErrorAlert(error.message, 'Đăng ký thất bại');
        } finally {
            btnRegister.innerText = 'Tiếp tục';
            btnRegister.disabled = false;
        }
    });
});