document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('accessToken');
    const displayNameLS = localStorage.getItem('displayName');
    const usernameLS = localStorage.getItem('username');

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

    window.authFetch = authFetch;

    document.getElementById('app').style.display = 'flex';

    // Hiển thị tên: ưu tiên displayName, fallback về username
    const displayName = displayNameLS || usernameLS || 'User';
    document.getElementById('user-display').innerText = displayName;

    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&color=fff&size=128&bold=true`;

    const avatars = document.querySelectorAll('.user-avatar');
    avatars.forEach(avatar => {
        avatar.style.backgroundImage = `url('${avatarUrl}')`;
        avatar.style.backgroundSize = 'cover';
        avatar.style.backgroundPosition = 'center';
        avatar.style.backgroundColor = 'transparent';
        avatar.innerHTML = '';
    });

    const btnSettings = document.getElementById('btn-settings');
    const settingsPopup = document.getElementById('settings-popup');
    const btnLogoutAction = document.getElementById('btn-logout-action');

    if (btnSettings && settingsPopup) {
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

    document.addEventListener('click', (e) => {
        if (settingsPopup && !settingsPopup.contains(e.target) && e.target !== btnSettings) {
            settingsPopup.classList.remove('show');
        }
    });

    if(btnLogoutAction) {
        btnLogoutAction.addEventListener('click', () => {
            Swal.fire({
                title: 'Đăng xuất?',
                text: "Bạn có chắc chắn muốn đăng xuất không?",
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#ed4245',
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