
// SERVER MODULE

// 1. LOAD SERVERS
async function loadServers() {
    if (!elements || !elements.sidebar) return;
    try {
        const servers = await Api.get('/api/servers/me');
        if (servers && Array.isArray(servers)) {
            renderServerSidebar(servers);
        }
    } catch (e) {
        console.error("Lỗi load servers:", e);
    }
}

function renderServerSidebar(servers) {
    const children = Array.from(elements.sidebar.children);
    children.forEach(child => {
        if (child.id === 'btn-dm' || child.classList.contains('separator') || child.id === 'createServerBtn') {
            // Keep static items
        } else {
            child.remove();
        }
    });
    const createBtn = document.getElementById('createServerBtn');
    servers.forEach(server => {
        const serverEl = createServerIcon(server);
        elements.sidebar.insertBefore(serverEl, createBtn);
    });
}

function createServerIcon(server) {
    const div = document.createElement('div');
    div.className = 'sidebar-item';
    div.dataset.serverId = server.id;
    div.dataset.ownerId = server.ownerId;
    div.title = server.name;
    let contentHtml = '';
    if (server.iconUrl) {
        contentHtml = `<img src="${server.iconUrl}">`;
    } else {
        const acronym = server.name.substring(0, 2).toUpperCase();
        contentHtml = acronym;
    }
    div.innerHTML = `<div class="pill"></div><div class="sidebar-icon">${contentHtml}</div><span class="tooltip">${server.name}</span>`;
    div.onclick = function () {
        document.querySelectorAll('.sidebar-item').forEach(e => e.classList.remove('active'));
        div.classList.add('active');
        if (window.switchToServer) window.switchToServer(div);
        onClickServer(server.id);
    };
    return div;
}

window.onClickServer = function (serverId) {
    if (state.currentServerId === serverId) return;
    state.currentServerId = serverId;
    const headerName = document.querySelector('.channel-header h4');
    if (headerName) {
        const serverItem = document.querySelector(`.sidebar-item[data-server-id="${serverId}"]`);
        if (serverItem) headerName.innerText = serverItem.getAttribute('title');
    }
    loadCategoriesAndChannels(serverId);
}

// 2. CREATE SERVER
document.addEventListener('DOMContentLoaded', () => {
    const createServerBtn = document.getElementById('createServerBtn');
    if (createServerBtn) {
        // Re-bind to handle Swal correctly
        createServerBtn.onclick = async () => {
            const result = await Swal.fire({
                title: 'Thêm máy chủ',
                text: 'Bạn muốn tạo máy chủ mới hay tham gia máy chủ có sẵn?',
                showCancelButton: true,
                showDenyButton: true,
                confirmButtonText: 'Tạo mới',
                denyButtonText: 'Tham gia',
                cancelButtonText: 'Hủy',
                confirmButtonColor: '#23a559',
                denyButtonColor: '#5865f2',
                background: '#313338',
                color: '#dbdee1'
            });

            if (result.isConfirmed) {
                showCreateServerDialog();
            } else if (result.isDenied) {
                showJoinServerDialog();
            }
        };
    }
});

// Helper: Show Join Dialog
async function showJoinServerDialog() {
    const { value: code } = await Swal.fire({
        title: 'Tham gia máy chủ',
        input: 'text',
        inputLabel: 'Nhập mã mời hoặc liên kết',
        inputPlaceholder: 'https://discord.gg/... hoặc mã code',
        showCancelButton: true,
        confirmButtonText: 'Tham gia',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1',
        confirmButtonColor: '#5865f2',
        inputValidator: (value) => {
            if (!value) {
                return 'Bạn cần nhập mã mời!';
            }
        }
    });

    if (code) {
        // Extract code if it's a URL
        let inviteCode = code;
        if (code.includes('/invite/')) {
            const parts = code.split('/invite/');
            if (parts.length > 1) inviteCode = parts[1];
        } else if (code.includes('/')) {
            // Basic naive check if user pasted a full url not matching standard pattern
            // Let's just try sending what they gave if it looks like a code, otherwise assume last part
            const parts = code.split('/');
            inviteCode = parts[parts.length - 1];
        }

        // Remove query parameters if any
        if (inviteCode && inviteCode.includes('?')) {
            inviteCode = inviteCode.split('?')[0];
        }

        inviteCode = inviteCode ? inviteCode.trim() : '';

        if (!inviteCode || inviteCode === 'undefined') {
            Swal.fire('Lỗi', 'Mã mời không hợp lệ (Trống hoặc lỗi)', 'error');
            return;
        }

        console.log("Joining with code:", inviteCode);

        try {
            const response = await Api.post(`/api/invites/join/${inviteCode}`);
            Swal.fire({
                icon: 'success',
                title: 'Thành công',
                text: 'Bạn đã tham gia máy chủ!',
                background: '#313338',
                color: '#dbdee1',
                timer: 1500,
                showConfirmButton: false
            });
            await loadServers(); // Reload custom server list

            // Auto switch to new server
            if (response && response.serverId) {
                console.log("Switching to new server:", response.serverId);
                setTimeout(() => onClickServer(response.serverId), 500);
            }
        } catch (e) {
            console.error(e);
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                text: e.message || 'Mã mời không hợp lệ hoặc đã hết hạn.',
                background: '#313338',
                color: '#dbdee1'
            });
        }
    }
}

// Helper: Show Create Dialog (The original logic refactored)
async function showCreateServerDialog() {
    await Swal.fire({
        title: 'Tạo máy chủ mới',
        html: `
                    <div style="display: flex; flex-direction: column; align-items: center; gap: 15px; margin-bottom: 10px;">
                        <div id="swal-preview" style="width: 80px; height: 80px; border-radius: 50%; background-color: #333; display: flex; justify-content: center; align-items: center; overflow: hidden; border: 2px dashed #99aab5; cursor: pointer; position: relative;">
                            <span style="font-size: 10px; color: #b9bbbe; font-weight: bold; text-align: center;">UPLOAD<br>ICON</span>
                            <img id="swal-preview-img" style="width: 100%; height: 100%; object-fit: cover; display: none; position: absolute; top:0; left:0;">
                        </div>
                        <input type="file" id="swal-file" style="display: none" accept="image/*">
                        <input id="swal-input1" class="swal2-input" placeholder="Tên máy chủ" style="margin: 0 !important; width: 80%; background: #1e1f22; color: #dbdee1; border: none;">
                    </div>
                `,
        background: '#313338',
        color: '#dbdee1',
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: 'Tạo',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#23a559',
        didOpen: () => {
            const preview = document.getElementById('swal-preview');
            const fileInput = document.getElementById('swal-file');
            const previewImg = document.getElementById('swal-preview-img');

            preview.onclick = () => fileInput.click();

            fileInput.onchange = () => {
                const file = fileInput.files[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        previewImg.src = e.target.result;
                        previewImg.style.display = 'block';
                    }
                    reader.readAsDataURL(file);
                }
            };
        },
        preConfirm: async () => {
            const name = document.getElementById('swal-input1').value;
            const file = document.getElementById('swal-file').files[0];

            if (!name) {
                Swal.showValidationMessage('Vui lòng nhập tên máy chủ');
                return false;
            }

            let iconUrl = null;
            if (file) {
                if (file.size > 10 * 1024 * 1024) {
                    Swal.showValidationMessage('File quá lớn (Max 10MB)');
                    return false;
                }

                try {
                    const formData = new FormData();
                    formData.append('file', file);
                    const res = await Api.upload('/api/upload', formData);
                    iconUrl = res.url;
                } catch (e) {
                    Swal.showValidationMessage('Lỗi upload ảnh: ' + e.message);
                    return false;
                }
            }

            return { name, iconUrl };
        }
    }).then(async (result) => {
        if (result.isConfirmed) {
            const { name, iconUrl } = result.value;
            try {
                const newServer = await Api.post('/api/servers', {
                    name: name,
                    iconUrl: iconUrl
                });

                if (newServer) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công',
                        text: 'Máy chủ đã được tạo!',
                        background: '#313338',
                        color: '#dbdee1'
                    });
                    await loadServers();
                    onClickServer(newServer.id);
                }
            } catch (e) {
                console.error("Create server error:", e);
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: 'Không thể tạo máy chủ: ' + e.message,
                    background: '#313338',
                    color: '#dbdee1'
                });
            }
        }
    });
}


