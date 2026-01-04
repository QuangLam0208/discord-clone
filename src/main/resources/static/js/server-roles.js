// --- SERVER ROLES TAB LOGIC ---
if (!window.state) { window.state = {}; }

window.loadServerRoles = async function (serverId) {
    const listBody = document.getElementById('roles-list-body');
    const countLabel = document.getElementById('roles-count-label');
    if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Đang tải...</div>';

    try {
        const roles = await Api.get(`/api/servers/${serverId}/roles`);
        // Sort by priority desc (assuming higher priority = top)
        if (roles) {
            // Sort logic: higher priority means higher in list
            roles.sort((a, b) => b.priority - a.priority);

            window.currentServerRoles = roles; // Store for drag reorder

            renderRolesList(roles);
            if (countLabel) countLabel.innerText = roles.length || 0;
            // Setup events if not already
            if (window.setupRoleEditorEvents) setupRoleEditorEvents();
        } else {
            if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #b5bac1;">Không có vai trò nào.</div>';
        }
    } catch (e) {
        console.error("Load Roles Error", e);
        if (listBody) listBody.innerHTML = '<div style="padding: 20px; text-align: center; color: #f23f42;">Lỗi tải vai trò.</div>';
    }
}

window.renderRolesList = function (roles) {
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

window.setupRoleEditorEvents = function () {
    const mainCreateBtn = document.querySelector('#tab-server-roles .btn-create-role-styled');
    if (mainCreateBtn) {
        mainCreateBtn.onclick = () => {
            createNewRoleFromEditor();
        };
    }
}

window.openRoleEditor = function (role) {
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    if (settingsLayout) settingsLayout.style.display = 'none';
    if (editorLayout) editorLayout.style.display = 'flex';

    loadRolesForEditor(role ? role.id : null);
}

window.closeRoleEditor = function () {
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    if (editorLayout) editorLayout.style.display = 'none';
    if (settingsLayout) settingsLayout.style.display = 'flex';

    if (state.editingServerId) loadServerRoles(state.editingServerId);
}

async function loadRolesForEditor(activeRoleId) {
    if (!state.editingServerId) return;
    try {
        const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
        if (roles) {
            currentEditorRoles = roles.sort((a, b) => b.priority - a.priority);
            renderRoleEditorSidebar(currentEditorRoles, activeRoleId);

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
        item.setAttribute('data-role-id', role.id);

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
    if (currentEditingRole) {
        currentEditingRole.color = color;
    }
    let activeItem = document.querySelector('.role-edit-item.active .role-color-circle');
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
            const editorView = document.getElementById('roles-editor-view');
            if (editorView && editorView.style.display !== 'none') {
                closeModal('serverSettingsModal');
            } else {
                closeModal('serverSettingsModal');
            }
        }
    }
});

