// ====== Bootstrapping: sidebar nav, avatar, logout, attach handlers aggregator ======
(function () {

    function attachAdminHandlers() {
        // Gọi từng nhóm handlers theo fragment hiện tại
        if (typeof window.attachAdminServerHandlers === 'function') {
            window.attachAdminServerHandlers();
        }
        if (typeof window.attachAdminUserHandlers === 'function') {
            window.attachAdminUserHandlers();
        }
        if (typeof window.attachAdminMessageHandlers === 'function') {
            window.attachAdminMessageHandlers();
        }
        if (typeof window.attachAdminDMHandlers === 'function') {
            window.attachAdminDMHandlers();
        }
        if (typeof window.attachAdminAuditHandlers === 'function') {
            window.attachAdminAuditHandlers();
        }
    }

    document.addEventListener('DOMContentLoaded', async () => {
        const nav = document.getElementById('admin-nav');
        nav?.addEventListener('click', (e) => {
            const a = e.target.closest('a.nav-item[data-section]');
            if (!a) return;
            e.preventDefault();
            const section = a.dataset.section;
            window.loadSection(section);
            history.replaceState({}, '', `/admin#${section}`);
        });

        // --- User Bar Logic (Admin Side) ---
        try {
            const user = await Api.get('/api/users/me');
            if (user) {
                window.currentUser = user; // Store globally for profile modal
                const userDisplay = document.getElementById('user-display');
                if (userDisplay) userDisplay.innerText = user.displayName || user.username;

                const userTag = document.querySelector('.usertag');
                if (userTag) userTag.innerText = '#' + (user.discriminator || '0000');

                const nameForAvatar = user.displayName || user.username || 'User';
                const avatarUrl = user.avatarUrl
                    ? user.avatarUrl
                    : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;

                const avatarEl = document.querySelector('.user-bar .user-avatar');
                if (avatarEl) {
                    avatarEl.style.backgroundImage = `url('${avatarUrl}')`;
                    avatarEl.style.backgroundSize = 'cover';
                    avatarEl.style.backgroundPosition = 'center';
                }
            }
        } catch (e) {
            console.error("Failed to load admin user info", e);
        }

        // Settings Popup Logic
        const btnSettings = document.getElementById('btn-settings');
        const settingsPopup = document.getElementById('settings-popup');

        if (btnSettings && settingsPopup) {
            btnSettings.addEventListener('click', (e) => {
                e.stopPropagation();
                settingsPopup.classList.toggle('show');
            });
            document.addEventListener('click', (e) => {
                if (!settingsPopup.contains(e.target) && e.target !== btnSettings) {
                    settingsPopup.classList.remove('show');
                }
            });
        }

        // --- Profile Modal Logic (Ported from user.js) ---
        const btnProfileAction = document.getElementById('btn-profile-action');
        if (btnProfileAction) {
            btnProfileAction.addEventListener('click', () => {
                const settingsPopup = document.getElementById('settings-popup');
                if (settingsPopup) settingsPopup.classList.remove('show');
                openAdminEditProfileModal();
            });
        }

        // File Input Preview
        const fileInput = document.getElementById('edit-profile-file');
        if (fileInput) {
            fileInput.addEventListener('change', function (e) {
                const file = e.target.files[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = function (evt) {
                        const preview = document.getElementById('edit-profile-avatar-preview');
                        if (preview) preview.src = evt.target.result;
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
                if (preview && window.currentUser) {
                    preview.innerText = val ? val : window.currentUser.username;
                }
            });
        }

        // Show "Back to App" in Admin
        const btnBackApp = document.getElementById('btn-back-app');
        if (btnBackApp) btnBackApp.style.display = 'flex';

        // Logout
        const btnLogoutAction = document.getElementById('btn-logout-action');
        btnLogoutAction?.addEventListener('click', () => {
            Swal.fire({ title: 'Đăng xuất?', icon: 'question', showCancelButton: true, confirmButtonText: 'Đăng xuất', cancelButtonText: 'Hủy', confirmButtonColor: '#ed4245', background: '#313338', color: '#fff' })
                .then(r => { if (r.isConfirmed) { localStorage.clear(); window.location.href = '/login'; } });
        });

        const hashSection = (location.hash || '').replace('#', '') || 'dashboard';
        window.loadSection(hashSection);
    });

    // --- Global Helpers for Admin Profile ---
    window.openAdminEditProfileModal = function () {
        const user = window.currentUser;
        if (!user) return;

        // Populate inputs
        const inpDisplay = document.getElementById('edit-profile-displayname');
        const inpUser = document.getElementById('edit-profile-username');
        const inpEmail = document.getElementById('edit-profile-email');
        const inpBio = document.getElementById('edit-profile-bio');

        if (inpDisplay) inpDisplay.value = user.displayName || "";
        if (inpUser) inpUser.value = user.username || "";
        if (inpEmail) inpEmail.value = user.email || "";
        if (inpBio) inpBio.value = user.bio || "";

        // Populate preview
        const previewDisplay = document.getElementById('preview-displayname-text');
        const previewUsername = document.getElementById('preview-username-text');
        if (previewDisplay) previewDisplay.innerText = user.displayName || user.username;
        if (previewUsername) previewUsername.innerText = user.username;

        const avatarPreview = document.getElementById('edit-profile-avatar-preview');
        const nameForAvatar = user.displayName || user.username || 'User';
        if (avatarPreview) {
            avatarPreview.src = user.avatarUrl
                ? user.avatarUrl
                : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;
        }

        if (window.openModal) window.openModal('editProfileModal');
    };

    window.saveUserProfile = async function () {
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
            const response = await fetch('/api/users/profile', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const updatedUser = await response.json();
                window.currentUser = updatedUser; // Update global state

                // Update UI
                const userDisplay = document.getElementById('user-display');
                if (userDisplay) userDisplay.innerText = updatedUser.displayName;

                const nameForAvatar = updatedUser.displayName || updatedUser.username || 'User';
                const avatarUrl = updatedUser.avatarUrl
                    ? updatedUser.avatarUrl
                    : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;

                const avatarEl = document.querySelector('.user-bar .user-avatar');
                if (avatarEl) {
                    avatarEl.style.backgroundImage = `url('${avatarUrl}')`;
                }

                if (window.closeModal) window.closeModal('editProfileModal');
                if (window.toastOk) window.toastOk('Đã cập nhật hồ sơ!');
            } else {
                if (window.toastErr) window.toastErr("Lỗi cập nhật hồ sơ!");
            }
        } catch (e) {
            console.error(e);
            if (window.toastErr) window.toastErr("Có lỗi xảy ra.");
        }
    }

    window.enableBioEdit = function () {
        const bioInput = document.getElementById('edit-profile-bio');
        if (bioInput) {
            bioInput.removeAttribute('readonly');
            bioInput.style.cursor = 'text';
            bioInput.focus();
        }
    }

    // Expose
    window.attachAdminHandlers = attachAdminHandlers;
})();