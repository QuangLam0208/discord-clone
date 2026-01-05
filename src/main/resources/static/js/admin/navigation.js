// ====== SPA load fragments & sidebar navigation ======
(function () {
    const routes = {
        dashboard: '/admin/_dashboard',
        users: '/admin/_users',
        servers: '/admin/_servers',
        'messages-channel': '/admin/_messages_channel',
        'messages-direct': '/admin/_messages_direct',
        audit: '/admin/_audit'
    };

    async function loadSection(section, params = {}) {
        const content = document.getElementById('admin-content');
        const title = document.getElementById('admin-page-title');
        if (!content) return;

        // Active sidebar
        document.querySelectorAll('#admin-nav .nav-item').forEach(a => a.classList.remove('active'));
        const activeItem = document.querySelector(`#admin-nav .nav-item[data-section="${section}"]`);
        activeItem?.classList.add('active');

        // Nếu là submenu Messages, mở group
        if (section.startsWith('messages-')) {
            document.getElementById('nav-group-messages')?.classList.add('open');
        }

        const labelMap = {
            dashboard: 'Dashboard', users: 'Users', servers: 'Servers',
            'messages-channel': 'Messages · Channel', 'messages-direct': 'Messages · Direct',
            audit: 'Audit'
        };
        const iconMap = {
            dashboard: 'fa-gauge', users: 'fa-user-shield', servers: 'fa-server',
            'messages-channel': 'fa-envelope', 'messages-direct': 'fa-envelope',
            audit: 'fa-clipboard-list'
        };
        if (title) {
            title.innerHTML = `<i class="fa-solid ${iconMap[section] || 'fa-gauge'}"></i><span>${labelMap[section] || 'Dashboard'}</span>`;
        }

        const path = routes[section] || routes['messages-channel'];
        const url = new URL(path, window.location.origin);
        Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null) url.searchParams.set(k, v); });
        url.searchParams.set('_', Date.now());

        content.innerHTML = `<div class="muted" style="padding:20px;">Đang tải ${labelMap[section] || section}...</div>`;

        try {
            const res = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest', 'Cache-Control': 'no-cache' }, cache: 'no-store' });
            if (!res.ok) throw new Error(`Load thất bại: ${res.status}`);
            const html = await res.text();
            content.innerHTML = html;
            if (typeof window.attachAdminHandlers === 'function') window.attachAdminHandlers();
        } catch (e) {
            console.error('Load section error', e);
            content.innerHTML = `<div class="muted" style="padding:20px;color:#ed4245">Không tải được nội dung (${e.message})</div>`;
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const nav = document.getElementById('admin-nav');
        nav?.addEventListener('click', (e) => {
            const toggle = e.target.closest('.nav-group-toggle');
            if (toggle) {
                e.preventDefault();
                toggle.closest('.nav-group')?.classList.toggle('open');
                return;
            }
            const a = e.target.closest('a.nav-item[data-section]');
            if (!a) return;
            e.preventDefault();
            const section = a.dataset.section;
            loadSection(section);
            history.replaceState({}, '', `/admin#${section}`);
        });

        const hashSection = (location.hash || '').replace('#', '');
        if (hashSection === 'messages') {
            loadSection('messages-channel'); // mặc định vào Channel
        } else {
            loadSection(hashSection || 'dashboard');
        }
    });

    window.changeAuditPage = function (page, keyword, action, adminId) {
        if (page < 0) return false;
        const params = { page: page };
        if (keyword) params.keyword = keyword;
        if (action) params.action = action;
        if (adminId !== null && adminId !== 'null') params.adminId = adminId;
        loadSection('audit', params);
        return false;
    };

    window.changeAuditPageFromData = function (el) {
        const page = el.getAttribute('data-page');
        const keyword = el.getAttribute('data-keyword');
        const action = el.getAttribute('data-action');
        const adminId = el.getAttribute('data-admin-id');
        return window.changeAuditPage(page, keyword, action, adminId);
    };

    window.loadSection = loadSection;
    window.__adminRoutes = routes;
})();