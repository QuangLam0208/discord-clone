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
            iconUrl: server.iconUrl,
            ownerId: server.ownerId
        };
        state.tempServerIcon = null;

        // Populate Header
        const header = document.getElementById('server-settings-header');
        if (header) header.querySelector('span').innerText = server.name.toUpperCase();

        // Check Permissions & Update Sidebar
        const perms = await refreshServerSettingsPermissions(serverId);

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
        if (perms.canViewOverview) {
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

window.refreshServerSettingsPermissions = async function (serverId) {
    if (!state.editingServerData) return { canViewOverview: false }; // Safety

    // Default Owner Check from state to avoid re-fetch server if possible, 
    // but permissions must be re-fetched.
    let myPerms = new Set();
    try {
        const permsList = await Api.get(`/api/servers/${serverId}/members/me/permissions`);
        if (permsList && Array.isArray(permsList)) {
            permsList.forEach(p => myPerms.add(p.code));
        }
    } catch (e) {
        console.error("Failed to fetch permissions", e);
    }

    const isOwner = (state.currentUser && state.currentUser.id === state.editingServerData.ownerId);
    const isAdmin = myPerms.has('ADMIN') || myPerms.has('ADMINISTRATOR');

    const canViewOverview = isOwner || isAdmin || myPerms.has('MANAGE_SERVER');
    // Updated requirement: "Only owner can edit roles" -> Effectively Owner or Admin since Admin is superuser.
    const canViewRoles = isOwner || isAdmin;

    // Audit Log Permission
    const canViewAudit = isOwner || isAdmin || myPerms.has('VIEW_AUDIT_LOG');

    const canViewBans = isOwner || isAdmin || myPerms.has('BAN_MEMBERS');
    const canDelete = isOwner;

    const setDisplay = (id, visible) => {
        const el = document.getElementById(id);
        if (el) {
            el.style.display = visible ? 'block' : 'none';
            // If current tab is hidden, switch to something else? 
            // e.g. if I am viewing Audit Log and it gets hidden.
            if (!visible && el.classList.contains('active')) {
                // Find first visible tab?
                if (canViewOverview) switchServerSettingsTab('overview');
                else switchServerSettingsTab('members');
            }
        }
    };

    setDisplay('nav-server-overview', canViewOverview);
    setDisplay('nav-server-roles', canViewRoles);
    setDisplay('nav-server-audit', canViewAudit);
    setDisplay('nav-server-bans', canViewBans);
    setDisplay('nav-server-delete', canDelete);
    setDisplay('nav-server-members', true); // Members always visible

    // Separators update
    const anyAdminTab = canViewOverview || canViewRoles || canViewAudit || canViewBans || canDelete;
    document.querySelectorAll('#serverSettingsModal .settings-nav-separator').forEach(el => el.style.display = anyAdminTab ? 'block' : 'none');

    return {
        isOwner, isAdmin,
        canViewOverview, canViewRoles, canViewAudit, canViewBans, canDelete
    };
}