// 3. SERVER SETTINGS LOGIC
// Open Modal
window.openServerSettings = async function (serverId) {
    // Hide Context Menu
    const ctxMenu = document.getElementById('context-menu');
    if (ctxMenu) ctxMenu.classList.remove('visible');

    // Reset save bar immediately to prevent transition flash
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

        // Populate Counts in Preview
        const onlineCountEl = document.getElementById('preview-online-count');
        const memberCountEl = document.getElementById('preview-member-count');

        // Logic: Online count must be at least 1 (the current user)
        let safeOnlineCount = server.onlineCount || 0;
        if (safeOnlineCount === 0) safeOnlineCount = 1;

        if (onlineCountEl) onlineCountEl.innerText = safeOnlineCount;
        if (memberCountEl) memberCountEl.innerText = server.memberCount || 0;
        if (memberCountEl) memberCountEl.innerText = server.memberCount || 0;

        // Populate Form
        document.getElementById('edit-server-name').value = server.name;
        document.getElementById('edit-server-desc').value = server.description || '';
        document.getElementById('delete-server-name-display').innerText = server.name;

        // Initial Preview Render
        updateServerPreviews(server.name, server.iconUrl);

        // Reset Inputs & Bar
        document.getElementById('editServerIconInput').value = '';
        state.tempServerIcon = null; // Ensure this is definitely null

        // Reset Tab
        switchServerSettingsTab('overview');

        // Open Modal
        document.getElementById('serverSettingsModal').classList.add('active');

        // Watchers
        watchServerChanges();

    } catch (e) {
        console.error("Lỗi mở cài đặt server:", e);
        alert("Không thể tải thông tin server.");
    }
}

// Helper to update both small and card previews
function updateServerPreviews(name, iconUrl) {
    const acronym = name ? name.substring(0, 2).toUpperCase() : 'SV';

    // Elements
    const smallImg = document.getElementById('edit-server-icon-preview');
    const smallPh = document.getElementById('edit-server-icon-placeholder');
    const cardImg = document.getElementById('preview-card-icon-img');
    const cardPh = document.getElementById('preview-card-icon-text');
    const cardName = document.getElementById('preview-card-name');

    // Update Name
    if (cardName) cardName.innerText = name || 'Máy Chủ';

    // Update Icons
    if (iconUrl) {
        smallImg.src = iconUrl;
        smallImg.style.display = 'block';
        if (smallPh) smallPh.style.display = 'none';

        cardImg.src = iconUrl;
        cardImg.style.display = 'block';
        if (cardPh) {
            cardPh.style.display = 'none';
        }
    } else {
        smallImg.style.display = 'none';
        if (smallPh) {
            smallPh.innerText = acronym;
            smallPh.style.display = 'block';
        }

        cardImg.style.display = 'none';
        if (cardPh) {
            cardPh.innerText = acronym;
            cardPh.style.display = 'block';
        }
    }

    // Link remove button visibility
    const removeBtn = document.querySelector('.remove-icon-btn');
    if (removeBtn) removeBtn.style.display = (iconUrl ? 'block' : 'none');
}

function watchServerChanges() {
    const inputs = ['edit-server-name', 'edit-server-desc'];
    inputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.oninput = () => {
                checkServerChanges();
                // Live update name on card
                const name = document.getElementById('edit-server-name').value;
                document.getElementById('preview-card-name').innerText = name || 'Server Name';

                // Update placeholder text if no icon
                if (!state.tempServerIcon && !state.editingServerData.iconUrl) {
                    const acronym = name.substring(0, 2).toUpperCase();
                    document.getElementById('edit-server-icon-placeholder').innerText = acronym;
                    document.getElementById('preview-card-icon-text').innerText = acronym;
                }
            };
        }
    });

    const fileInput = document.getElementById('editServerIconInput');
    if (fileInput) {
        fileInput.onchange = function (e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (ev) => {
                    // Update visuals immediately
                    updateServerPreviews(document.getElementById('edit-server-name').value, ev.target.result);

                    state.tempServerIcon = file;
                    checkServerChanges();
                };
                reader.readAsDataURL(file);
            }
        };
    }
}

window.removeServerIcon = function () {
    document.getElementById('editServerIconInput').value = '';
    state.tempServerIcon = 'REMOVED';

    // Update visuals to show placeholders
    updateServerPreviews(document.getElementById('edit-server-name').value, null);

    checkServerChanges();
}

function checkServerChanges() {
    const nameInput = document.getElementById('edit-server-name');
    const descInput = document.getElementById('edit-server-desc');

    if (!nameInput || !descInput) return;

    const currentName = (nameInput.value || '').trim();
    const currentDesc = (descInput.value || '').trim();

    const original = state.editingServerData || {};
    const originalName = (original.name || '').trim();
    const originalDesc = (original.description || '').trim();

    let hasChanges = false;
    if (currentName !== originalName) hasChanges = true;
    if (currentDesc !== originalDesc) hasChanges = true;
    if (state.tempServerIcon) hasChanges = true;

    const bar = document.getElementById('server-save-bar');
    if (bar) {
        if (hasChanges) bar.classList.add('visible');
        else bar.classList.remove('visible');
    }
}

window.resetServerOverview = function () {
    const original = state.editingServerData;
    document.getElementById('edit-server-name').value = original.name;
    document.getElementById('edit-server-desc').value = original.description;

    state.tempServerIcon = null;
    document.getElementById('editServerIconInput').value = '';

    // Reset visuals
    updateServerPreviews(original.name, original.iconUrl);

    checkServerChanges();

    // Success Toast
    Swal.fire({
        icon: 'success',
        title: 'Đã đặt lại!',
        toast: true,
        position: 'bottom-end',
        showConfirmButton: false,
        timer: 1500,
        background: '#1e1f22',
        color: '#fff'
    });
}

// Switch Tab
window.switchServerSettingsTab = function (tabName) {
    // Sidebar items
    document.querySelectorAll('#serverSettingsModal .settings-nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`nav-server-${tabName}`).classList.add('active');

    // Handle Save Bar Logic BEFORE showing content to prevent glitches
    const saveBar = document.getElementById('server-save-bar');
    if (saveBar) {
        if (tabName === 'overview') {
            // When entering overview, verify if bar should be shown
            checkServerChanges();
        } else {
            // When leaving overview, ensure it's hidden for next time
            saveBar.classList.remove('visible');
        }
    }

    // Content areas
    document.querySelectorAll('#serverSettingsModal .settings-tab-content').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-server-${tabName}`).classList.add('active');

    // Load data if switching to members
    if (tabName === 'members') {
        loadServerMembers(state.editingServerId);
    }

    // Load data if switching to roles
    if (tabName === 'roles') {
        loadServerRoles(state.editingServerId);
    }
    // Load data if switching to roles
    if (tabName === 'roles') {
        loadServerRoles(state.editingServerId);
    }
}

// MEMBER MANAGEMENT LOGIC
state.membersCache = []; // Store fetched members
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
        // Fallback or empty state
        state.membersCache = [];
        renderMembersTable();
    }
}

// Search Handler
document.getElementById('member-search-input').addEventListener('input', (e) => {
    state.memberSearchQuery = e.target.value.toLowerCase();
    state.currentMemberPage = 1; // Reset to page 1 on search
    renderMembersTable();
});

// Pagination Buttons Removed


function getFilteredMembers() {
    if (!state.membersCache) return [];
    return state.membersCache.filter(m => {
        const name = m.user ? (m.user.displayName || m.user.username) : '';
        return name.toLowerCase().includes(state.memberSearchQuery);
    });
}

function renderMembersTable() {
    const tbody = document.getElementById('members-table-body');
    tbody.innerHTML = '';

    const allFiltered = getFilteredMembers();
    const totalItems = allFiltered.length;
    const totalPages = Math.ceil(totalItems / state.membersPerPage) || 1;

    // Adjust Page if out of bounds
    if (state.currentMemberPage > totalPages) state.currentMemberPage = totalPages;
    if (state.currentMemberPage < 1) state.currentMemberPage = 1;

    // Slice
    const start = (state.currentMemberPage - 1) * state.membersPerPage;
    const end = start + state.membersPerPage;
    const pageItems = allFiltered.slice(start, end);

    // Render Rows
    pageItems.forEach(m => {
        const user = m.user;

        // Date formatting relative
        const joinedAt = new Date(m.joinedAt).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' });

        let joinedDiscord = "Không xác định";
        if (user.createdAt) {
            joinedDiscord = calculateTimeAgo(user.createdAt);
        }

        const joinMethod = "Mời"; // Mock data for now as we don't track invite code used in Member entity yet

        const rolesHtml = ((m.roles && m.roles.length > 0)
            ? m.roles.map(r => `<span class="role-tag"><div class="role-dot" style="background-color: ${r.color || '#99aab5'}"></div>${r.name || 'New Role'}</span>`).join('')
            : '') + `<button class="btn-add-role" onclick="openRoleSelector(event, ${m.user.id}, '${m.roles ? JSON.stringify(m.roles).replace(/"/g, '&quot;') : '[]'}')"><i class="fas fa-plus"></i></button>`;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                <div class="member-info">
                    <img src="${user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png'}" class="member-avatar">
                    <div class="member-name-col">
                        <span class="member-username">${user.displayName || user.username}</span>
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
                 <button class="kick-btn" title="Đuổi thành viên" onclick="kickMember(${m.user.id})"><i class="fas fa-user-shield"></i></button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    // Update Pagination Controls
    document.getElementById('members-count-display').innerText =
        `Đang hiển thị ${totalItems} thành viên`;
}

