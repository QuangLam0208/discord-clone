
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('resetPasswordForm');
    const btnSubmit = document.getElementById('btn-submit');

    // Get email from URL
    const urlParams = new URLSearchParams(window.location.search);
    const email = urlParams.get('email');

    if (!email) {
        Swal.fire({
            icon: 'error',
            title: 'Thiếu thông tin',
            text: 'Không tìm thấy email. Vui lòng thử lại từ bước quên mật khẩu.',
            allowOutsideClick: false
        }).then(() => {
            window.location.href = '/forgot-password';
        });
        return;
    }

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const otpCode = document.getElementById('otpCode').value.trim();
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (!otpCode || !newPassword) {
            Swal.fire('Lỗi', 'Vui lòng nhập đủ thông tin', 'error');
            return;
        }

        if (newPassword !== confirmPassword) {
            Swal.fire('Lỗi', 'Mật khẩu nhập lại không khớp', 'error');
            return;
        }

        btnSubmit.disabled = true;
        btnSubmit.innerText = 'Đang xử lý...';

        try {
            await AuthService.resetPassword(email, otpCode, newPassword);

            await Swal.fire({
                icon: 'success',
                title: 'Thành công',
                text: 'Mật khẩu đã được đổi. Vui lòng đăng nhập lại.',
                timer: 2000,
                showConfirmButton: false
            });

            window.location.href = '/login';

        } catch (error) {
            Swal.fire('Lỗi', error.message || 'Đặt lại mật khẩu thất bại', 'error');
        } finally {
            btnSubmit.disabled = false;
            btnSubmit.innerText = 'Đổi mật khẩu';
        }
    });
});
