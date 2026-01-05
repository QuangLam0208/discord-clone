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
    const btnProfileAction = document.getElementById('btn-profile-action');
    if (btnProfileAction) {
        btnProfileAction.addEventListener('click', () => {
            document.getElementById('settings-popup').classList.remove('show');
            openEditProfileModal();
        });
    }
    const fileInput = document.getElementById('edit-profile-file');
    if (fileInput) {
        fileInput.addEventListener('change', function (e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function (evt) {
                    document.getElementById('edit-profile-avatar-preview').src = evt.target.result;
                }
                reader.readAsDataURL(file);
            }
        });
    }

    // Live Preview for Display Name
    const displayNameInput = document.getElementById('edit-profile-displayname');
    if (displayNameInput) {
        displayNameInput.addEventListener('input', (e) => {
            const val = e.target.value;
            const preview = document.getElementById('preview-displayname-text');
            if (preview) {
                preview.innerText = val ? val : (state.currentUser ? state.currentUser.username : '');
            }
        });
    }

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
                    if (typeof DM !== 'undefined') {
                        DM.reset();
                    }
                    localStorage.clear();
                    window.location.href = '/login';
                }
            });
        });
    }
});

function openEditProfileModal() {
    const user = state.currentUser;
    if (!user) return;

    // Điền thông tin vào input
    document.getElementById('edit-profile-displayname').value = user.displayName || "";
    document.getElementById('edit-profile-username').value = user.username || "";
    document.getElementById('edit-profile-email').value = user.email || "";
    document.getElementById('edit-profile-bio').value = user.bio || "";

    // Điền ảnh avatar
    const previewDisplay = document.getElementById('preview-displayname-text');
    const previewUsername = document.getElementById('preview-username-text');

    if (previewDisplay) previewDisplay.innerText = user.displayName || user.username;
    if (previewUsername) previewUsername.innerText = user.username;

    const avatarPreview = document.getElementById('edit-profile-avatar-preview');
    const nameForAvatar = user.displayName || user.username || 'User';
    avatarPreview.src = user.avatarUrl
        ? user.avatarUrl
        : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;

    const modal = document.getElementById('editProfileModal');
    if (modal) modal.classList.add('active');
}

// Hàm gọi API lưu thông tin
async function saveUserProfile() {
    const newDisplayName = document.getElementById('edit-profile-displayname').value;
    const newBio = document.getElementById('edit-profile-bio').value;
    const fileInput = document.getElementById('edit-profile-file');
    const file = fileInput.files[0];

    const formData = new FormData();
    formData.append("displayName", newDisplayName);
    formData.append("bio", newBio);
    if (file) {
        formData.append("file", file);
    }

    try {
        // Gửi request (Đảm bảo đường dẫn API đúng với Backend của bạn)
        const response = await fetch('/api/users/profile', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const updatedUser = await response.json();

            // Cập nhật lại giao diện ngay lập tức
            state.currentUser = updatedUser;
            if (elements.userDisplay) elements.userDisplay.innerText = updatedUser.displayName;

            // Reload lại avatar ở góc trái dưới
            if (typeof updateUserAvatarUI === "function") {
                updateUserAvatarUI(updatedUser.avatarUrl, updatedUser.displayName);
            } else {
                // Nếu chưa có hàm tách riêng, reload trang cho nhanh
                location.reload();
            }

            closeModal('editProfileModal');

            // Thông báo thành công (Nếu có Swal)
            if (typeof Swal !== 'undefined') {
                Swal.fire({ icon: 'success', title: 'Đã cập nhật hồ sơ!', timer: 1500, showConfirmButton: false, background: '#313338', color: '#fff' });
            }
        } else {
            alert("Lỗi cập nhật hồ sơ!");
        }
    } catch (e) {
        console.error(e);
        alert("Có lỗi xảy ra.");
    }
}

// Global helper to toggle bio edit
function enableBioEdit() {
    const bioInput = document.getElementById('edit-profile-bio');
    if (bioInput) {
        bioInput.removeAttribute('readonly');
        bioInput.style.cursor = 'text';
        bioInput.focus();
    }
}