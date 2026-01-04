
// --- SERVER BANS LOGIC ---
if (!window.state) {
    window.state = {};
}
state.bansCache = [];
state.bansSearchQuery = '';

window.loadServerBans = async function (serverId) {
    const listContainer = document.getElementById('server-bans-list');
    console.log("DEBUG: loadServerBans called for server:", serverId);

    // Validate Server ID
    if (!serverId) {
        console.error("DEBUG: serverId is missing or invalid in loadServerBans");
        if (listContainer) listContainer.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi: Không tìm thấy Server ID.</div>';
        return;
    }

    if (listContainer) listContainer.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Đang tải...</div>';

    try {
        console.log(`DEBUG: Sending GET request to /api/servers/${serverId}/members/bans`);
        const bans = await Api.get(`/api/servers/${serverId}/members/bans`);

        if (!Array.isArray(bans)) {
            console.error("DEBUG: API did NOT return an array! Got:", bans);
            state.bansCache = [];
        } else {
            state.bansCache = bans;
        }

        renderBansList();
    } catch (e) {
        console.error("Lỗi load bans (Exception):", e);
        if (listContainer) listContainer.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi tải danh sách chặn: ' + (e.message || 'Unknown') + '</div>';
    }
}

window.filterServerBans = function () {
    const input = document.getElementById('server-bans-search');
    if (input) {
        state.bansSearchQuery = input.value.toLowerCase();
        renderBansList();
    }
}

window.renderBansList = function () {
    const listContainer = document.getElementById('server-bans-list');
    const countLabel = document.getElementById('bans-count-label');

    // Safety check for cache
    let bans = state.bansCache || [];

    // Apply filter
    const query = (state.bansSearchQuery || '').toLowerCase();
    const filtered = bans.filter(b => {
        // Handle potential null user gracefully
        const name = (b.user && (b.user.displayName || b.user.username)) ? (b.user.displayName || b.user.username) : 'Unknown User';
        return name.toLowerCase().includes(query);
    });

    // Update count label
    if (countLabel) countLabel.innerText = filtered.length;

    // Check container
    if (!listContainer) return;

    // Clear current list
    listContainer.innerHTML = '';

    // Handle empty list
    if (filtered.length === 0) {
        listContainer.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Không có ai bị chặn.</div>';
        return;
    }

    // Render items
    filtered.forEach(ban => {
        try {
            // Check if user is null (safety)
            const user = ban.user || { id: 0, username: 'Deleted User', displayName: 'Deleted User', avatarUrl: null };
            const bannedBy = ban.bannedBy || { username: 'Unknown' };
            const reason = ban.reason || 'Không có lý do';

            const item = document.createElement('div');
            item.className = 'member-item'; // Re-use member item styling
            item.style.cursor = 'pointer'; // Make clickable for unban
            item.style.display = 'flex';
            item.style.alignItems = 'center';
            item.style.padding = '8px 16px';
            item.style.borderBottom = '1px solid #2b2d31';
            item.style.borderRadius = '4px';
            item.style.marginBottom = '2px';
            item.onmouseover = () => item.style.backgroundColor = 'rgba(79, 84, 92, 0.16)';
            item.onmouseout = () => item.style.backgroundColor = 'transparent';

            // Fix: pass serverId and user object correctly
            item.onclick = function (e) {
                if (!e.target.closest('button')) confirmRevokeBan(ban.serverId, user);
            };

            const avatarUrl = user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png';

            item.innerHTML = `
                <div class="member-info" style="flex: 0 0 200px;">
                    <img src="${avatarUrl}" class="member-avatar" onerror="this.src='https://cdn.discordapp.com/embed/avatars/0.png'" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover;">
                    <div class="member-details" style="margin-left: 10px;">
                        <div class="member-name" style="color: #f23f42; font-weight: 600; font-size: 14px;">${user.displayName || user.username}</div>
                        <div class="member-subtext" style="color: #949ba4; font-size: 12px;">${user.username}</div>
                    </div>
                </div>
                
                <div style="flex: 1; display: flex; flex-direction: column; justify-content: center; padding: 0 15px; overflow: hidden;">
                    <span style="font-size: 12px; color: #b5bac1; text-transform: uppercase; font-weight: 700; margin-bottom: 2px;">Lý do</span>
                    <span style="color: #dbdee1; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${reason}">${reason}</span>
                </div>

                <div style="flex: 0 0 150px; display: flex; flex-direction: column; justify-content: center; text-align: right; margin-right: 15px;">
                    <span style="font-size: 11px; color: #949ba4;">Bị chặn bởi</span>
                    <span style="color: #b5bac1; font-size: 13px;">${bannedBy.username}</span>
                </div>

                <div class="member-actions" style="flex: 0 0 40px; display: flex; justify-content: center; align-items: center;">
                     <button class="btn-icon-danger" title="Gỡ Ban" type="button" style="background: transparent; border: none; color: #ed4245; cursor: pointer; padding: 8px; transition: 0.2s;">
                        <i class="fas fa-user-slash" style="font-size: 16px;"></i>
                    </button>
                </div>
            `;
            // Attach event listener explicitly to the button to avoid HTML string escaping issues
            const btn = item.querySelector('button');
            btn.onclick = (e) => {
                e.stopPropagation();
                confirmRevokeBan(ban.serverId, user);
            };

            listContainer.appendChild(item);
        } catch (err) {
            console.error("Error rendering ban item:", err);
        }
    });
}

// Renamed from revokeBan to confirmRevokeBan to match calls
window.confirmRevokeBan = function (serverId, user) {
    const userId = user.id || user; // Handle if object or ID passed
    const username = user.username || 'User';

    Swal.fire({
        title: 'Bỏ chặn?',
        text: `Bạn có chắc muốn bỏ chặn ${username} không?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                // Use passed serverId or fallback to state
                const sId = serverId || state.editingServerId;
                await Api.delete(`/api/servers/${sId}/members/bans/${userId}`);
                Swal.fire(
                    'Đã bỏ chặn!',
                    `${username} đã có thể tham gia lại server.`,
                    'success'
                );
                loadServerBans(sId);
                // Also refresh members list if available, in case unban restores membership or affects list
                if (window.loadServerMembers) {
                    loadServerMembers(sId);
                }
            } catch (e) {
                console.error(e);
                Swal.fire('Lỗi', 'Không thể bỏ chặn: ' + e.message, 'error');
            }
        }
    });
}
