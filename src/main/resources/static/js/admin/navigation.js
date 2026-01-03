// ====== SPA load fragments & sidebar navigation ======
(function () {
    const routes = {
        dashboard: '/admin/_dashboard',
        users: '/admin/_users',
        servers: '/admin/_servers',
        messages: '/admin/_messages',
        audit: '/admin/_audit'
    };

    async function loadSection(section, params={}) {
        const content = document.getElementById('admin-content');
        const title = document.getElementById('admin-page-title');
        if (!content) return;

        // Active sidebar
        document.querySelectorAll('#admin-nav .nav-item').forEach(a=>{
            a.classList.toggle('active', a.dataset.section===section);
        });

        // Cập nhật icon + title
        const labelMap = {dashboard:'Dashboard', users:'Users', servers:'Servers', messages:'Messages', audit:'Audit'};
        const iconMap = {dashboard:'fa-gauge', users:'fa-user-shield', servers:'fa-server', messages:'fa-envelope', audit:'fa-clipboard-list'};
        if (title) {
            title.innerHTML = `<i class="fa-solid ${iconMap[section]||'fa-gauge'}"></i><span>${labelMap[section]||'Dashboard'}</span>`;
        }

        // Build query
        const url = new URL(routes[section], window.location.origin);
        Object.entries(params).forEach(([k,v]) => { if (v!==undefined && v!==null) url.searchParams.set(k, v); });
        // Bust cache để luôn lấy fragment mới
        url.searchParams.set('_', Date.now());

        // Loading
        content.innerHTML = `<div class="muted" style="padding:20px;">Đang tải ${labelMap[section]||section}...</div>`;

        try {
            const res = await fetch(url, {
                headers: { 'X-Requested-With': 'XMLHttpRequest', 'Cache-Control': 'no-cache' },
                cache: 'no-store'
            });
            if (!res.ok) throw new Error(`Load thất bại: ${res.status}`);
            const html = await res.text();
            content.innerHTML = html;

            // Gắn handlers sau khi inject fragment
            if (typeof window.attachAdminHandlers === 'function') {
                window.attachAdminHandlers();
            }
        } catch (e) {
            console.error('Load section error', e);
            content.innerHTML = `<div class="muted" style="padding:20px;color:#ed4245">Không tải được nội dung (${e.message})</div>`;
        }
    }

    // Expose để dùng trong template (onclick="loadSection('servers', {...})")
    window.loadSection = loadSection;

    // Expose routes nếu cần debug
    window.__adminRoutes = routes;
})();