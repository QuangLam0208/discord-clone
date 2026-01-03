
// ====== Helpers ======
function closeModal(id){ const m=document.getElementById(id); if(m) m.classList.remove('active'); }
function openModal(id){ const m=document.getElementById(id); if(m) m.classList.add('active'); }
function toastOk(t='Thành công'){ if(window.Swal){ return Swal.fire({icon:'success',title:t,timer:1200,showConfirmButton:false,background:'#313338',color:'#fff'});} }
function toastErr(t='Có lỗi xảy ra'){ if(window.Swal){ return Swal.fire({icon:'error',title:'Thất bại',text:t,background:'#313338',color:'#fff'});} }
function escapeHtml(s){ return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

// Đóng modal khi click overlay / ESC
window.addEventListener('click', e=>{ if(e.target.classList && e.target.classList.contains('modal-overlay')) e.target.classList.remove('active'); });
window.addEventListener('keydown', e=>{ if(e.key==='Escape'){ document.querySelectorAll('.modal-overlay.active').forEach(el=>el.classList.remove('active')); } });

// ====== SPA load fragments ======
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

    // Loading
    content.innerHTML = `<div class="muted" style="padding:20px;">Đang tải ${labelMap[section]||section}...</div>`;

    try {
        const res = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
        if (!res.ok) throw new Error(`Load thất bại: ${res.status}`);
        const html = await res.text();
        content.innerHTML = html;

        // Sau khi inject fragment, gắn handlers cho các nút trong fragment
        attachAdminHandlers();
    } catch (e) {
        console.error('Load section error', e);
        content.innerHTML = `<div class="muted" style="padding:20px;color:#ed4245">Không tải được nội dung (${e.message})</div>`;
    }
}