function selectRoleInEditor(role) {
    currentEditingRole = role;
    renderRoleEditorSidebar(currentEditorRoles, role.id);

    document.getElementById('role-editor-title').innerText = `SỬA ĐỔI VAI TRÒ - ${(role.name || 'VAI TRÒ MỚI').toUpperCase()}`;
    document.getElementById('edit-role-name').value = role.name;
    document.getElementById('edit-role-color').value = role.color || '#99aab5';

    switchRoleEditorTab('display');
    const bar = document.getElementById('role-save-bar');
    if (bar) bar.classList.remove('visible');
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

window.renderRolePermissions = function (role) {
    const list = document.getElementById('role-permissions-list');
    if (!list) return;
    list.innerHTML = '';

    const rolePermCodes = new Set((role.permissionCodes || role.permissions || []).map(p => typeof p === 'string' ? p : p.code));
    const searchQuery = (document.getElementById('permission-search-input').value || '').toLowerCase();

    permissionsDefinitions.forEach(group => {
        const filteredPerms = group.permissions.filter(p =>
            p.name.toLowerCase().includes(searchQuery) ||
            p.description.toLowerCase().includes(searchQuery)
        );

        if (filteredPerms.length === 0) return;

        const header = document.createElement('div');
        header.className = 'perm-group-header';
        header.innerText = group.category;
        header.style.cssText = "color: #b5bac1; font-size: 12px; font-weight: 700; text-transform: uppercase; margin: 20px 0 10px 0;";
        list.appendChild(header);

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

window.togglePermission = function (code, toggleElement) {
    if (!currentEditingRole) return;

    let targetArray;
    let isCodeArray = false;

    if (currentEditingRole.permissionCodes) {
        targetArray = currentEditingRole.permissionCodes;
        isCodeArray = true;
    } else {
        if (!currentEditingRole.permissions) currentEditingRole.permissions = [];
        targetArray = currentEditingRole.permissions;
    }

    const permIndex = targetArray.findIndex(p => (typeof p === 'string' ? p : p.code) === code);
    let isActive = false;
    if (permIndex > -1) {
        targetArray.splice(permIndex, 1);
        isActive = false;
    } else {
        if (isCodeArray) { targetArray.push(code); } else { targetArray.push({ code: code }); }
        isActive = true;
    }

    toggleElement.style.background = isActive ? '#23a559' : '#80848e';
    const circle = toggleElement.querySelector('div');
    circle.style.left = isActive ? '19px' : '3px';
    circle.innerHTML = isActive
        ? '<i class="fas fa-check" style="font-size: 10px; color: #23a559;"></i>'
        : '<i class="fas fa-times" style="font-size: 10px; color: #80848e;"></i>';
    toggleElement.classList.toggle('active', isActive);

    showRoleSaveBar();
}

document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('permission-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            if (currentEditingRole) renderRolePermissions(currentEditingRole);
        });
    }
});