window.kickMember = async function (userId) {
    const result = await Swal.fire({
        title: 'Trục xuất thành viên?',
        text: "Bạn có chắc chắn muốn trục xuất thành viên này khỏi máy chủ?",
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
            Swal.fire(
                'Đã trục xuất!',
                'Thành viên đã bị trục xuất khỏi máy chủ.',
                'success'
            );
            loadServerMembers(state.editingServerId);
        } catch (e) {
            console.error(e);
            Swal.fire(
                'Lỗi!',
                'Không thể trục xuất thành viên: ' + (e.message || 'Lỗi không xác định'),
                'error'
            );
        }
    }
}

// --- SERVER ROLES TAB LOGIC ---
window.loadServerRoles = async function (serverId) {
    const listBody = document.getElementById('roles-list-body');
    const countLabel = document.getElementById('roles-count-label');
    if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Đang tải...</div>';

    try {
        const roles = await Api.get(`/api/servers/${serverId}/roles`);
        // Sort by priority desc (assuming higher priority = top)
        if (roles) {
            // Sort logic: higher priority means higher in list
            // Assuming priority is number. If not provided, fallback by id or name
            roles.sort((a, b) => b.priority - a.priority);

            window.currentServerRoles = roles; // Store for drag reorder

            renderRolesList(roles);
            if (countLabel) countLabel.innerText = roles.length || 0;
            setupRoleEditorEvents();
        } else {
            if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Không có vai trò nào.</div>';
        }
    } catch (e) {
        console.error("Load Roles Error", e);
        if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi tải vai trò.</div>';
    }
}

function renderRolesList(roles) {
    const listBody = document.getElementById('roles-list-body');
    if (!listBody) return;
    listBody.innerHTML = '';

    roles.forEach(role => {
        const div = document.createElement('div');
        div.className = 'settings-role-item';
        div.draggable = true;
        div.ondragstart = (e) => handleRoleDragStart(e, role.id);
        div.ondragover = (e) => handleRoleDragOver(e);
        div.ondrop = (e) => handleRoleDrop(e, role.id);

        // Mock member count for now as backend DTO update might lag or need more queries
        const memberCount = role.memberCount !== undefined ? role.memberCount : 0;

        div.innerHTML = `
            <div class="role-item-main">
                <div class="role-drag-handle"><i class="fas fa-grip-vertical"></i></div>
                <div class="role-shield" style="color: ${role.color || '#b5bac1'}"><i class="fas fa-shield-alt"></i></div>
                <div class="role-name">${role.name}</div>
            </div>
            <div class="role-members-count">
                ${memberCount} <i class="fas fa-user" style="margin-left: 4px; font-size: 12px;"></i>
            </div>
            <div class="role-actions">
                 <button class="btn-role-action" title="Chỉnh sửa" onclick='openRoleEditor(${JSON.stringify(role)})'><i class="fas fa-pencil-alt"></i></button>
                 <button class="btn-role-action" title="Khác" onclick="showRoleActionMenu(event, '${role.id}')"><i class="fas fa-ellipsis-h"></i></button>
            </div>
        `;
        listBody.appendChild(div);
    });
}

// DRAG AND DROP HANDLERS
let draggedRoleId = null;

window.handleRoleDragStart = function (e, roleId) {
    draggedRoleId = roleId;
    e.dataTransfer.effectAllowed = 'move';
    e.target.style.opacity = '0.5';
}

window.handleRoleDragOver = function (e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    return false;
}

window.handleRoleDrop = function (e, targetRoleId) {
    e.stopPropagation();
    e.preventDefault();

    const items = document.querySelectorAll('.settings-role-item');
    items.forEach(i => i.style.opacity = '1');

    if (draggedRoleId === targetRoleId) return;
    if (!window.currentServerRoles) return;

    const fromIndex = window.currentServerRoles.findIndex(r => r.id === draggedRoleId);
    const toIndex = window.currentServerRoles.findIndex(r => r.id === targetRoleId);

    if (fromIndex < 0 || toIndex < 0) return;

    const movedItem = window.currentServerRoles.splice(fromIndex, 1)[0];
    window.currentServerRoles.splice(toIndex, 0, movedItem);

    renderRolesList(window.currentServerRoles);

    const updates = window.currentServerRoles.map((r, index) => {
        const newPriority = window.currentServerRoles.length - index;
        return { id: r.id, priority: newPriority };
    });

    updates.forEach(u => {
        const role = window.currentServerRoles.find(r => r.id === u.id);
        if (role) {
            role.priority = u.priority;
            const payload = {
                name: role.name,
                color: role.color,
                priority: u.priority,
                permissionCodes: role.permissionCodes || []
            };
            Api.put(`/api/servers/${state.editingServerId}/roles/${u.id}`, payload)
                .catch(err => console.error("Update priority failed", err));
        }
    });
}

// --- FULL SCREEN ROLE EDITOR LOGIC ---
let currentEditorRoles = [];
let currentEditingRole = null;

function setupRoleEditorEvents() {
    const mainCreateBtn = document.querySelector('#tab-server-roles .btn-create-role-styled');
    if (mainCreateBtn) {
        mainCreateBtn.onclick = () => {
            // Create a specific "new role" object or just open editor with empty
            createNewRoleFromEditor();
        };
    }
}
// Call setup once or when modal opens
// We will call it in openServerSettings or ensure global binding

window.openRoleEditor = function (role) {
    // 1. Hide Main Settings
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    if (settingsLayout) settingsLayout.style.display = 'none';
    if (editorLayout) editorLayout.style.display = 'flex';

    // 2. Load Roles again to ensure fresh data in sidebar
    // We can use the cached roles from the main list if available, or fetch
    // For now, let's refetch or use what we have.
    loadRolesForEditor(role ? role.id : null);
}

window.closeRoleEditor = function () {
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    if (editorLayout) editorLayout.style.display = 'none';
    if (settingsLayout) settingsLayout.style.display = 'flex';

    // Refresh the main roles list in case of changes
    if (state.editingServerId) loadServerRoles(state.editingServerId);
}

