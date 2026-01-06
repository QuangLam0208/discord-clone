
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('forgotPasswordForm');
    const btnSubmit = document.getElementById('btn-submit');

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value.trim();

        if (!email) {
            Swal.fire('Lỗi', 'Vui lòng nhập email', 'error');
            return;
        }

        btnSubmit.disabled = true;
        btnSubmit.innerText = 'Đang gửi...';

        try {
            await AuthService.forgotPassword(email);
            // On success, redirect to reset password page with email param
            window.location.href = `/reset-password?email=${encodeURIComponent(email)}`;
        } catch (error) {
            Swal.fire('Lỗi', error.message || 'Không thể gửi yêu cầu', 'error');
        } finally {
            btnSubmit.disabled = false;
            btnSubmit.innerText = 'Gửi mã OTP';
        }
    });
});
