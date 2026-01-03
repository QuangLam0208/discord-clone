
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
        contentHtml = `<img src="${server.iconUrl}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
    } else {
        const acronym = server.name.substring(0, 2).toUpperCase();
        contentHtml = `<div class="sidebar-icon">${acronym}</div>`;
    }
    div.innerHTML = `<div class="pill"></div>${contentHtml}<span class="tooltip">${server.name}</span>`;
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

        // Populate Form
        document.getElementById('edit-server-name').value = server.name;
        document.getElementById('edit-server-desc').value = server.description || '';
        document.getElementById('delete-server-name-display').innerText = server.name;

        // Initial Preview Render
        updateServerPreviews(server.name, server.iconUrl);

        // Reset Inputs & Bar
        document.getElementById('editServerIconInput').value = '';
        state.tempServerIcon = null; // Ensure this is definitely null
        const saveBar = document.getElementById('server-save-bar');
        if (saveBar) saveBar.classList.remove('visible');

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
}

// Switch Tab
window.switchServerSettingsTab = function (tabName) {
    // Sidebar items
    document.querySelectorAll('#serverSettingsModal .settings-nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`nav-server-${tabName}`).classList.add('active');

    // Content areas
    document.querySelectorAll('#serverSettingsModal .settings-tab-content').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-server-${tabName}`).classList.add('active');

    // Load data if switching to members
    if (tabName === 'members') {
        loadServerMembers(state.editingServerId);
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

        // Date formatting relative (mocked for now, simple string)
        const joinedAt = new Date(m.joinedAt).toLocaleDateString('vi-VN');
        const joinedDiscord = "2 năm trước"; // Mock data
        const joinMethod = "Không xác định"; // Mock data

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

// ROLE MANAGEMENT
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

// Save Overview
window.saveServerOverview = async function () {
    const name = document.getElementById('edit-server-name').value;
    const description = document.getElementById('edit-server-desc').value;
    const serverId = state.editingServerId;

    if (!name) return alert("Tên máy chủ không được để trống");

    // Handle Icon Upload
    let iconUrl = state.editingServerData.iconUrl; // Keep old by default

    // If removed
    if (state.tempServerIcon === 'REMOVED') {
        iconUrl = null;
    }
    // If new file
    else if (state.tempServerIcon && typeof state.tempServerIcon === 'object') {
        if (state.tempServerIcon.size > 10 * 1024 * 1024) {
            return alert("File quá lớn (Max 10MB)");
        }
        try {
            const formData = new FormData();
            formData.append('file', state.tempServerIcon);
            const res = await Api.upload('/api/upload', formData);
            iconUrl = res.url;
        } catch (e) {
            return alert("Lỗi upload ảnh: " + e.message);
        }
    }

    try {
        await Api.put(`/api/servers/${serverId}`, {
            name: name,
            description: description,
            iconUrl: iconUrl
        });

        // Success feedback
        Swal.fire({
            icon: 'success',
            title: 'Đã lưu!',
            toast: true,
            position: 'bottom-end',
            showConfirmButton: false,
            timer: 3000,
            background: '#313338',
            color: '#dbdee1'
        });

        // Update State
        state.editingServerData = { name, description, iconUrl };
        state.tempServerIcon = null;
        document.getElementById('server-save-bar').classList.remove('visible');

        await loadServers(); // Refresh sidebar

        // Update header if still open
        const header = document.getElementById('server-settings-header');
        if (header) header.querySelector('span').innerText = name.toUpperCase();

        // Sync Delete Tab Name
        const deleteNameDisplay = document.getElementById('delete-server-name-display');
        if (deleteNameDisplay) deleteNameDisplay.innerText = name;

    } catch (e) {
        Swal.fire('Lỗi', e.message, 'error');
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
