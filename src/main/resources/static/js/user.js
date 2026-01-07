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

            const bannerDisplay = document.getElementById('banner-display');
            if (bannerDisplay && user.bannerColor) {
                bannerDisplay.style.backgroundColor = user.bannerColor;
            }

            elements.userAvatars.forEach(el => {
                el.style.backgroundImage = `url('${avatarUrl}')`;
                el.style.backgroundSize = 'cover';
                el.style.backgroundPosition = 'center';
                el.style.backgroundColor = 'transparent';
                el.innerHTML = '';
            });

            // Handle Admin Button logic
            const btnAdmin = document.getElementById('btn-admin-manage');
            if (btnAdmin) {
                if (user.roles && user.roles.includes('ADMIN')) {
                    btnAdmin.style.display = 'flex';
                } else {
                    btnAdmin.style.display = 'none';
                }
            }
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
    const colorInput = document.getElementById('edit-profile-color');
    if (colorInput) {
        colorInput.value = user.bannerColor || "#5865f2";
    }

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
// Hàm gọi API lưu thông tin
async function saveUserProfile() {
    console.log("Saving user profile..."); // DEBUG LOG
    const nameInput = document.getElementById('edit-profile-displayname');
    const newDisplayName = nameInput ? nameInput.value : "";

    const bioInput = document.getElementById('edit-profile-bio');
    const newBio = bioInput ? bioInput.value : "";

    const fileInput = document.getElementById('edit-profile-file');
    const file = fileInput ? fileInput.files[0] : null;

    console.log("Params:", { newDisplayName, newBio, file }); // DEBUG LOG

    const formData = new FormData();
    formData.append("displayName", newDisplayName);
    formData.append("bio", newBio);
    const colorInput = document.getElementById('edit-profile-color');
    if (colorInput) {
        formData.append("bannerColor", colorInput.value);
    }
    if (file) {
        formData.append("file", file);
    }

    try {
        // Sử dụng Api.upload để tự động kèm Token và Header phù hợp
        const updatedUser = await Api.upload('/api/users/profile', formData);

        if (updatedUser) {
            // Cập nhật lại giao diện ngay lập tức
            if (window.state) window.state.currentUser = updatedUser;

            // Update UI elements from global elements or by ID if missing
            const disp = document.getElementById('main-displayname');
            if (disp) disp.innerText = updatedUser.displayName;

            const bannerDisplay = document.getElementById('banner-display');
            if (bannerDisplay && updatedUser.bannerColor) {
                bannerDisplay.style.backgroundColor = updatedUser.bannerColor;
            }

            // Reload lại avatar ở góc trái dưới
            if (typeof updateUserAvatarUI === "function") {
                updateUserAvatarUI(updatedUser.avatarUrl, updatedUser.displayName);
            }

            // Close modal using specific ID (assuming defined in profile.html/user.js)
            const modal = document.getElementById('editProfileModal');
            if (modal) modal.classList.remove('active');

            // Thông báo thành công và RELOAD trang
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'success',
                    title: 'Đã cập nhật hồ sơ!',
                    timer: 800,
                    showConfirmButton: false,
                    background: '#313338',
                    color: '#fff'
                }).then(() => {
                    location.reload();
                });
            } else {
                location.reload();
            }
        }
    } catch (e) {
        console.error("Save profile error:", e);
        if (typeof Swal !== 'undefined') {
            Swal.fire({ icon: 'error', title: 'Lỗi', text: e.message || "Có lỗi xảy ra", background: '#313338', color: '#fff' });
        } else {
            alert("Lỗi cập nhật hồ sơ: " + e.message);
        }
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