async function loadRolesForEditor(activeRoleId) {
    if (!state.editingServerId) return;
    try {
        const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
        if (roles) {
            currentEditorRoles = roles.sort((a, b) => b.priority - a.priority);
            renderRoleEditorSidebar(currentEditorRoles, activeRoleId);

            // If activeRoleId is null (and not creating new), select default (first one or @everyone)
            if (!activeRoleId && currentEditorRoles.length > 0) {
                selectRoleInEditor(currentEditorRoles[0]);
            } else if (activeRoleId) {
                const found = currentEditorRoles.find(r => r.id === activeRoleId);
                if (found) selectRoleInEditor(found);
            }
        }
    } catch (e) {
        console.error("Error loading roles for editor", e);
    }
}

function renderRoleEditorSidebar(roles, activeRoleId) {
    const container = document.getElementById('role-edit-list-container');
    if (!container) return;
    container.innerHTML = '';

    roles.forEach(role => {
        const item = document.createElement('div');
        item.className = 'role-edit-item ' + (activeRoleId === role.id ? 'active' : '');
        item.setAttribute('data-role-id', role.id); // For easy finding

        // Color Circle
        const colorCircle = document.createElement('div');
        colorCircle.className = 'role-color-circle';
        colorCircle.style.cssText = `width: 12px; height: 12px; border-radius: 50%; background-color: ${role.color || '#99aab5'}; margin-right: 8px; flex-shrink: 0;`;

        const nameSpan = document.createElement('span');
        nameSpan.innerText = role.name;
        nameSpan.style.cssText = "white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1;";

        const countSpan = document.createElement('span');
        countSpan.innerText = role.memberCount !== undefined ? role.memberCount : 0;
        countSpan.style.cssText = "color: #b5bac1; font-size: 12px; margin-left: 8px;";

        item.style.display = 'flex';
        item.style.alignItems = 'center';

        item.appendChild(colorCircle);
        item.appendChild(nameSpan);
        item.appendChild(countSpan);

        item.onclick = () => selectRoleInEditor(role);



        container.appendChild(item);
    });
}

window.updateRoleColorPreview = function (color) {
    // 1. Update current editing role object
    if (currentEditingRole) {
        currentEditingRole.color = color;
    }

    // 2. Find active item in sidebar and update circle
    let activeItem = document.querySelector('.role-edit-item.active .role-color-circle');

    // Fallback: search by ID if active class missing for some reason
    if (!activeItem && currentEditingRole && currentEditingRole.id) {
        const item = document.querySelector(`.role-edit-item[data-role-id="${currentEditingRole.id}"]`);
        if (item) activeItem = item.querySelector('.role-color-circle');
    }

    if (activeItem) {
        activeItem.style.backgroundColor = color;
    }
}

// ESC Key Listener for Modals
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const settingsModal = document.getElementById('serverSettingsModal');
        if (settingsModal && settingsModal.style.display !== 'none') {
            // Check if we are in Role Editor?
            const editorView = document.getElementById('roles-editor-view');
            if (editorView && editorView.style.display !== 'none') {
                closeModal('serverSettingsModal'); // Or closeRoleEditor()? Discord closes modal.
            } else {
                closeModal('serverSettingsModal');
            }
        }
    }
});

// Update selectRoleInEditor to handle "New Role" active state in sidebar
function selectRoleInEditor(role) {
    currentEditingRole = role;
    renderRoleEditorSidebar(currentEditorRoles, role.id); // Re-render to highlight correctly by ID

    // Fill Form
    document.getElementById('role-editor-title').innerText = `SỬA ĐỔI VAI TRÒ - ${(role.name || 'VAI TRÒ MỚI').toUpperCase()}`;
    document.getElementById('edit-role-name').value = role.name;
    document.getElementById('edit-role-color').value = role.color || '#99aab5';

    switchRoleEditorTab('display');
    document.getElementById('role-save-bar').classList.remove('visible');
}

// PERMISSIONS DATA
const permissionsDefinitions = [
    {
        category: "Quyền Tổng Quát Máy Chủ",
        permissions: [
            { code: "VIEW_CHANNELS", name: "Xem Kênh", description: "Cho phép thành viên xem các kênh (không tính kênh riêng tư)." },
            { code: "MANAGE_CHANNELS", name: "Quản Lý Kênh", description: "Cho phép tạo, chỉnh sửa hoặc xóa kênh." },
            { code: "MANAGE_ROLES", name: "Quản Lý Vai Trò", description: "Cho phép thành viên tạo, chỉnh sửa hoặc xóa các vai trò thấp hơn vai trò của họ." },
            { code: "MANAGE_SERVER", name: "Quản Lý Máy Chủ", description: "Cho phép thay đổi tên máy chủ và xem lời mời." },
            { code: "VIEW_AUDIT_LOG", name: "Xem Nhật Ký Kiểm Tra", description: "Cho phép xem lịch sử thay đổi của máy chủ." }
        ]
    },
    {
        category: "Quản lý thành viên",
        permissions: [
            { code: "CREATE_INVITE", name: "Tạo Lời Mời", description: "Cho phép mời người mới vào máy chủ." },
            { code: "CHANGE_NICKNAME", name: "Đổi Biệt Danh", description: "Cho phép thay đổi biệt danh của chính mình." },
            { code: "MANAGE_NICKNAME", name: "Quản Lý Biệt Danh", description: "Cho phép thay đổi biệt danh của người khác." },
            { code: "KICK_APPROVE_REJECT_MEMBERS", name: "Đuổi Thành Viên", description: "Cho phép đuổi thành viên khỏi máy chủ." },
            { code: "BAN_MEMBERS", name: "Cấm Thành Viên", description: "Cho phép cấm vĩnh viễn thành viên khỏi máy chủ." },
            { code: "TIME_OUT_MEMBERS", name: "Timeout Thành Viên", description: "Cho phép cách ly thành viên tạm thời." }
        ]
    },
    {
        category: "Kênh Văn Bản",
        permissions: [
            { code: "SEND_MESSAGES", name: "Gửi Tin Nhắn", description: "Cho phép gửi tin nhắn trong kênh chat." },
            { code: "EMBED_LINK", name: "Nhúng Liên Kết", description: "Cho phép hiển thị bản xem trước của liên kết." },
            { code: "ATTACH_FILES", name: "Đính Kèm Tệp", description: "Cho phép tải lên tệp và ảnh." },
            { code: "ADD_REACTIONS", name: "Thêm Biểu Cảm", description: "Cho phép thêm biểu cảm (reaction) vào tin nhắn." },
            { code: "MANAGE_MESSAGES", name: "Quản Lý Tin Nhắn", description: "Cho phép xóa tin nhắn của người khác." },
            { code: "READ_MESSAGE_HISTORY", name: "Đọc Lịch Sử Tin Nhắn", description: "Cho phép xem tin nhắn cũ." }
        ]
    }
];

function renderRolePermissions(role) {
    const list = document.getElementById('role-permissions-list');
    if (!list) return;
    list.innerHTML = '';

    // Parse role permissions. 
    // Backend returns permissions as array of objects [{id, code, description}] or strings?
    // Based on ServerRoleServiceImpl, it maps codes to entities. 
    // The GET /roles API likely returns Set<Permission> or similar.
    // Let's assume role.permissions is an array of objects checking checks against .code

    const rolePermCodes = new Set((role.permissionCodes || role.permissions || []).map(p => typeof p === 'string' ? p : p.code));
    const searchQuery = (document.getElementById('permission-search-input').value || '').toLowerCase();

    permissionsDefinitions.forEach(group => {
        // Filter permissions in group
        const filteredPerms = group.permissions.filter(p =>
            p.name.toLowerCase().includes(searchQuery) ||
            p.description.toLowerCase().includes(searchQuery)
        );

        if (filteredPerms.length === 0) return;

        // Group Header
        const header = document.createElement('div');
        header.className = 'perm-group-header';
        header.innerText = group.category;
        header.style.cssText = "color: #b5bac1; font-size: 12px; font-weight: 700; text-transform: uppercase; margin: 20px 0 10px 0;";
        list.appendChild(header);

        // Perm Items
        filteredPerms.forEach(perm => {
            const item = document.createElement('div');
            item.className = 'perm-item';
            item.style.cssText = "display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #3f4147;";

            const info = document.createElement('div');
            info.innerHTML = `<div style="color: #dbdee1; font-weight: 500; font-size: 16px;">${perm.name}</div>
                               <div style="color: #b5bac1; font-size: 14px; margin-top: 4px;">${perm.description}</div>`;

            const toggle = document.createElement('div');
            const isActive = rolePermCodes.has(perm.code);
            toggle.className = `role-toggle ${isActive ? 'active' : ''}`;
            toggle.style.cssText = `width: 40px; height: 24px; background: ${isActive ? '#23a559' : '#80848e'}; border-radius: 14px; position: relative; cursor: pointer; transition: background 0.2s;`;
            toggle.innerHTML = `<div style="width: 18px; height: 18px; background: white; border-radius: 50%; position: absolute; top: 3px; left: ${isActive ? '19px' : '3px'}; transition: left 0.2s; display: flex; justify-content: center; align-items: center;">
                                    ${!isActive ? '<i class="fas fa-times" style="font-size: 10px; color: #80848e;"></i>' : '<i class="fas fa-check" style="font-size: 10px; color: #23a559;"></i>'}
                                 </div>`;

            toggle.onclick = () => togglePermission(perm.code, toggle);

            item.appendChild(info);
            item.appendChild(toggle);
            list.appendChild(item);
        });
    });
}