window.createNewRoleFromEditor = async function () {
    const settingsLayout = document.querySelector('#serverSettingsModal .settings-layout:not(.roles-editor-layout)');
    const editorLayout = document.getElementById('roles-editor-view');

    if (editorLayout && editorLayout.style.display === 'none') {
        if (settingsLayout) settingsLayout.style.display = 'none';
        editorLayout.style.display = 'flex';

        if (state.editingServerId) {
            try {
                const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
                if (roles) currentEditorRoles = roles.sort((a, b) => b.priority - a.priority);
            } catch (e) { console.error(e); }
        }
    }

    const tempRole = { id: null, name: 'new role', color: '#99aab5', permissions: 0, priority: 0 };
    currentEditorRoles.unshift(tempRole);
    selectRoleInEditor(tempRole);
    setTimeout(() => {
        const input = document.getElementById('edit-role-name');
        if (input) { input.focus(); input.select(); }
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

    const roleData = { ...currentEditingRole, name: name, color: color, permissionCodes: permCodes };
    delete roleData.permissions;

    if (roleData.id === null) {
        Api.post(`/api/servers/${state.editingServerId}/roles`, roleData)
            .then(created => {
                currentEditorRoles.shift();
                currentEditorRoles.unshift(created);
                currentEditingRole = created;
                renderRoleEditorSidebar(currentEditorRoles, created.id);
                document.getElementById('role-save-bar').classList.remove('visible');
                Swal.fire({
                    icon: 'success', title: 'Đã tạo vai trò!', toast: true,
                    position: 'bottom-end', showConfirmButton: false, timer: 2000,
                    background: '#1e1f22', color: '#fff'
                });
            })
            .catch(e => Swal.fire('Lỗi', 'Tạo thất bại', 'error'));
    } else {
        Api.put(`/api/servers/${state.editingServerId}/roles/${roleData.id}`, roleData)
            .then(updated => {
                const idx = currentEditorRoles.findIndex(r => r.id === updated.id);
                if (idx !== -1) currentEditorRoles[idx] = updated;
                currentEditingRole = updated;
                renderRoleEditorSidebar(currentEditorRoles, updated.id);
                document.getElementById('role-save-bar').classList.remove('visible');
                Swal.fire({
                    icon: 'success', title: 'Đã lưu thay đổi!', toast: true,
                    position: 'bottom-end', showConfirmButton: false, timer: 2000,
                    background: '#1e1f22', color: '#fff'
                });
                renderRolesList(currentEditorRoles);
            })
            .catch(e => { console.error(e); Swal.fire('Lỗi', 'Lưu thất bại', 'error'); });
    }
}

window.resetRoleChanges = async function () {
    if (!currentEditingRole) return;
    const roleId = currentEditingRole.id;
    if (!roleId) {
        document.getElementById('edit-role-name').value = 'new role';
        document.getElementById('edit-role-color').value = '#99aab5';
        updateRoleColorPreview('#99aab5');
        if (currentEditingRole.permissionCodes) currentEditingRole.permissionCodes = [];
        const permTab = document.getElementById('role-tab-permissions');
        if (permTab && permTab.style.display !== 'none') { renderRolePermissions(currentEditingRole); }
        document.getElementById('role-save-bar').classList.remove('visible');
    } else {
        try {
            const roles = await Api.get(`/api/servers/${state.editingServerId}/roles`);
            const cleanRole = roles.find(r => r.id === roleId);
            if (cleanRole) {
                const idx = currentEditorRoles.findIndex(r => r.id === roleId);
                if (idx !== -1) currentEditorRoles[idx] = cleanRole;
                currentEditingRole = cleanRole;
                document.getElementById('edit-role-name').value = cleanRole.name;
                document.getElementById('edit-role-color').value = cleanRole.color || '#99aab5';
                updateRoleColorPreview(cleanRole.color || '#99aab5');
                const permTab = document.getElementById('role-tab-permissions');
                if (permTab && permTab.style.display !== 'none') { renderRolePermissions(cleanRole); }
                document.getElementById('role-save-bar').classList.remove('visible');
            }
        } catch (e) { console.error("Reset failed", e); }
    }
}

document.getElementById('edit-role-name').addEventListener('input', showRoleSaveBar);
document.getElementById('edit-role-color').addEventListener('input', showRoleSaveBar);

function showRoleSaveBar() {
    document.getElementById('role-save-bar').classList.add('visible');
}

window.switchRoleEditorTab = function (tabName) {
    const headers = document.querySelectorAll('.role-tab-item');
    headers.forEach(h => {
        if (h.getAttribute('onclick').includes(tabName)) {
            h.classList.add('active'); h.style.color = "white";
        } else {
            h.classList.remove('active'); h.style.color = "#b5bac1";
        }
    });

    const contents = document.querySelectorAll('.role-tab-content');
    contents.forEach(c => c.style.display = 'none');

    const activeContent = document.getElementById('role-tab-' + tabName);
    if (activeContent) {
        if (tabName === 'display' || tabName === 'permissions') { activeContent.style.display = 'flex'; }
        else { activeContent.style.display = 'block'; }

        if (tabName === 'permissions') { renderRolePermissions(currentEditingRole); }
        if (tabName === 'members') { loadRoleMembers(currentEditingRole.id); }
    }
}

// --- ADD MEMBER MODAL & LIST LOGIC ---
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
        icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33', cancelButtonColor: '#3085d6', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy', background: '#313338', color: '#dbdee1', zIndex: 10005
    });
    if (!result.isConfirmed) return;
    const remainingRoleIds = member.roles.filter(r => r.id !== currentEditingRole.id).map(r => r.id);
    try {
        await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, { userId: userId, roleIds: remainingRoleIds });
        currentRoleMembers = currentRoleMembers.filter(m => m.user.id !== userId);
        filterRoleMembers();
        loadServerMembers(state.editingServerId); // update main list
        Swal.fire({ title: 'Đã xóa!', icon: 'success', timer: 1000, showConfirmButton: false, background: '#313338', color: '#dbdee1' });
    } catch (e) { Swal.fire('Lỗi', 'Không thể gỡ vai trò: ' + e.message, 'error'); }
}

window.openAddRoleMemberModal = async function () {
    const modal = document.getElementById('add-role-member-modal');
    modal.style.display = 'flex';
    document.getElementById('add-role-member-list').innerHTML = '<div style="padding:10px; color:#b5bac1;">Đang tải...</div>';
    await loadServerMembers(state.editingServerId); // fresh data
    const currentRoleId = currentEditingRole.id;
    addMemberCandidates = state.membersCache.filter(m => {
        const hasRole = m.roles && m.roles.some(r => r.id === currentRoleId);
        return !hasRole;
    });
    selectedAddMemberIds.clear();
    renderAddMemberCandidates(addMemberCandidates);
}

