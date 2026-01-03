// USER MODULE

// 1. LOAD USER INFO
async function loadMe() {
    try {
        const user = await Api.get('/api/users/me');
        if (user) {
            state.currentUser = user;
            if (elements.userDisplay) elements.userDisplay.innerText = user.displayName || user.username;
            const nameForAvatar = user.displayName || user.username || 'User';
            const avatarUrl = user.avatarUrl
                ? user.avatarUrl
                : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;
            elements.userAvatars.forEach(el => {
                el.style.backgroundImage = `url('${avatarUrl}')`;
                el.style.backgroundSize = 'cover';
                el.style.backgroundPosition = 'center';
                el.style.backgroundColor = 'transparent';
                el.innerHTML = '';
            });
        }
    } catch (e) {
        console.error("Lỗi load user:", e);
    }
}

// 2. USER SETTINGS & LOGOUT
document.addEventListener('DOMContentLoaded', () => {
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
    }

    // Đóng popup khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (settingsPopup && !settingsPopup.contains(e.target) && e.target !== btnSettings) {
            settingsPopup.classList.remove('show');
        }
    });

    // Logout với SweetAlert2
    if (btnLogoutAction) {
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
                background: '#36393f',
                color: '#fff'
            }).then((result) => {
                if (result.isConfirmed) {
                    localStorage.clear();
                    window.location.href = '/login';
                }
            });
        });
    }
});
