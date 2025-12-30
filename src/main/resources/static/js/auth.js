document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const btnLogin = document.getElementById('btn-login');

    if(loginForm) {
        loginForm.addEventListener('submit', function(event) {
            event.preventDefault();

            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;

            btnLogin.innerText = "Đang đăng nhập...";
            btnLogin.disabled = true;

            fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: username, password: password })
            })
                .then(async response => {
                    const data = await response.json();
                    if (!response.ok) {
                        const serverMessage = data && (data.message || data.error);
                        let message;

                        if (serverMessage) {
                            message = serverMessage;
                        } else if (response.status === 400 || response.status === 401 || response.status === 403) {
                            // Lỗi xác thực: giữ nguyên thông báo hiện tại
                            message = "Sai tài khoản hoặc mật khẩu";
                        } else {
                            // Lỗi khác (server, mạng, ...): thông báo chung
                            message = "Đã xảy ra lỗi. Vui lòng thử lại sau.";
                        }

                        throw new Error(message);
                    }
                    // Lưu token
                    localStorage.setItem('accessToken', data.token || data.accessToken);
                    localStorage.setItem('username', data.username);

                    // Thông báo nhẹ -> Chuyển trang
                    const Toast = Swal.mixin({
                        toast: true, position: 'top-end',
                        showConfirmButton: false, timer: 1500,
                        timerProgressBar: true,
                        didOpen: (toast) => {
                            toast.addEventListener('mouseenter', Swal.stopTimer)
                            toast.addEventListener('mouseleave', Swal.resumeTimer)
                        }
                    });

                    Toast.fire({
                        icon: 'success',
                        title: 'Đăng nhập thành công'
                    }).then(() => {
                        window.location.href = '/';
                    });
                })
                .catch(error => {
                    Swal.fire({
                        icon: 'error',
                        title: 'Đăng nhập thất bại',
                        text: error.message,
                        background: '#36393f', color: '#fff'
                    });
                    btnLogin.innerText = "Đăng nhập";
                    btnLogin.disabled = false;
                });
        });
    }
});