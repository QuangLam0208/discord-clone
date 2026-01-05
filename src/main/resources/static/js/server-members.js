// --- MEMBER MANAGEMENT LOGIC ---
if (!window.state) { window.state = {}; }

// Default states
state.membersCache = [];
state.currentMemberPage = 1;
state.membersPerPage = 8;
state.memberSearchQuery = '';

window.loadServerMembers = async function (serverId) {
    try {
        const members = await Api.get(`/api/servers/${serverId}/members`);
        state.membersCache = members || [];
        state.currentMemberPage = 1;
        renderMembersTable();
    } catch (e) {
        console.error("Lỗi load members:", e);
        state.membersCache = [];
        renderMembersTable();
    }
}

// Search Handler
const memberSearchInput = document.getElementById('member-search-input');
if (memberSearchInput) {
    memberSearchInput.addEventListener('input', (e) => {
        state.memberSearchQuery = e.target.value.toLowerCase();
        state.currentMemberPage = 1;
        renderMembersTable();
    });
}

window.getFilteredMembers = function () {
    if (!state.membersCache) return [];
    return state.membersCache.filter(m => {
        const name = m.user ? (m.user.displayName || m.user.username) : '';
        return name.toLowerCase().includes(state.memberSearchQuery);
    });
}

window.renderMembersTable = function () {
    const tbody = document.getElementById('members-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';

    const allFiltered = getFilteredMembers();
    const totalItems = allFiltered.length;
    const totalPages = Math.ceil(totalItems / state.membersPerPage) || 1;

    if (state.currentMemberPage > totalPages) state.currentMemberPage = totalPages;
    if (state.currentMemberPage < 1) state.currentMemberPage = 1;

    const start = (state.currentMemberPage - 1) * state.membersPerPage;
    const end = start + state.membersPerPage;
    const pageItems = allFiltered.slice(start, end);

    pageItems.forEach(m => {
        const user = m.user;
        const joinedAt = new Date(m.joinedAt).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' });

        let joinedDiscord = "Không xác định";
        if (user.createdAt) {
            // Using logic similar to calculateTimeAgo but locally
            const d = new Date(user.createdAt);
            joinedDiscord = d.toLocaleDateString('vi-VN');
        }

        const rolesHtml = ((m.roles && m.roles.length > 0)
            ? m.roles.map(r => `<span class="role-tag"><div class="role-dot" style="background-color: ${r.color || '#99aab5'}"></div>${r.name || 'New Role'}</span>`).join('')
            : '') + `<button class="btn-add-role" onclick="openRoleSelector(event, ${m.user.id}, '${m.roles ? JSON.stringify(m.roles).replace(/"/g, '&quot;') : '[]'}')"><i class="fas fa-plus"></i></button>`;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                <div class="member-info">
                    <img src="${user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png'}" class="member-avatar">
                    <div class="member-name-col">
                        <span class="member-username">
                            ${user.displayName || user.username}
                            ${(state.editingServerData && state.editingServerData.ownerId == user.id) ? '<i class="fa-solid fa-crown" title="Server Owner" style="color: #f1c40f; margin-left: 6px; font-size: 12px;"></i>' : ''}
                        </span>
                        <span class="member-tag">${user.username}</span>
                    </div>
                </div>
            </td>
            <td>${joinedAt}</td>
            <td>${joinedDiscord}</td>
            <td>
                <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 4px;">
                    ${rolesHtml}
                </div>
            </td>
            <td style="text-align: right;">
                <div style="position: relative; display: inline-block;">
                     <button class="kick-btn" title="Tùy chọn" onclick="showMemberContextMenu(event, ${m.user.id})"><i class="fas fa-ellipsis-v"></i></button>
                     <div id="member-ctx-menu-${m.user.id}" class="ban-context-menu" style="display: none; position: absolute; right: 0; top: 100%; background: #111214; border-radius: 4px; padding: 6px; z-index: 100; width: 170px; box-shadow: 0 8px 16px rgba(0,0,0,0.24); text-align: left;">
                        ${(function () {
                            try {
                                // Ưu tiên lấy từ state.currentUser (đã load từ API /users/me), fallback sang localStorage
                                const currentUser = window.state.currentUser || JSON.parse(localStorage.getItem('user') || '{}');
                
                                // Debug log
                                const ownerId = state.editingServerData ? state.editingServerData.ownerId : undefined;
                                const myId = currentUser.id;
                                // console.log(`[Debug] Checking Owner: OwnerID=${ownerId}, MyID=${myId}, TargetID=${m.user.id}`);
                
                                const isOwner = (ownerId != null && myId != null && ownerId == myId);
                
                                // Hiển thị nút Transfer Owner nếu: Mình là Owner VÀ Target không phải là mình
                                if (isOwner && m.user.id != myId) {
                                    return `<div class="ctx-item-red" onclick="confirmTransferOwner(${m.id}, '${m.user.username}')" style="padding: 8px; cursor: pointer; color: #dbdee1; font-size: 14px; border-radius: 2px;" onmouseover="this.style.backgroundColor='#4752c4';this.style.color='white'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#dbdee1'">
                                                                    <i class="fas fa-crown" style="margin-right: 8px;"></i> Chuyển quyền sở hữu
                                                                </div>`;
                                }
                                return '';
                            } catch (e) { console.error(e); return ''; }
                        })()}
                        <div class="ctx-item-red" onclick="confirmKickMember(${m.user.id})" style="padding: 8px; cursor: pointer; color: #f23f42; font-size: 14px; border-radius: 2px;" onmouseover="this.style.backgroundColor='#da373c';this.style.color='white'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#f23f42'">
                            <i class="fas fa-user-minus" style="margin-right: 8px;"></i> Đuổi thành viên
                        </div>
                        <div class="ctx-item-red" onclick="confirmBanMember(${m.user.id})" style="padding: 8px; cursor: pointer; color: #f23f42; font-size: 14px; border-radius: 2px;" onmouseover="this.style.backgroundColor='#da373c';this.style.color='white'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#f23f42'">
                            <i class="fas fa-gavel" style="margin-right: 8px;"></i> Chặn thành viên
                        </div>
                     </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });

    const display = document.getElementById('members-count-display');
    if (display) display.innerText = `Đang hiển thị ${totalItems} thành viên`;
}

window.showMemberContextMenu = function (e, userId) {
    e.stopPropagation();
    document.querySelectorAll('.ban-context-menu').forEach(el => el.style.display = 'none');
    const menu = document.getElementById(`member-ctx-menu-${userId}`);
    if (menu) menu.style.display = 'block';
}

window.confirmKickMember = async function (userId) {
    document.querySelectorAll('.ban-context-menu').forEach(el => el.style.display = 'none');

    const result = await Swal.fire({
        title: 'Trục xuất thành viên?',
        text: "Bạn có chắc chắn muốn trục xuất thành viên này?",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Trục xuất',
        cancelButtonText: 'Hủy'
    });

    if (result.isConfirmed) {
        try {
            await Api.delete(`/api/servers/${state.editingServerId}/members/${userId}`);
            Swal.fire('Đã trục xuất!', 'Thành viên đã bị trục xuất.', 'success');
            loadServerMembers(state.editingServerId);
        } catch (e) {
            console.error(e);
            Swal.fire('Lỗi!', 'Không thể trục xuất: ' + (e.message || 'Lỗi'), 'error');
        }
    }
}

// Alias for kickMember to use same logic
window.kickMember = window.confirmKickMember;

window.confirmTransferOwner = async function (memberId, username) {
    document.querySelectorAll('.ban-context-menu').forEach(el => el.style.display = 'none');

    const check = await Swal.fire({
        title: 'Chuyển quyền sở hữu?',
        html: `Bạn đang chuẩn bị chuyển quyền chủ sở hữu cho <b>${username}</b>.<br><span style="color:red; font-weight:bold;">CẢNH BÁO:</span> Bạn sẽ mất quyền chủ sở hữu server này.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xác nhận chuyển',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1'
    });

    if (check.isConfirmed) {
        try {
            await Api.post(`/api/servers/${state.editingServerId}/transfer-owner`, {
                newOwnerMemberId: memberId
            });
            Swal.fire({
                title: 'Chuyển quyền thành công!',
                text: `Bạn không còn là chủ sở hữu nữa.`,
                icon: 'success',
                timer: 2000,
                showConfirmButton: false,
                background: '#313338',
                color: '#dbdee1'
            }).then(() => {
                window.location.href = '/home';
            });
        } catch (e) {
            console.error(e);
            Swal.fire('Lỗi', 'Chuyển quyền thất bại: ' + (e.message || 'Lỗi'), 'error');
        }
    }
}

window.confirmBanMember = async function (userId) {
    document.querySelectorAll('.ban-context-menu').forEach(el => el.style.display = 'none');

    const { value: reason } = await Swal.fire({
        title: 'Chặn thành viên',
        input: 'text',
        inputPlaceholder: 'Nhập lý do chặn (tùy chọn)',
        showCancelButton: true,
        confirmButtonText: 'Chặn ngay',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#d33',
        background: '#313338',
        color: '#dbdee1'
    });

    if (reason !== undefined) {
        try {
            await Api.post(`/api/servers/${state.editingServerId}/members/bans`, {
                userId: userId,
                reason: reason
            });
            Swal.fire('Đã chặn!', 'Thành viên đã bị chặn khỏi máy chủ.', 'success');
            loadServerMembers(state.editingServerId);
        } catch (e) {
            console.error(e);
            Swal.fire('Lỗi!', 'Không thể chặn: ' + (e.message || 'Lỗi'), 'error');
        }
    }
}

// Global listener to close context menus
document.addEventListener('click', (e) => {
    if (!e.target.closest('.ban-context-menu') && !e.target.closest('.kick-btn')) {
        document.querySelectorAll('.ban-context-menu').forEach(el => el.style.display = 'none');
    }
});