function togglePermission(code, toggleElement) {
    if (!currentEditingRole) return;

    // Determine target array (prefer permissionCodes as backend returns that)
    let targetArray;
    let isCodeArray = false;

    if (currentEditingRole.permissionCodes) {
        targetArray = currentEditingRole.permissionCodes;
        isCodeArray = true; // backend usually returns list of strings
    } else {
        if (!currentEditingRole.permissions) currentEditingRole.permissions = [];
        targetArray = currentEditingRole.permissions;
    }

    // Find index
    // If isCodeArray, elements are strings. If not, could be objects or strings.
    const permIndex = targetArray.findIndex(p => (typeof p === 'string' ? p : p.code) === code);

    let isActive = false;
    if (permIndex > -1) {
        // Remove
        targetArray.splice(permIndex, 1);
        isActive = false;
    } else {
        // Add
        if (isCodeArray) {
            targetArray.push(code);
        } else {
            targetArray.push({ code: code });
        }
        isActive = true;
    }

    // Update UI
    // Don't re-render entire list to keep scroll pos, just update toggle
    toggleElement.style.background = isActive ? '#23a559' : '#80848e';
    const circle = toggleElement.querySelector('div');
    circle.style.left = isActive ? '19px' : '3px';
    circle.innerHTML = isActive
        ? '<i class="fas fa-check" style="font-size: 10px; color: #23a559;"></i>'
        : '<i class="fas fa-times" style="font-size: 10px; color: #80848e;"></i>';
    toggleElement.classList.toggle('active', isActive);

    // Show save bar
    showRoleSaveBar();
}

// Search Listener
document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('permission-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            if (currentEditingRole) renderRolePermissions(currentEditingRole);
        });
    }
});

window.createNewRoleFromEditor = async function () {
    // 1. Ensure view is visible
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    // If editor is not visible, switch to it and load roles
    if (editorLayout && editorLayout.style.display === 'none') {
        if (settingsLayout) settingsLayout.style.display = 'none';
        editorLayout.style.display = 'flex';

        // Ensure roles are loaded before adding new one
        if (state.editingServerId) {
            // Populate currentEditorRoles first
            try {
                const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
                if (roles) currentEditorRoles = roles.sort((a, b) => b.priority - a.priority);
            } catch (e) { console.error(e); }
        }
    }

    // Draft Mode: Create a temporary role object
    const tempRole = {
        id: null, // Indicates it's new
        name: 'new role',
        color: '#99aab5',
        permissions: 0,
        priority: 0
    };

    // Add to current list temporarily for display
    currentEditorRoles.unshift(tempRole); // Add to top
    selectRoleInEditor(tempRole);
    // Focus Name input
    setTimeout(() => {
        const input = document.getElementById('edit-role-name');
        if (input) {
            input.focus();
            input.select();
        }
    }, 100);
}


window.saveRoleChanges = function () {
    if (!currentEditingRole) return;

    const name = document.getElementById('edit-role-name').value;
    const color = document.getElementById('edit-role-color').value;

    if (!name || name.trim() === '') {
        Swal.fire('Lỗi', 'Tên vai trò không được để trống', 'error');
        return;
    }

    const sourcePerms = currentEditingRole.permissionCodes || currentEditingRole.permissions || [];
    const permCodes = sourcePerms.map(p => (typeof p === 'string' ? p : p.code));

    const roleData = {
        ...currentEditingRole,
        name: name,
        color: color,
        permissionCodes: permCodes
    };
    delete roleData.permissions; // Don't send legacy field

    if (roleData.id === null) {
        // CREATE
        Api.post(`/api/servers/${state.editingServerId}/roles`, roleData)
            .then(created => {
                // Update local list
                currentEditorRoles.shift(); // Remove draft
                currentEditorRoles.unshift(created); // Add real role
                currentEditingRole = created;

                renderRoleEditorSidebar(currentEditorRoles, created.id);
                document.getElementById('role-save-bar').classList.remove('visible');
                // Don't reset tab
                Swal.fire({
                    icon: 'success',
                    title: 'Đã tạo vai trò!',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 2000,
                    background: '#1e1f22',
                    color: '#fff'
                });
            })
            .catch(e => Swal.fire('Lỗi', 'Tạo thất bại', 'error'));
    } else {
        // UPDATE
        Api.put(`/api/servers/${state.editingServerId}/roles/${roleData.id}`, roleData)
            .then(updated => {
                // Update local list
                const idx = currentEditorRoles.findIndex(r => r.id === updated.id);
                if (idx !== -1) currentEditorRoles[idx] = updated;
                currentEditingRole = updated;

                renderRoleEditorSidebar(currentEditorRoles, updated.id);
                document.getElementById('role-save-bar').classList.remove('visible');

                Swal.fire({
                    icon: 'success',
                    title: 'Đã lưu thay đổi!',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 2000,
                    background: '#1e1f22',
                    color: '#fff'
                });

                // Update main role list even if hidden, so it's fresh when exiting editor
                renderRolesList(currentEditorRoles);
            })
            .catch(e => {
                console.error(e);
                Swal.fire('Lỗi', 'Lưu thất bại', 'error');
            });
    }
}

window.resetRoleChanges = async function () {
    if (!currentEditingRole) return;

    const roleId = currentEditingRole.id;

    if (!roleId) {
        // It's a draft (New Role)
        document.getElementById('edit-role-name').value = 'new role';
        document.getElementById('edit-role-color').value = '#99aab5';
        updateRoleColorPreview('#99aab5');

        // Reset permissions for draft (empty)
        if (currentEditingRole.permissionCodes) currentEditingRole.permissionCodes = [];
        if (currentEditingRole.permissions) currentEditingRole.permissions = [];

        // Re-render permissions if on that tab
        const permTab = document.getElementById('role-tab-permissions');
        if (permTab && permTab.style.display !== 'none') {
            renderRolePermissions(currentEditingRole);
        }

        document.getElementById('role-save-bar').classList.remove('visible');
        Swal.fire({
            icon: 'success',
            title: 'Đã đặt lại!',
            toast: true,
            position: 'bottom-end',
            showConfirmButton: false,
            timer: 1500,
            background: '#1e1f22',
            color: '#fff'
        });
        return;
    }

    // Existing Role: Re-fetch text/color to be safe
    // Ideally we re-fetch from server to be 100% sure we revert to saved state
    try {
        // Fetch single role or find in list re-fetched
        const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
        const cleanRole = roles.find(r => r.id === roleId);

        if (cleanRole) {
            // Update reference
            const idx = currentEditorRoles.findIndex(r => r.id === roleId);
            if (idx !== -1) currentEditorRoles[idx] = cleanRole;
            currentEditingRole = cleanRole;

            // Restore UI inputs
            document.getElementById('edit-role-name').value = cleanRole.name;
            document.getElementById('edit-role-color').value = cleanRole.color || '#99aab5';
            updateRoleColorPreview(cleanRole.color || '#99aab5');

            // Restore Permissions Tab if visible
            const permTab = document.getElementById('role-tab-permissions');
            if (permTab && permTab.style.display !== 'none') {
                renderRolePermissions(cleanRole);
            }

            // Hide Bar
            document.getElementById('role-save-bar').classList.remove('visible');

            // Toast
            Swal.fire({
                icon: 'success',
                title: 'Đã đặt lại!',
                toast: true,
                position: 'bottom-end',
                showConfirmButton: false,
                timer: 1500,
                background: '#1e1f22',
                color: '#fff'
            });
        }
    } catch (e) {
        console.error("Reset failed", e);
        Swal.fire('Lỗi', 'Không thể đặt lại dữ liệu', 'error');
    }
}

