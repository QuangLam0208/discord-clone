
document.addEventListener('DOMContentLoaded', () => {
    const btnSendOtp = document.getElementById('btnSendOtp');
    const changePasswordForm = document.getElementById('changePasswordForm');

    // Step 1: Send OTP
    if (btnSendOtp) {
        btnSendOtp.addEventListener('click', async () => {
            btnSendOtp.disabled = true;
            btnSendOtp.innerText = 'Đang gửi...';

            try {
                const response = await fetch('/api/users/change-password/send-otp', {
                    method: 'POST'
                });

                const data = await response.json();

                if (response.ok) {
                    Swal.fire('Thành công', data.message, 'success');
                    // Switch to Step 2
                    document.getElementById('step1').classList.remove('active');
                    document.getElementById('step1-indicator').classList.add('completed');
                    document.getElementById('step1-indicator').classList.remove('active');

                    document.getElementById('step2').classList.add('active');
                    document.getElementById('step2-indicator').classList.add('active');
                } else {
                    Swal.fire('Lỗi', data.message || 'Không thể gửi OTP', 'error');
                    btnSendOtp.disabled = false;
                    btnSendOtp.innerText = 'Gửi OTP đến Email';
                }
            } catch (error) {
                console.error(error);
                Swal.fire('Lỗi', 'Lỗi kết nối đến server', 'error');
                btnSendOtp.disabled = false;
                btnSendOtp.innerText = 'Gửi OTP đến Email';
            }
        });
    }

    // Step 2: Verify & Change Password
    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const otpCode = document.getElementById('otpCode').value.trim();
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (newPassword !== confirmPassword) {
                Swal.fire('Lỗi', 'Mật khẩu nhập lại không khớp', 'error');
                return;
            }

            const btnSubmit = changePasswordForm.querySelector('button[type="submit"]');
            btnSubmit.disabled = true;
            btnSubmit.innerText = 'Đang xử lý...';

            try {
                // Use form-data/params as controller expects @RequestParam
                const params = new URLSearchParams();
                params.append('otpCode', otpCode);
                params.append('newPassword', newPassword);

                const response = await fetch('/api/users/change-password/verify', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: params
                });

                const data = await response.json();

                if (response.ok) {
                    await Swal.fire('Thành công', 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.', 'success');
                    window.location.href = '/login';
                } else {
                    Swal.fire('Lỗi', data.message || 'Đổi mật khẩu thất bại', 'error');
                    btnSubmit.disabled = false;
                    btnSubmit.innerText = 'Đổi Mật Khẩu';
                }
            } catch (error) {
                console.error(error);
                Swal.fire('Lỗi', 'Lỗi kết nối đến server', 'error');
                btnSubmit.disabled = false;
                btnSubmit.innerText = 'Đổi Mật Khẩu';
            }
        });
    }
});