window.closeAddRoleMemberModal = function () { document.getElementById('add-role-member-modal').style.display = 'none'; }

window.renderAddMemberCandidates = function (list) {
    const container = document.getElementById('add-role-member-list');
    container.innerHTML = '';
    if (list.length === 0) { container.innerHTML = '<div style="padding:10px; color:#b5bac1;">Không tìm thấy thành viên phù hợp.</div>'; return; }
    list.forEach(m => {
        const div = document.createElement('div');
        div.className = 'add-role-member-item';
        if (selectedAddMemberIds.has(m.user.id)) div.classList.add('selected');
        const avatarUrl = m.user.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png';
        const displayName = m.nickname || m.user.displayName || m.user.username;
        div.onclick = () => toggleAddRoleMember(m.user.id, div);
        div.innerHTML = `
            <div class="selection-circle">${selectedAddMemberIds.has(m.user.id) ? '<i class="fas fa-check" style="font-size:10px;"></i>' : ''}</div>
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
    const currentRoleId = currentEditingRole.id;
    let successCount = 0;
    for (const userId of selectedAddMemberIds) {
        const member = state.membersCache.find(m => m.user.id === userId);
        if (member) {
            const newRoleIds = member.roles.map(r => r.id);
            newRoleIds.push(currentRoleId);
            try {
                await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, { userId: userId, roleIds: newRoleIds });
                successCount++;
            } catch (e) { console.error(`Failed to add role for user ${userId}`, e); }
        }
    }
    if (successCount > 0) {
        Swal.fire({ icon: 'success', title: `Đã thêm ${successCount} thành viên`, toast: true, position: 'bottom-end', showConfirmButton: false, timer: 2000, background: '#1e1f22', color: '#fff' });
        loadRoleMembers(currentRoleId);
    }
    closeAddRoleMemberModal();
}

// Role Deletion & Context Menu
window.deleteCurrentRole = async function (optionalId) {
    const roleId = optionalId || (currentEditingRole ? currentEditingRole.id : null);
    if (!roleId) return;
    const result = await Swal.fire({ title: 'Xóa Vai Trò?', text: "Hành động này không thể hoàn tác.", icon: 'warning', showCancelButton: true, confirmButtonColor: '#da373c', cancelButtonColor: '#3085d6', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy', background: '#313338', color: '#dbdee1' });
    if (result.isConfirmed) {
        try {
            await Api.delete(`/api/servers/${state.editingServerId}/roles/${roleId}`);
            Swal.fire({ icon: 'success', title: 'Đã xóa!', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 1500, background: '#1e1f22', color: '#fff' });
            if (window.currentServerRoles) {
                window.currentServerRoles = window.currentServerRoles.filter(r => String(r.id) !== String(roleId));
                renderRoleEditorSidebar(window.currentServerRoles);
                renderRolesList(window.currentServerRoles);
                const countLabel = document.getElementById('roles-count-label');
                if (countLabel) countLabel.innerText = window.currentServerRoles.length;
            }
            if (currentEditingRole && currentEditingRole.id === roleId) {
                const first = window.currentServerRoles?.[0];
                if (first) { selectRoleInEditor(first); } else { currentEditingRole = null; }
            } else { if (currentEditingRole) loadRolesForEditor(currentEditingRole.id); }
            await loadServerRoles(state.editingServerId);
            if (window.currentServerRoles) { renderRoleEditorSidebar(window.currentServerRoles, currentEditingRole ? currentEditingRole.id : null); }
        } catch (e) { Swal.fire('Lỗi', 'Không thể xóa vai trò: ' + e.message, 'error'); }
    }
}

let actionMenuTargetRoleId = null;
window.showRoleActionMenu = function (e, roleId) {
    e.stopPropagation(); e.preventDefault();
    actionMenuTargetRoleId = roleId;
    const existing = document.getElementById('dynamic-role-menu');
    if (existing) existing.remove();
    const menu = document.createElement('div');
    menu.id = 'dynamic-role-menu';
    Object.assign(menu.style, { position: 'fixed', zIndex: '100000', minWidth: '220px', backgroundColor: '#111214', border: '1px solid #1e1f22', borderRadius: '4px', padding: '6px 8px', boxShadow: '0 8px 16px rgba(0,0,0,0.24)', display: 'block' });
    menu.innerHTML = `<div class="dynamic-menu-item" onclick="confirmDeleteRoleFromActionMenu()" onmouseover="this.style.backgroundColor='#da373c'; this.style.color='white'" onmouseout="this.style.backgroundColor='transparent'; this.style.color='#da373c'" style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-radius: 2px; cursor: pointer; color: #da373c; font-weight: 500; font-size: 14px; transition: all 0.1s;"><span>Xóa</span><i class="fas fa-trash-can"></i></div>`;
    document.body.appendChild(menu);
    let left = e.clientX - 210; let top = e.clientY + 10;
    const rect = { width: 220, height: 100 };
    if (left + rect.width > window.innerWidth) left = window.innerWidth - rect.width - 10;
    if (top + rect.height > window.innerHeight) top = window.innerHeight - rect.height - 10;
    if (left < 10) left = 10;
    menu.style.left = left + 'px'; menu.style.top = top + 'px';
}

window.closeRoleActionMenu = function () { const menu = document.getElementById('dynamic-role-menu'); if (menu) menu.remove(); }
window.confirmDeleteRoleFromActionMenu = function () { if (actionMenuTargetRoleId) { deleteCurrentRole(actionMenuTargetRoleId); } closeRoleActionMenu(); }

document.addEventListener('click', (e) => {
    if (!e.target.closest('#dynamic-role-menu') && !e.target.closest('.btn-role-action')) { closeRoleActionMenu(); }
});

// POPUP LOGIC FOR ASSIGNING ROLES
window.openRoleSelector = async function (event, userId, currentRolesJson) {
    event.stopPropagation(); closeRoleSelector();
    currentRoleSelectorUserId = userId;
    const currentRoles = JSON.parse(currentRolesJson);
    const existingRoleIds = new Set(currentRoles.map(r => r.id));
    let allRoles = [];
    try { allRoles = await Api.get(`/api/servers/${state.editingServerId}/roles`); } catch (e) { console.error("Lỗi fetch roles:", e); return; }
    const popover = document.createElement('div');
    popover.className = 'role-selector-popover visible';
    popover.style.left = (event.clientX) + 'px'; popover.style.top = (event.clientY + 20) + 'px'; popover.style.position = 'fixed';
    let html = '';
    allRoles.filter(r => r.name !== '@everyone').forEach(r => {
        const checked = existingRoleIds.has(r.id) ? 'checked' : '';
        html += `<label class="role-item"><input type="checkbox" value="${r.id}" ${checked} onchange="toggleRoleForUser(${userId}, ${r.id}, this.checked)"><div style="width: 10px; height: 10px; border-radius: 50%; background-color: ${r.color || '#99aab5'}; margin-right: 8px;"></div>${r.name}</label>`;
    });
    if (html === '') html = '<div style="padding: 8px; color: #b5bac1; font-size: 12px;">Không có vai trò nào</div>';
    popover.innerHTML = html;
    document.body.appendChild(popover);
}

window.closeRoleSelector = function () { const existing = document.querySelector('.role-selector-popover'); if (existing) existing.remove(); currentRoleSelectorUserId = null; }
document.addEventListener('click', (e) => { if (!e.target.closest('.role-selector-popover') && !e.target.closest('.btn-add-role')) { closeRoleSelector(); } });

window.toggleRoleForUser = async function (userId, roleId, isAdded) {
    const member = state.membersCache.find(m => m.user.id === userId);
    if (!member) return;
    let currentRoleIds = new Set(member.roles.map(r => r.id));
    if (isAdded) { currentRoleIds.add(roleId); } else { currentRoleIds.delete(roleId); }
    try {
        await Api.post(`/api/servers/${state.editingServerId}/roles/assign`, { userId: userId, roleIds: Array.from(currentRoleIds) });
        loadServerMembers(state.editingServerId);
    } catch (e) {
        console.error("Lỗi cập nhật role:", e); alert("Không thể cập nhật vai trò: " + e.message);
    }
}