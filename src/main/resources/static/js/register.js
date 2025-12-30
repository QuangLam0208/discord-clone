document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const btnRegister = document.getElementById('btn-register');

    registerForm.addEventListener('submit', function(event) {
        event.preventDefault();

        // 1. Lấy dữ liệu
        const email = document.getElementById('email').value.trim();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        let displayName = document.getElementById('displayName').value.trim();

        // Validate định dạng email đơn giản phía client
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(email)) {
            Swal.fire({
                icon: 'error',
                title: 'Email không hợp lệ',
                text: 'Vui lòng nhập đúng định dạng email (ví dụ: ten@vidu.com).',
                background: '#36393f', color: '#fff'
            });
            return;
        }
        // Tự động điền displayName nếu trống
        if (!displayName) displayName = username;

        // 2. Validate phía Client
        if (password !== confirmPassword) {
            Swal.fire({
                icon: 'error',
                title: 'Mật khẩu không khớp!',
                text: 'Vui lòng kiểm tra lại mật khẩu nhập lại.',
                background: '#36393f', color: '#fff'
            });
            return;
        }

        // Khóa nút để tránh spam
        const originalBtnText = btnRegister.innerText;
        btnRegister.innerText = "Đang xử lý...";
        btnRegister.disabled = true;

        const payload = {
            username: username,
            email: email,
            password: password,
            displayName: displayName
        };

        // 3. Gọi API
        fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(async response => {
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || "Đăng ký thất bại");
                return data;
            })
            .then(() => {
                // Thành công -> Thông báo đẹp -> Chuyển trang
                Swal.fire({
                    icon: 'success',
                    title: 'Đăng ký thành công!',
                    text: 'Bạn sẽ được chuyển đến trang đăng nhập.',
                    background: '#36393f', color: '#fff',
                    timer: 2000,
                    showConfirmButton: false
                }).then(() => {
                    window.location.href = "/login";
                });
            })
            .catch(error => {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: error.message,
                    background: '#36393f', color: '#fff'
                });
                btnRegister.innerText = originalBtnText;
                btnRegister.disabled = false;
            });
    });
});