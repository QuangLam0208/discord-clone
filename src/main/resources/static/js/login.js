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
            // 1) Đăng nhập → lưu token vào localStorage
            await AuthService.login(username, password);
            await AuthService.showToastSuccess('Đăng nhập thành công');

            // 2) Lấy thông tin user để quyết định redirect theo role
            const me = await AuthService.getCurrentUser();
            const roles = (me?.roles || []).map(r => (typeof r === 'string' ? r : r.name));
            const isAdmin = roles.includes('ADMIN') || roles.includes('MODERATOR');

            // 3) Admin → /admin; người dùng thường → /home
            window.location.href = isAdmin ? '/admin' : '/home';
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