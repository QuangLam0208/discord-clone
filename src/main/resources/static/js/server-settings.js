
// --- SERVER SETTINGS MODAL & PLUMBING ---

window.calculateTimeAgo = function (dateString) {
    if (!dateString) return "Không xác định";
    const now = new Date();
    const joined = new Date(dateString);
    const diffTime = Math.abs(now - joined);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 1) return "Hôm nay";
    if (diffDays < 30) {
        return diffDays + " ngày trước";
    } else if (diffDays < 365) {
        const months = Math.floor(diffDays / 30);
        return months + " tháng trước";
    } else {
        const years = Math.floor(diffDays / 365);
        return years + " năm trước";
    }
}

window.openServerSettings = async function (serverId) {
    // Hide Context Menu if open
    const ctxMenu = document.getElementById('context-menu');
    if (ctxMenu) ctxMenu.classList.remove('visible');

    const saveBar = document.getElementById('server-save-bar');
    if (saveBar) saveBar.classList.remove('visible');

    try {
        const server = await Api.get(`/api/servers/${serverId}`);
        if (!server) return;

        state.editingServerId = serverId;
        state.editingServerData = {
            name: server.name,
            description: server.description || '',
            iconUrl: server.iconUrl
        };
        state.tempServerIcon = null;

        // Populate Header
        const header = document.getElementById('server-settings-header');
        if (header) header.querySelector('span').innerText = server.name.toUpperCase();

        // Check Permissions
        let myPerms = new Set();
        try {
            const permsList = await Api.get(`/api/servers/${serverId}/members/me/permissions`);
            if (permsList && Array.isArray(permsList)) {
                permsList.forEach(p => myPerms.add(p.code));
            }
        } catch (e) {
            console.error("Failed to fetch permissions", e);
        }

        const isOwner = (state.currentUser && state.currentUser.id === server.ownerId);
        // "ADMIN" or "ADMINISTRATOR" - verify backend code
        const isAdmin = myPerms.has('ADMIN') || myPerms.has('ADMINISTRATOR');

        // Logic
        const canViewOverview = isOwner || isAdmin || myPerms.has('MANAGE_SERVER');
        const canViewRoles = isOwner || isAdmin || myPerms.has('MANAGE_ROLES');
        const canViewAudit = isOwner || isAdmin || myPerms.has('VIEW_AUDIT_LOG');
        const canViewBans = isOwner || isAdmin || myPerms.has('BAN_MEMBERS');
        // Usually only owner deletes, or ADMIN with strict checks
        const canDelete = isOwner;

        // Tabs
        const setDisplay = (id, visible) => {
            const el = document.getElementById(id);
            if (el) el.style.display = visible ? 'block' : 'none';
        };

        setDisplay('nav-server-overview', canViewOverview);
        setDisplay('nav-server-roles', canViewRoles);
        setDisplay('nav-server-audit', canViewAudit);
        setDisplay('nav-server-bans', canViewBans);
        setDisplay('nav-server-delete', canDelete);

        // Members always visible for now (or check VIEW_MEMBERS?)
        setDisplay('nav-server-members', true);

        // Separators
        const separators = document.querySelectorAll('#serverSettingsModal .settings-nav-separator');
        const anyAdminTab = canViewOverview || canViewRoles || canViewAudit || canViewBans || canDelete;
        separators.forEach(el => el.style.display = anyAdminTab ? 'block' : 'none');

        // PREVIEW COUNTS
        const onlineCountEl = document.getElementById('preview-online-count');
        const memberCountEl = document.getElementById('preview-member-count');
        let safeOnlineCount = server.onlineCount || 0;
        if (safeOnlineCount === 0) safeOnlineCount = 1;
        if (onlineCountEl) onlineCountEl.innerText = safeOnlineCount;
        if (memberCountEl) memberCountEl.innerText = server.memberCount || 0;

        // FORM FILL
        if (document.getElementById('edit-server-name')) {
            document.getElementById('edit-server-name').value = server.name;
        }
        if (document.getElementById('edit-server-desc')) {
            document.getElementById('edit-server-desc').value = server.description || '';
        }
        if (document.getElementById('delete-server-name-display')) {
            document.getElementById('delete-server-name-display').innerText = server.name;
        }

        // VISUALS
        if (window.updateServerPreviews) {
            updateServerPreviews(server.name, server.iconUrl);
        }

        if (document.getElementById('editServerIconInput')) {
            document.getElementById('editServerIconInput').value = '';
        }

        // Show Modal
        document.getElementById('serverSettingsModal').classList.add('active');

        // DEFAULT TAB
        if (canViewOverview) {
            switchServerSettingsTab('overview');
        } else {
            switchServerSettingsTab('members');
        }

        // Setup Watchers
        if (window.watchServerChanges) {
            watchServerChanges();
        }

    } catch (e) {
        console.error("Lỗi mở cài đặt server:", e);
        alert("Không thể tải thông tin server.");
    }
}

window.switchServerSettingsTab = function (tabName) {
    // 1. Navigation State
    document.querySelectorAll('#serverSettingsModal .settings-nav-item').forEach(el => el.classList.remove('active'));
    const navItem = document.getElementById(`nav-server-${tabName}`);
    if (navItem) navItem.classList.add('active');

    // 2. Save Bar Logic
    const saveBar = document.getElementById('server-save-bar');
    if (saveBar) {
        if (tabName === 'overview') {
            if (window.checkServerChanges) checkServerChanges();
        } else {
            saveBar.classList.remove('visible');
        }
    }

    // 3. Content Visibility
    document.querySelectorAll('#serverSettingsModal .settings-tab-content').forEach(el => el.classList.remove('active'));
    const content = document.getElementById(`tab-server-${tabName}`);
    if (content) content.classList.add('active');

    // 4. Lazy Loading
    if (tabName === 'members' && window.loadServerMembers) {
        loadServerMembers(state.editingServerId);
    } else if (tabName === 'roles' && window.loadServerRoles) {
        loadServerRoles(state.editingServerId);
    } else if (tabName === 'bans' && window.loadServerBans) {
        loadServerBans(state.editingServerId);
    } else if (tabName === 'audit' && window.loadAuditLogs) {
        loadAuditLogs();
    }
}