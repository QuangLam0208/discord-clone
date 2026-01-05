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

    document.addEventListener('DOMContentLoaded', () => {
        const nav = document.getElementById('admin-nav');
        nav?.addEventListener('click', (e) => {
            const a = e.target.closest('a.nav-item[data-section]');
            if (!a) return;
            e.preventDefault();
            const section = a.dataset.section;
            window.loadSection(section);
            history.replaceState({}, '', `/admin#${section}`);
        });

        const displayName = localStorage.getItem('displayName') || localStorage.getItem('username') || 'Admin';
        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&color=fff&size=128&bold=true`;
        const avatarEl = document.getElementById('admin-avatar'); if (avatarEl) avatarEl.style.backgroundImage = `url('${avatarUrl}')`;
        const nameEl = document.getElementById('admin-name'); if (nameEl) nameEl.innerText = displayName;

        const btnLogout = document.getElementById('btn-admin-logout');
        btnLogout?.addEventListener('click', () => {
            Swal.fire({ title: 'Đăng xuất?', icon: 'question', showCancelButton: true, confirmButtonText: 'Đăng xuất', cancelButtonText: 'Hủy', confirmButtonColor: '#ed4245', background: '#313338', color: '#fff' })
                .then(r => { if (r.isConfirmed) { localStorage.clear(); window.location.href = '/login'; } });
        });

        const hashSection = (location.hash || '').replace('#', '') || 'dashboard';
        window.loadSection(hashSection);
    });

    // Expose
    window.attachAdminHandlers = attachAdminHandlers;
})();