// Monitor changes to show Save Bar
document.getElementById('edit-role-name').addEventListener('input', showRoleSaveBar);
document.getElementById('edit-role-color').addEventListener('input', showRoleSaveBar);

function showRoleSaveBar() {
    document.getElementById('role-save-bar').classList.add('visible');
}



window.switchRoleEditorTab = function (tabName) {
    // Update tab headers
    const headers = document.querySelectorAll('.role-tab-item');
    headers.forEach(h => {
        if (h.getAttribute('onclick').includes(tabName)) {
            h.classList.add('active');
            h.style.color = "white";
        } else {
            h.classList.remove('active');
            h.style.color = "#b5bac1";
        }
    });

    // Update content
    const contents = document.querySelectorAll('.role-tab-content');
    contents.forEach(c => c.style.display = 'none');

    const activeContent = document.getElementById('role-tab-' + tabName);
    if (activeContent) {
        if (tabName === 'display' || tabName === 'permissions') {
            activeContent.style.display = 'flex';
        } else {
            activeContent.style.display = 'block';
        }

        if (tabName === 'permissions') {
            renderRolePermissions(currentEditingRole);
        }
        if (tabName === 'members') {
            loadRoleMembers(currentEditingRole.id);
        }
    }
}



// MEMBER ROLE ASSIGNMENT POPUP (OLD NAME: ROLE MANAGEMENT)
// We need a global listener for clicking outside popover
document.addEventListener('click', (e) => {
    if (!e.target.closest('.role-selector-popover') && !e.target.closest('.btn-add-role')) {
        closeRoleSelector();
    }
});

let currentRoleSelectorUserId = null;

window.openRoleSelector = async function (event, userId, currentRolesJson) {
    event.stopPropagation();
    closeRoleSelector(); // Close others

    currentRoleSelectorUserId = userId;
    const currentRoles = JSON.parse(currentRolesJson);
    const existingRoleIds = new Set(currentRoles.map(r => r.id));

    // Fetch All Roles needed? Or use cache?
    // Let's assume we fetch them once or every time. Fetching every time is safer for sync.
    let allRoles = [];
    try {
        allRoles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
    } catch (e) {
        console.error("Lỗi fetch roles:", e);
        return;
    }

    // Create Popover Element
    const popover = document.createElement('div');
    popover.className = 'role-selector-popover visible';

    // Position it
    const rect = event.target.getBoundingClientRect();
    const modalRect = document.querySelector('.modal-box').getBoundingClientRect();

    // Calculate relative position inside modal
    popover.style.left = (event.clientX) + 'px';
    popover.style.top = (event.clientY + 20) + 'px';
    // Ideally we append to body or a fixed container to avoid overflow issues, 
    // but appending to body requires adjusting coordinates to page.
    popover.style.position = 'fixed'; // Easiest way to position relative to viewport

    let html = '';
    // Filter out @everyone usually? Or allow it? AssignRolesRequest might handle it.
    // Usually @everyone is default. Let's filter it out if name is '@everyone' or verify ID.
    // user always has everyone role.

    allRoles.filter(r => r.name !== '@everyone').forEach(r => {
        const checked = existingRoleIds.has(r.id) ? 'checked' : '';
        html += `
            <label class="role-item">
                <input type="checkbox" value="${r.id}" ${checked} onchange="toggleRoleForUser(${userId}, ${r.id}, this.checked)">
                <div style="width: 10px; height: 10px; border-radius: 50%; background-color: ${r.color || '#99aab5'}; margin-right: 8px;"></div>
                ${r.name}
            </label>
        `;
    });

    if (html === '') html = '<div style="padding: 8px; color: #b5bac1; font-size: 12px;">Không có vai trò nào</div>';

    popover.innerHTML = html;
    document.body.appendChild(popover);
}

window.closeRoleSelector = function () {
    const existing = document.querySelector('.role-selector-popover');
    if (existing) existing.remove();
    currentRoleSelectorUserId = null;
}

window.toggleRoleForUser = async function (userId, roleId, isAdded) {
    // We need to construct the FULL list of roles for this user to send to backend
    // 1. Find the member in cache
    const member = state.membersCache.find(m => m.user.id === userId);
    if (!member) return;

    let currentRoleIds = new Set(member.roles.map(r => r.id));

    if (isAdded) {
        currentRoleIds.add(roleId);
    } else {
        currentRoleIds.delete(roleId);
    }

    // Call API
    try {
        await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, {
            userId: userId,
            roleIds: Array.from(currentRoleIds)
        });

        // Update local cache to reflect change immediately (optimistic UI)
        // We know we updated it, so reload members to get fresh data (or simpler: update local cache)
        loadServerMembers(state.editingServerId);
    } catch (e) {
        console.error("Lỗi cập nhật role:", e);
        alert("Không thể cập nhật vai trò: " + e.message);
        // Revert checkbox? For now simpler to reload or just alert.
    }
}

window.kickMember = async function (userId) {
    const result = await Swal.fire({
        title: 'Đuổi thành viên?',
        text: "Hành động này sẽ xóa thành viên khỏi máy chủ.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Đuổi đi!',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1'
    });

    if (result.isConfirmed) {
        try {
            await Api.delete(`/api/servers/${state.editingServerId}/members/${userId}`);
            Swal.fire({
                title: 'Đã đuổi!',
                icon: 'success',
                timer: 1500,
                showConfirmButton: false,
                background: '#313338',
                color: '#dbdee1'
            });
            loadServerMembers(state.editingServerId); // Reload
        } catch (e) {
            Swal.fire('Lỗi', 'Không thể đuổi thành viên: ' + e.message, 'error');
        }
    }
}



// Confirm Delete
window.confirmDeleteServer = async function () {
    const serverId = state.editingServerId;
    try {
        const success = await Api.delete(`/api/servers/${serverId}`);
        if (success) {
            closeModal('serverSettingsModal');
            Swal.fire('Thành công', 'Đã xóa server', 'success').then(() => location.reload());
        } else {
            Swal.fire('Lỗi', 'Không thể xóa server', 'error');
        }
    } catch (e) {
        Swal.fire('Lỗi', e.message, 'error');
    }
};

// Old helpers reference in case needed (deleted)

