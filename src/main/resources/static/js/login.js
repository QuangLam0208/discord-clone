// login.js - chỉ gắn sự kiện cho trang đăng nhập
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const btnLogin = document.getElementById('btn-login');

    if (!loginForm) return;

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const usernameError = document.getElementById('username-error');
        const passwordError = document.getElementById('password-error');

        const username = (usernameInput?.value || '').trim();
        const password = passwordInput?.value || '';

        if (usernameError) usernameError.textContent = '';
        if (passwordError) passwordError.textContent = '';

        btnLogin.innerText = 'Đang đăng nhập...';
        btnLogin.disabled = true;

        try {
            await AuthService.login(username, password);
            await AuthService.showToastSuccess('Đăng nhập thành công');
            window.location.href = '/';
        } catch (error) {
            if (passwordError) {
                passwordError.textContent = error.message;
            }
            await AuthService.showErrorAlert(error.message, 'Đăng nhập thất bại');
        } finally {
            btnLogin.innerText = 'Đăng nhập';
            btnLogin.disabled = false;
        }
    });
});