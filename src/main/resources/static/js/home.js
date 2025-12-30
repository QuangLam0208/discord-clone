document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('accessToken');
    const username = localStorage.getItem('username');

    if (!token) {
        window.location.href = '/login';
        return;
    }

    // Utility: authenticated fetch that attaches the Authorization header
    function authFetch(url, options = {}) {
        const headers = options.headers instanceof Headers
            ? options.headers
            : new Headers(options.headers || {});

        if (token) {
            headers.set('Authorization', 'Bearer ' + token);
        }

        return fetch(url, {
            ...options,
            headers
        });
    }

    // Expose helper globally for other scripts on the home page
    window.authFetch = authFetch;
    // Hiện giao diện
    document.getElementById('app').style.display = 'flex';

    // --- 1. XỬ LÝ AVATAR ĐỘNG (THEO TÊN) ---
    const displayName = username || "User";
    document.getElementById('user-display').innerText = displayName;

    // Tạo URL ảnh từ UI Avatars (Nền ngẫu nhiên, chữ trắng)
    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&color=fff&size=128&bold=true`;

    // Gán vào tất cả chỗ nào có class user-avatar
    const avatars = document.querySelectorAll('.user-avatar');
    avatars.forEach(avatar => {
        avatar.style.backgroundImage = `url('${avatarUrl}')`;
        avatar.style.backgroundSize = 'cover';
        avatar.style.backgroundPosition = 'center';
        avatar.style.backgroundColor = 'transparent'; // Bỏ màu tím mặc định
        avatar.innerHTML = ''; // Xóa nội dung cũ
    });

    // --- 2. LOGIC MENU & LOGOUT ---
    const btnSettings = document.getElementById('btn-settings');
    const settingsPopup = document.getElementById('settings-popup');
    const btnLogoutAction = document.getElementById('btn-logout-action');

    // Toggle menu
    if (btnSettings && settingsPopup) {
        // Ensure the settings control is focusable and announced as a button
        if (!btnSettings.hasAttribute('tabindex')) {
            btnSettings.setAttribute('tabindex', '0');
        }
        if (!btnSettings.hasAttribute('role')) {
            btnSettings.setAttribute('role', 'button');
        }

        const toggleSettingsPopup = (event) => {
            event.stopPropagation();
            settingsPopup.classList.toggle('show');
        };

        btnSettings.addEventListener('click', (e) => {
            toggleSettingsPopup(e);
        });

        btnSettings.addEventListener('keydown', (e) => {
            const key = e.key || e.code;
            if (key === 'Enter' || key === ' ' || key === 'Spacebar') {
                e.preventDefault();
                toggleSettingsPopup(e);
            }
        });
    }

    // Đóng menu khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (settingsPopup && !settingsPopup.contains(e.target) && e.target !== btnSettings) {
            settingsPopup.classList.remove('show');
        }
    });

    // Logout có xác nhận
    if(btnLogoutAction) {
        btnLogoutAction.addEventListener('click', () => {
            Swal.fire({
                title: 'Đăng xuất?',
                text: "Bạn có chắc chắn muốn đăng xuất không?",
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#ed4245', // Màu đỏ discord
                cancelButtonColor: '#7289da',
                confirmButtonText: 'Đăng xuất',
                cancelButtonText: 'Hủy',
                background: '#36393f', color: '#fff'
            }).then((result) => {
                if (result.isConfirmed) {
                    localStorage.clear();
                    window.location.href = '/login';
                }
            });
        });
    }
});