// Helper: Calculate Time Ago
function calculateTimeAgo(dateString) {
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

// Save Overview
window.saveServerOverview = async function () {
    const nameInput = document.getElementById('edit-server-name');
    const descInput = document.getElementById('edit-server-desc');

    if (!nameInput || !nameInput.value.trim()) {
        Swal.fire('Lỗi', 'Tên máy chủ không được để trống', 'error');
        return;
    }

    let iconUrl = state.editingServerData.iconUrl;

    // Handle Icon Upload
    if (state.tempServerIcon && state.tempServerIcon !== 'REMOVED') {
        const formData = new FormData();
        formData.append('file', state.tempServerIcon);

        try {
            const uploadRes = await Api.upload('/api/upload', formData);
            if (uploadRes && uploadRes.url) {
                iconUrl = uploadRes.url;
            }
        } catch (e) {
            console.error(e);
            Swal.fire('Lỗi', 'Không thể upload ảnh: ' + e.message, 'error');
            return;
        }
    } else if (state.tempServerIcon === 'REMOVED') {
        iconUrl = ''; // Backend normalize sets to null if blank
    }

    const payload = {
        name: nameInput.value.trim(),
        description: descInput ? descInput.value.trim() : null,
        iconUrl: iconUrl
    };

    try {
        const updatedServer = await Api.put(`/api/servers/${state.editingServerId}`, payload);

        // Update Success
        state.editingServerData = updatedServer;
        state.tempServerIcon = null;
        document.getElementById('editServerIconInput').value = '';
        const saveBar = document.getElementById('server-save-bar');
        if (saveBar) saveBar.classList.remove('visible');

        await loadServers(); // Refresh sidebar & cache

        Swal.fire({
            icon: 'success',
            title: 'Đã lưu thay đổi!',
            toast: true,
            position: 'bottom-end',
            showConfirmButton: false,
            timer: 2000,
            background: '#1e1f22',
            color: '#fff'
        });

    } catch (e) {
        console.error(e);
        Swal.fire('Lỗi', 'Lưu hồ sơ thất bại', 'error');
    }
}

// --- ROLE MEMBERS MANAGEMENT LOGIC ---
let currentRoleMembers = [];
let addMemberCandidates = [];
let selectedAddMemberIds = new Set();

window.loadRoleMembers = async function (roleId) {
    const listDiv = document.getElementById('role-members-list');
    listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Đang tải thành viên...</div>';

    try {
        const members = await Api.get(`/api/servers/${state.editingServerId}/roles/${roleId}/members`);
        currentRoleMembers = members || [];
        renderRoleMembers(currentRoleMembers);
    } catch (e) {
        console.error("Load Role Members Error", e);
        listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi tải thành viên.</div>';
    }
}

window.renderRoleMembers = function (members) {
    const listDiv = document.getElementById('role-members-list');
    listDiv.innerHTML = '';

    if (!members || members.length === 0) {
        listDiv.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Chưa có thành viên nào đảm nhận vai trò này.</div>';
        return;
    }

    members.forEach(m => {
        const div = document.createElement('div');
        div.className = 'role-member-item';
        // Avatar fallback
        const avatarUrl = m.user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png';
        const displayName = m.nickname || m.user.displayName || m.user.username;

        div.innerHTML = `
            <div class="rm-info">
                <img src="${avatarUrl}" class="rm-avatar">
                <div>
                    <span class="rm-name">${displayName}</span>
                    <span class="rm-username">${m.user.username}</span>
                </div>
            </div>
            <i class="fas fa-times btn-remove-member" title="Gỡ vai trò" onclick="removeRoleFromMember(${m.user.id})"></i>
        `;
        listDiv.appendChild(div);
    });
}

window.filterRoleMembers = function () {
    const keyword = document.getElementById('role-members-search').value.toLowerCase();
    const filtered = currentRoleMembers.filter(m => {
        const name = (m.nickname || m.user.displayName || '').toLowerCase();
        const username = m.user.username.toLowerCase();
        return name.includes(keyword) || username.includes(keyword);
    });
    renderRoleMembers(filtered);
}

window.removeRoleFromMember = async function (userId) {
    if (!currentEditingRole) return;

    const member = currentRoleMembers.find(m => m.user.id === userId);
    if (!member) return;

    const result = await Swal.fire({
        title: 'Xóa thành viên khỏi vai trò?',
        text: `Bạn có chắc chắn muốn xóa ${member.nickname || member.user.displayName || member.user.username} khỏi vai trò này?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1',
        zIndex: 10005 // Ensure it's on top of role editor (usually 10001)
    });

    if (!result.isConfirmed) return;

    const remainingRoleIds = member.roles
        .filter(r => r.id !== currentEditingRole.id)
        .map(r => r.id);

    try {
        await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, {
            userId: userId,
            roleIds: remainingRoleIds
        });

        // Remove from local list immediately
        currentRoleMembers = currentRoleMembers.filter(m => m.user.id !== userId);
        filterRoleMembers(); // Re-render logic

        // Also update main cache if needed
        loadServerMembers(state.editingServerId);

        Swal.fire({
            title: 'Đã xóa!',
            icon: 'success',
            timer: 1000,
            showConfirmButton: false,
            background: '#313338',
            color: '#dbdee1'
        });
    } catch (e) {
        Swal.fire('Lỗi', 'Không thể gỡ vai trò: ' + e.message, 'error');
    }
}

// --- ADD MEMBER MODAL LOGIC ---
window.openAddRoleMemberModal = async function () {
    const modal = document.getElementById('add-role-member-modal');
    modal.style.display = 'flex';
    document.getElementById('add-role-member-list').innerHTML = '<div style="padding:10px; color:#b5bac1;">Đang tải...</div>';

    // Refresh cache
    await loadServerMembers(state.editingServerId);

    // Filter candidates: All members who do NOT have this role
    const currentRoleId = currentEditingRole.id;
    addMemberCandidates = state.membersCache.filter(m => {
        const hasRole = m.roles && m.roles.some(r => r.id === currentRoleId);
        return !hasRole;
    });

    selectedAddMemberIds.clear();
    renderAddMemberCandidates(addMemberCandidates);
}

window.closeAddRoleMemberModal = function () {
    document.getElementById('add-role-member-modal').style.display = 'none';
}

window.renderAddMemberCandidates = function (list) {
    const container = document.getElementById('add-role-member-list');
    container.innerHTML = '';

    if (list.length === 0) {
        container.innerHTML = '<div style="padding:10px; color:#b5bac1;">Không tìm thấy thành viên phù hợp.</div>';
        return;
    }

    list.forEach(m => {
        const div = document.createElement('div');
        div.className = 'add-role-member-item';
        if (selectedAddMemberIds.has(m.user.id)) div.classList.add('selected');

        const avatarUrl = m.user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png';
        const displayName = m.nickname || m.user.displayName || m.user.username;

        div.onclick = () => toggleAddRoleMember(m.user.id, div);

        div.innerHTML = `
            <div class="selection-circle">
                ${selectedAddMemberIds.has(m.user.id) ? '<i class="fas fa-check" style="font-size:10px;"></i>' : ''}
            </div>
            <img src="${avatarUrl}" style="width: 32px; height: 32px; border-radius: 50%; margin-right: 10px; object-fit: cover;">
            <div style="font-weight: 500; color: #f2f3f5;">${displayName}</div>
            <div style="color: #b5bac1; font-size: 12px; margin-left: 6px;">${m.user.username}</div>
        `;
        container.appendChild(div);
    });
}

window.filterAddRoleMembers = function () {
    const keyword = document.getElementById('add-role-member-search').value.toLowerCase();
    const filtered = addMemberCandidates.filter(m => {
        const name = (m.nickname || m.user.displayName || '').toLowerCase();
        const username = m.user.username.toLowerCase();
        return name.includes(keyword) || username.includes(keyword);
    });
    renderAddMemberCandidates(filtered);
}

window.toggleAddRoleMember = function (userId, divElement) {
    if (selectedAddMemberIds.has(userId)) {
        selectedAddMemberIds.delete(userId);
        divElement.classList.remove('selected');
        divElement.querySelector('.selection-circle').innerHTML = '';
        divElement.querySelector('.selection-circle').style.backgroundColor = 'transparent';
        divElement.querySelector('.selection-circle').style.borderColor = '#b5bac1';
    } else {
        selectedAddMemberIds.add(userId);
        divElement.classList.add('selected');
        divElement.querySelector('.selection-circle').innerHTML = '<i class="fas fa-check" style="font-size:10px;"></i>';
        divElement.querySelector('.selection-circle').style.backgroundColor = '#5865F2';
        divElement.querySelector('.selection-circle').style.borderColor = '#5865F2';
    }
}

window.submitAddRoleMembers = async function () {
    if (selectedAddMemberIds.size === 0) return closeAddRoleMemberModal();

    // Process additions
    const currentRoleId = currentEditingRole.id;
    let successCount = 0;

    for (const userId of selectedAddMemberIds) {
        const member = state.membersCache.find(m => m.user.id === userId);
        if (member) {
            const newRoleIds = member.roles.map(r => r.id);
            newRoleIds.push(currentRoleId);

            try {
                await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, {
                    userId: userId,
                    roleIds: newRoleIds
                });
                successCount++;
            } catch (e) {
                console.error(`Failed to add role for user ${userId}`, e);
            }
        }
    }

    if (successCount > 0) {
        Swal.fire({
            icon: 'success',
            title: `Đã thêm ${successCount} thành viên`,
            toast: true,
            position: 'bottom-end',
            showConfirmButton: false,
            timer: 2000,
            background: '#1e1f22',
            color: '#fff'
        });
        loadRoleMembers(currentRoleId);
    }

    closeAddRoleMemberModal();
}


// Update deleteCurrentRole to accept optional ID or fallback to currentEditingRole
window.deleteCurrentRole = async function (optionalId) {
    const roleId = optionalId || (currentEditingRole ? currentEditingRole.id : null);

    if (!roleId) return;

    // Check @everyone logic locally if needed, or let backend fail
    // @everyone likely has specific ID or Name. But simple safety:
    // Fetch role details? Or just try delete.

    const result = await Swal.fire({
        title: 'Xóa Vai Trò?',
        text: "Hành động này không thể hoàn tác.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#da373c',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1'
    });

    if (result.isConfirmed) {
        try {
            await Api.delete(`/api/servers/${state.editingServerId}/roles/${roleId}`);

            Swal.fire({
                icon: 'success',
                title: 'Đã xóa!',
                toast: true,
                position: 'bottom-end',
                showConfirmButton: false,
                timer: 1500,
                background: '#1e1f22',
                color: '#fff'
            });

            // OPTIMISTIC UPDATE: Remove from local state immediately
            if (window.currentServerRoles) {
                // Fix: Ensure ID comparison is type-safe (String vs Number)
                window.currentServerRoles = window.currentServerRoles.filter(r => String(r.id) !== String(roleId));

                // Force re-render of lists immediately
                renderRoleEditorSidebar(window.currentServerRoles);
                renderRolesList(window.currentServerRoles); // Also update main list

                // Update roles count label if exists
                const countLabel = document.getElementById('roles-count-label');
                if (countLabel) countLabel.innerText = window.currentServerRoles.length;
            }

            // If we deleted the currently editing role, switch to another
            if (currentEditingRole && currentEditingRole.id === roleId) {
                const first = window.currentServerRoles?.[0]; // Usually @everyone
                if (first) {
                    selectRoleInEditor(first);
                } else {
                    // Fallback if no roles left (unlikely due to @everyone)
                    currentEditingRole = null;
                }
            } else {
                // Refresh permissions/members for current role just in case
                if (currentEditingRole) loadRolesForEditor(currentEditingRole.id);
            }

            // Sync with server in background (to be sure and get fresh data)
            await loadServerRoles(state.editingServerId);

            // CRITICAL FIX: Ensure sidebar is re-rendered with the data we just fetched from server
            if (window.currentServerRoles) {
                // Preserve active selection if possible, currently we might have lost it
                renderRoleEditorSidebar(window.currentServerRoles, currentEditingRole ? currentEditingRole.id : null);
            }

        } catch (e) {
            Swal.fire('Lỗi', 'Không thể xóa vai trò: ' + e.message, 'error');
        }
    }
}

// --- ROLE ACTION MENU LOGIC (Three Dots) ---
let actionMenuTargetRoleId = null;

window.showRoleActionMenu = function (e, roleId) {
    e.stopPropagation();
    e.preventDefault();

    actionMenuTargetRoleId = roleId;

    // Remove existing if any
    const existing = document.getElementById('dynamic-role-menu');
    if (existing) existing.remove();

    // Create new menu
    const menu = document.createElement('div');
    menu.id = 'dynamic-role-menu';
    // Base styles
    Object.assign(menu.style, {
        position: 'fixed',
        zIndex: '100000',
        minWidth: '220px',
        backgroundColor: '#111214',
        border: '1px solid #1e1f22',
        borderRadius: '4px',
        padding: '6px 8px',
        boxShadow: '0 8px 16px rgba(0,0,0,0.24)',
        display: 'block' // Always block
    });

    menu.innerHTML = `
        <div class="dynamic-menu-item" onclick="handleViewAsRole()" 
             onmouseover="this.style.backgroundColor='#404249'; this.style.color='white'" 
             onmouseout="this.style.backgroundColor='transparent'; this.style.color='#b5bac1'"
             style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-radius: 2px; cursor: pointer; color: #b5bac1; font-weight: 500; font-size: 14px; transition: all 0.1s;">
            <span>Xem Máy Chủ Theo Vai Trò</span>
            <i class="fas fa-arrow-right"></i>
        </div>
        <div style="height: 1px; background-color: #2b2d31; margin: 4px 0;"></div>
        <div class="dynamic-menu-item" onclick="confirmDeleteRoleFromActionMenu()"
             onmouseover="this.style.backgroundColor='#da373c'; this.style.color='white'" 
             onmouseout="this.style.backgroundColor='transparent'; this.style.color='#da373c'"
             style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-radius: 2px; cursor: pointer; color: #da373c; font-weight: 500; font-size: 14px; transition: all 0.1s;">
            <span>Xóa</span>
            <i class="fas fa-trash-can"></i>
        </div>
    `;

    document.body.appendChild(menu);

    // Position relative to mouse
    let left = e.clientX - 210;
    let top = e.clientY + 10;

    // Boundary checks
    const rect = { width: 220, height: 100 }; // Approx
    if (left + rect.width > window.innerWidth) left = window.innerWidth - rect.width - 10;
    if (top + rect.height > window.innerHeight) top = window.innerHeight - rect.height - 10;
    if (left < 10) left = 10;

    menu.style.left = left + 'px';
    menu.style.top = top + 'px';
}

window.closeRoleActionMenu = function () {
    const menu = document.getElementById('dynamic-role-menu');
    if (menu) menu.remove();
}

window.handleViewAsRole = function () {
    closeRoleActionMenu();
    Swal.fire({
        title: 'Tính năng đang phát triển',
        text: 'Chức năng "Xem máy chủ theo vai trò" sẽ sớm ra mắt!',
        icon: 'info',
        background: '#313338',
        color: '#dbdee1'
    });
}

window.confirmDeleteRoleFromActionMenu = function () {
    if (actionMenuTargetRoleId) {
        deleteCurrentRole(actionMenuTargetRoleId);
    }
    closeRoleActionMenu();
}

// Global click to close menus
document.addEventListener('click', (e) => {
    if (!e.target.closest('#dynamic-role-menu') && !e.target.closest('.btn-role-action')) {
        closeRoleActionMenu();
    }
});



window.updateServerPreviews = function (name, iconUrl) {
    // 1. Update Name on Preview Card
    const nameEl = document.getElementById('preview-card-name');
    if (nameEl) nameEl.innerText = name || 'Server Name';

    // 2. Update Icon on Preview Card
    const imgEl = document.getElementById('preview-card-icon-img');
    const textEl = document.getElementById('preview-card-icon-text');

    if (imgEl && textEl) {
        if (iconUrl) {
            imgEl.src = iconUrl;
            imgEl.style.display = 'block';
            textEl.style.display = 'none';
        } else {
            imgEl.style.display = 'none';
            textEl.style.display = 'block';
            textEl.innerText = (name || '').substring(0, 2).toUpperCase();
        }
    }

    // 3. Update Icon in Settings Panel (Left side)
    const settingsImgEl = document.getElementById('edit-server-icon-preview');
    const settingsPlaceholderEl = document.getElementById('edit-server-icon-placeholder');

    if (settingsImgEl && settingsPlaceholderEl) {
        if (iconUrl) {
            settingsImgEl.src = iconUrl;
            settingsImgEl.style.display = 'block';
            settingsPlaceholderEl.style.display = 'none';
        } else {
            settingsImgEl.style.display = 'none';
            settingsPlaceholderEl.style.display = 'block';
            settingsPlaceholderEl.innerText = (name || '').substring(0, 2).toUpperCase();
        }
    }

    // 4. Update "Remove Icon" Button Visibility
    const removeBtn = document.querySelector('.remove-icon-btn');
    if (removeBtn) {
        // Show if there is an iconUrl
        removeBtn.style.display = iconUrl ? 'block' : 'none';
    }
}