// Gắn click cho sidebar
document.addEventListener('DOMContentLoaded', () => {
    const nav = document.getElementById('admin-nav');
    nav?.addEventListener('click', (e) => {
        const a = e.target.closest('a.nav-item[data-section]');
        if (!a) return;
        e.preventDefault();
        const section = a.dataset.section;
        loadSection(section);
        history.replaceState({}, '', `/admin#${section}`);
    });

    // Avatar/name
    const displayName = localStorage.getItem('displayName') || localStorage.getItem('username') || 'Admin';
    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&color=fff&size=128&bold=true`;
    const avatarEl = document.getElementById('admin-avatar'); if (avatarEl) avatarEl.style.backgroundImage = `url('${avatarUrl}')`;
    const nameEl = document.getElementById('admin-name'); if (nameEl) nameEl.innerText = displayName;

    // Logout
    const btnLogout = document.getElementById('btn-admin-logout');
    btnLogout?.addEventListener('click', () => {
        Swal.fire({ title:'Đăng xuất?', icon:'question', showCancelButton:true, confirmButtonText:'Đăng xuất', cancelButtonText:'Hủy', confirmButtonColor:'#ed4245', background:'#313338', color:'#fff' })
            .then(r => { if (r.isConfirmed) { localStorage.clear(); window.location.href = '/login'; }});
    });

    // Load section từ hash nếu có
    const hashSection = (location.hash || '').replace('#','') || 'dashboard';
    loadSection(hashSection);
});

// ====== OPEN MODALS (Users/Servers) ======
function openDeleteModal(id, name) {
    const modal = document.getElementById('deleteModal'); if (!modal) return;
    modal.classList.add('active');
    const idInput = document.getElementById('deleteServerId');
    const nameLabel = document.getElementById('deleteServerName');
    if (idInput) idInput.value = id;
    if (nameLabel) nameLabel.innerText = name || '';
}
function openTransferModal(id, name) {
    const modal = document.getElementById('transferModal'); if (!modal) return;
    modal.classList.add('active');
    const idInput = document.getElementById('transferServerId');
    const nameInput = document.getElementById('transferServerName');
    const ownerInput = document.getElementById('newOwnerInput');
    if (idInput) idInput.value = id;
    if (nameInput) nameInput.value = name || '';
    if (ownerInput) { ownerInput.value = ''; ownerInput.focus(); }
}
function openBanModal(userId, username) {
    const modal = document.getElementById('banModal'); if (!modal) return;
    modal.classList.add('active');
    const idInput = document.getElementById('banUserId');
    const nameLabel = document.getElementById('banUsername');
    if (idInput) idInput.value = userId;
    if (nameLabel) nameLabel.innerText = username || '';
}
function openUnbanModal(userId, username) {
    const modal = document.getElementById('unbanModal'); if (!modal) return;
    modal.classList.add('active');
    const idInput = document.getElementById('unbanUserId');
    const nameLabel = document.getElementById('unbanUsername');
    if (idInput) idInput.value = userId;
    if (nameLabel) nameLabel.innerText = username || '';
}

// ====== Only enable "Lưu thay đổi" when form actually changed ======
function normalizeRoles(arr){
    return Array.from(new Set((arr||[]).map(x => String(x).trim()))).sort();
}
function isEqualArray(a,b){
    const aa = normalizeRoles(a), bb = normalizeRoles(b);
    if (aa.length !== bb.length) return false;
    for (let i=0;i<aa.length;i++){ if (aa[i] !== bb[i]) return false; }
    return true;
}
function computeCurrentRoles(){
    return Array.from(document.querySelectorAll('input[name="roleNames"]:checked')).map(cb => cb.value);
}
function parseOriginalRolesStr(str){
    // roles div chứa chuỗi kiểu "ADMIN, USER_DEFAULT," → tách theo ","
    return normalizeRoles(String(str||'').split(',').map(s => s.trim()).filter(Boolean));
}
function updateEditSaveEnabled(){
    const btn = document.getElementById('btnEditSave');
    if (!btn) return;
    const modal = document.getElementById('editModal');
    const origDn = (modal?.dataset?.origDn || '').trim();
    const origRoles = parseOriginalRolesStr(modal?.dataset?.origRoles || '');

    const curDn = (document.getElementById('editDisplayName')?.value || '').trim();
    const curRoles = computeCurrentRoles();

    const dnChanged = origDn !== curDn;
    const rolesChanged = !isEqualArray(origRoles, curRoles);

    const changed = dnChanged || rolesChanged;
    btn.disabled = !changed;
}

function openEditModal(userId, username) {
    const modal = document.getElementById('editModal'); if (!modal) return;
    modal.classList.add('active');

    // Điền dữ liệu hiện tại
    const idInput = document.getElementById('editUserId');
    const usernameInput = document.getElementById('editUsername');
    const displayNameHidden = document.getElementById('displayName-' + userId);
    const displayNameInput = document.getElementById('editDisplayName');
    if (idInput) idInput.value = userId;
    if (usernameInput) usernameInput.value = username || '';
    if (displayNameHidden && displayNameInput) displayNameInput.value = displayNameHidden.value || '';

    // Reset và tick roles
    const checkboxes = document.querySelectorAll('input[name="roleNames"]');
    checkboxes.forEach(cb => cb.checked = false);
    const rolesDiv = document.getElementById('roles-' + userId);
    const rolesStr = rolesDiv ? rolesDiv.innerText : '';
    const origRoles = parseOriginalRolesStr(rolesStr);
    checkboxes.forEach(cb => { if (origRoles.includes(cb.value)) cb.checked = true; });

    // Lưu trạng thái gốc vào dataset của modal
    modal.dataset.origDn = displayNameInput?.value || '';
    modal.dataset.origRoles = rolesStr || '';

    // Disable nút Lưu khi chưa đổi gì
    const btn = document.getElementById('btnEditSave');
    if (btn) btn.disabled = true;

    // Lắng nghe thay đổi để bật/tắt nút
    displayNameInput?.addEventListener('input', updateEditSaveEnabled, { once:false });
    checkboxes.forEach(cb => cb.addEventListener('change', updateEditSaveEnabled, { once:false }));
}

// Expose để gọi từ HTML
window.openDeleteModal = openDeleteModal;
window.openTransferModal = openTransferModal;
window.openBanModal = openBanModal;
window.openUnbanModal = openUnbanModal;
window.openEditModal = openEditModal;

// Escape cho string nhúng vào HTML/JS inline
function escapeJsAttr(s){
    return String(s||'').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

// ====== CONFIRM HANDLERS (gắn sau khi fragment được inject) ======
function attachAdminHandlers() {
    // Servers: Xóa
    const btnConfirmDelete = document.getElementById('btnConfirmDelete');
    btnConfirmDelete?.addEventListener('click', async () => {
        const id = document.getElementById('deleteServerId')?.value;
        if (!id) return;
        try {
            const ok = await Api.delete(`/api/admin/servers/${id}`);
            if (!ok) throw new Error('Xóa server thất bại');
            closeModal('deleteModal');
            const row = document.querySelector(`[data-server-row="${id}"]`);
            row?.remove();
            toastOk('Đã xóa server');
        } catch (e) { toastErr(e.message || 'Xóa server thất bại'); }
    });

    // Servers: Chuyển owner
    const btnConfirmTransfer = document.getElementById('btnConfirmTransfer');
    btnConfirmTransfer?.addEventListener('click', async () => {
        const id = document.getElementById('transferServerId')?.value;
        const newOwnerUsername = document.getElementById('newOwnerInput')?.value?.trim();
        if (!id || !newOwnerUsername) { toastErr('Vui lòng nhập username owner mới'); return; }
        try {
            await Api.post(`/api/admin/servers/${id}/transfer-owner`, { newOwnerUsername });
            closeModal('transferModal');
            const ownerCell = document.querySelector(`[data-server-owner="${id}"]`);
            if (ownerCell) ownerCell.innerText = newOwnerUsername;
            toastOk('Đã chuyển quyền owner');
        } catch (e) { toastErr(e.message || 'Chuyển owner thất bại'); }
    });

    // Users: Ban
    const btnBanConfirm = document.getElementById('btnBanConfirm');
    btnBanConfirm?.addEventListener('click', async () => {
        const id = document.getElementById('banUserId')?.value;
        if (!id) return;
        try {
            await Api.post(`/api/admin/users/${id}/ban`, {});
            closeModal('banModal');

            // Cập nhật badge trạng thái
            const st = document.querySelector(`[data-user-status="${id}"]`);
            if (st) st.innerHTML = '<span class="badge-status banned">Banned</span>';

            // Cập nhật nút hành động: chuyển Ban -> Unban
            const actions = document.querySelector(`[data-user-actions="${id}"]`);
            const username = actions?.dataset?.userUsername || '';
            if (actions) {
                actions.innerHTML = `
          <button type="button" class="btn-mini edit"
                  data-id="${id}" data-username="${escapeJsAttr(username)}"
                  onclick="openEditModal(${id}, '${escapeJsAttr(username)}')">
            <i class="fas fa-pen"></i> Sửa
          </button>
          <button type="button" class="btn-mini edit"
                  data-id="${id}" data-username="${escapeJsAttr(username)}"
                  onclick="openUnbanModal(${id}, '${escapeJsAttr(username)}')">
            <i class="fas fa-unlock"></i> Unban
          </button>
        `;
            }

            toastOk('Đã khóa tài khoản');
        } catch (e) { toastErr(e.message || 'Khóa tài khoản thất bại'); }
    });

    // Users: Unban
    const btnUnbanConfirm = document.getElementById('btnUnbanConfirm');
    btnUnbanConfirm?.addEventListener('click', async () => {
        const id = document.getElementById('unbanUserId')?.value;
        if (!id) return;
        try {
            await Api.post(`/api/admin/users/${id}/unban`, {});
            closeModal('unbanModal');

            // Cập nhật badge trạng thái
            const st = document.querySelector(`[data-user-status="${id}"]`);
            if (st) st.innerHTML = '<span class="badge-status active">Active</span>';

            // Cập nhật nút hành động: chuyển Unban -> Ban
            const actions = document.querySelector(`[data-user-actions="${id}"]`);
            const username = actions?.dataset?.userUsername || '';
            if (actions) {
                actions.innerHTML = `
          <button type="button" class="btn-mini edit"
                  data-id="${id}" data-username="${escapeJsAttr(username)}"
                  onclick="openEditModal(${id}, '${escapeJsAttr(username)}')">
            <i class="fas fa-pen"></i> Sửa
          </button>
          <button type="button" class="btn-mini ban"
                  data-id="${id}" data-username="${escapeJsAttr(username)}"
                  onclick="openBanModal(${id}, '${escapeJsAttr(username)}')">
            <i class="fas fa-gavel"></i> Ban
          </button>
        `;
            }

            toastOk('Đã mở khóa tài khoản');
        } catch (e) { toastErr(e.message || 'Mở khóa thất bại'); }
    });

    // Users: Lưu roles + displayName (chỉ khi có thay đổi)
    const btnEditSave = document.getElementById('btnEditSave');
    btnEditSave?.addEventListener('click', async () => {
        if (btnEditSave.disabled) return; // không làm gì nếu chưa thay đổi

        const id = document.getElementById('editUserId')?.value;
        const displayName = document.getElementById('editDisplayName')?.value?.trim();
        const roles = Array.from(document.querySelectorAll('input[name="roleNames"]:checked')).map(cb => cb.value);
        if (!id) return;

        try {
            // Gọi PATCH để lưu cả displayName & roles
            await Api.patch(`/api/admin/users/${id}`, { displayName, roles });

            // Cập nhật UI: cột "Tên hiển thị"
            const displayCell = document.querySelector(`[data-user-display="${id}"]`);
            if (displayCell) displayCell.innerText = displayName || '';

            // Cập nhật input ẩn để modal lần sau lấy đúng giá trị
            const hiddenDisplay = document.getElementById('displayName-' + id);
            if (hiddenDisplay) hiddenDisplay.value = displayName || '';

            // Cập nhật vai trò hệ thống (ADMIN/PREMIUM/DEFAULT)
            const rolesCell = document.querySelector(`[data-user-roles-cell="${id}"]`);
            if (rolesCell) {
                const set = new Set(roles.map(String));
                let label='DEFAULT', cls='default';
                if (set.has('ADMIN')) { label='ADMIN'; cls='admin'; }
                else if (set.has('USER_PREMIUM')) { label='PREMIUM'; cls='premium'; }
                else if (set.has('USER_DEFAULT')) { label='DEFAULT'; cls='default'; }
                rolesCell.innerHTML = `<span class="badge-role ${cls}">${label}</span>`;
            }

            closeModal('editModal');
            toastOk('Đã cập nhật người dùng');
        } catch (e) {
            toastErr(e.message || 'Cập nhật người dùng thất bại');
        }
    });
}