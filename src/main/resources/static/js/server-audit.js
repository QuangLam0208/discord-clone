
// --- AUDIT LOG LOGIC ---
if (!window.state) {
    window.state = {};
}
let currentAuditLogs = [];

window.loadAuditLogs = async function () {
    const listDiv = document.getElementById('audit-log-list-container');
    listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Đang tải nhật ký...</div>';

    try {
        const logs = await Api.get(`/api/servers/${state.editingServerId}/audit-logs`);
        currentAuditLogs = logs || [];
        filterAuditLogs(); // This will filter and render
    } catch (e) {
        console.error("Load Audit Logs Error", e);
        listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi tải nhật ký: ' + e.message + '</div>';
    }
}

window.filterAuditLogs = function () {
    const keyword = (document.getElementById('audit-log-search').value || '').toLowerCase();
    const actionFilter = document.getElementById('audit-log-filter-action').value;

    let filtered = currentAuditLogs.filter(log => {
        // Safe access to properties
        const actionType = log.actionType || 'UNKNOWN';
        const changes = log.changes || '';
        const actorName = log.actor ? (log.actor.displayName || log.actor.username || '') : '';

        // Filter by Action Type
        if (actionFilter !== 'ALL') {
            if (actionFilter === 'SERVER_UPDATE' && actionType !== 'SERVER_UPDATE') return false;
            if (actionFilter === 'CHANNEL' && !actionType.startsWith('CHANNEL_')) return false;
            if (actionFilter === 'ROLE' && !actionType.startsWith('ROLE_')) return false;
            if (actionFilter === 'MEMBER' && !actionType.startsWith('MEMBER_')) return false;
        }

        // Filter by Keyword (Actor, Action Type, Changes)
        if (keyword) {
            return actorName.toLowerCase().includes(keyword) ||
                actionType.toLowerCase().includes(keyword) ||
                changes.toLowerCase().includes(keyword);
        }

        return true;
    });

    renderAuditLogs(filtered);
}

window.renderAuditLogs = function (logs) {
    const listDiv = document.getElementById('audit-log-list-container');
    listDiv.innerHTML = '';

    if (!logs || logs.length === 0) {
        listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Không có nhật ký nào.</div>';
        return;
    }

    logs.forEach(log => {
        const div = document.createElement('div');
        div.className = 'audit-log-item';
        div.style.display = 'flex';
        div.style.alignItems = 'flex-start';
        div.style.padding = '10px';
        div.style.borderBottom = '1px solid #2b2d31';
        div.style.marginBottom = '4px';

        const actionType = log.actionType || 'UNKNOWN';
        const changes = log.changes || '';
        const actor = log.actor || {};
        const actorName = actor.displayName || actor.username || 'Unknown';
        const actorAvatar = actor.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png';

        // Icon based on action
        let iconClass = 'fa-pen';
        let color = '#b5bac1';

        if (actionType.includes('CREATE')) { iconClass = 'fa-plus'; color = '#3ba55c'; }
        else if (actionType.includes('DELETE')) { iconClass = 'fa-trash-can'; color = '#ed4245'; }
        else if (actionType.includes('UPDATE')) { iconClass = 'fa-pencil'; color = '#5865f2'; }
        else if (actionType.includes('KICK')) { iconClass = 'fa-user-minus'; color = '#ed4245'; }

        // Use a simple formatting function or moment.js if available
        // Here we rely on the helper calculateTimeAgo which is assumed to be global or we need to copy it.
        // I'll copy a safe version here to be independent.
        const timeAgo = calculateTimeAgoSafe(log.createdAt);

        div.innerHTML = `
            <div style="margin-right: 12px; position: relative;">
                <img src="${actorAvatar}" style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;">
                <div style="position: absolute; bottom: -2px; right: -2px; background: #313338; border-radius: 50%; padding: 2px;">
                    <i class="fas ${iconClass}" style="color: ${color}; font-size: 12px;"></i>
                </div>
            </div>
            <div style="flex: 1;">
                <div style="display: flex; justify-content: space-between; align-items: baseline;">
                    <div>
                        <span style="font-weight: 600; color: #f2f3f5; margin-right: 6px;">${actorName}</span>
                        <span style="color: #b5bac1; font-size: 12px; background: #2b2d31; padding: 2px 6px; border-radius: 4px;">${formatActionName(actionType)}</span>
                    </div>
                    <span style="font-size: 12px; color: #949ba4;">${timeAgo}</span>
                </div>
                <div style="color: #dbdee1; font-size: 14px; margin-top: 4px; word-break: break-word;">
                    ${changes}
                </div>
            </div>
        `;
        listDiv.appendChild(div);
    });
}

function formatActionName(action) {
    if (!action) return 'UNKNOWN';
    switch (action) {
        case 'SERVER_UPDATE': return 'CẬP NHẬT MÁY CHỦ';
        case 'CHANNEL_CREATE': return 'TẠO KÊNH';
        case 'CHANNEL_UPDATE': return 'CẬP NHẬT KÊNH';
        case 'CHANNEL_DELETE': return 'XÓA KÊNH';
        case 'ROLE_CREATE': return 'TẠO VAI TRÒ';
        case 'ROLE_UPDATE': return 'CẬP NHẬT VAI TRÒ';
        case 'ROLE_DELETE': return 'XÓA VAI TRÒ';
        case 'MEMBER_KICK': return 'ĐUỔI THÀNH VIÊN';
        case 'MEMBER_BAN': return 'BAN THÀNH VIÊN';
        case 'MEMBER_UNBAN': return 'BỎ BAN';
        case 'MEMBER_ROLE_UPDATE': return 'CẬP NHẬT VAI TRÒ TV';
        default: return action.replace('_', ' ');
    }
}

function calculateTimeAgoSafe(dateString) {
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