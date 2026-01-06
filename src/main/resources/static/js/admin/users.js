// ====== Users (ban/unban/edit) ======
(function () {

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

    // ====== Edit user modal enable save on change ======
    function normalizeRoles(arr) {
        return Array.from(new Set((arr || []).map(x => String(x).trim()))).sort();
    }
    function isEqualArray(a, b) {
        const aa = normalizeRoles(a), bb = normalizeRoles(b);
        if (aa.length !== bb.length) return false;
        for (let i = 0; i < aa.length; i++) { if (aa[i] !== bb[i]) return false; }
        return true;
    }
    function computeCurrentRoles() {
        return Array.from(document.querySelectorAll('input[name="roleNames"]:checked')).map(cb => cb.value);
    }
    function parseOriginalRolesStr(str) {
        return normalizeRoles(String(str || '').split(',').map(s => s.trim()).filter(Boolean));
    }
    function updateEditSaveEnabled() {
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

        const idInput = document.getElementById('editUserId');
        const usernameInput = document.getElementById('editUsername');
        const displayNameHidden = document.getElementById('displayName-' + userId);
        const displayNameInput = document.getElementById('editDisplayName');
        if (idInput) idInput.value = userId;
        if (usernameInput) usernameInput.value = username || '';
        if (displayNameHidden && displayNameInput) displayNameInput.value = displayNameHidden.value || '';

        const checkboxes = document.querySelectorAll('input[name="roleNames"]');
        checkboxes.forEach(cb => cb.checked = false);
        const rolesDiv = document.getElementById('roles-' + userId);
        const rolesStr = rolesDiv ? rolesDiv.innerText : '';
        const origRoles = parseOriginalRolesStr(rolesStr);
        checkboxes.forEach(cb => { if (origRoles.includes(cb.value)) cb.checked = true; });

        const btn = document.getElementById('btnEditSave');
        const modalEl = document.getElementById('editModal');
        if (modalEl) {
            modalEl.dataset.origDn = displayNameInput?.value || '';
            modalEl.dataset.origRoles = rolesStr || '';
        }
        if (btn) btn.disabled = true;

        displayNameInput?.addEventListener('input', updateEditSaveEnabled, { once: false });
        checkboxes.forEach(cb => cb.addEventListener('change', updateEditSaveEnabled, { once: false }));
    }

    async function openUserDetail(userId) {
        try {
            const resp = await Api.get(`/api/admin/users/${userId}/detail`);
            // Hỗ trợ cả envelope {data: {...}} hoặc trả trực tiếp {...}
            const data = (resp && typeof resp === 'object' && 'data' in resp) ? resp.data : resp;

            if (!data || typeof data !== 'object') {
                throw new Error('Dữ liệu chi tiết user không hợp lệ');
            }

            // Gán giá trị với fallback an toàn
            setText('ud-username', safeStr(data.username));
            setText('ud-displayName', safeStr(data.displayName));
            setText('ud-birthDate', formatDate(data.birthDate));
            setText('ud-createdAt', formatInstant(data.createdAt));
            setText('ud-lastActive', formatInstant(data.lastActive));

            const listEl = document.getElementById('ud-servers');
            const servers = Array.isArray(data.servers) ? data.servers : [];
            listEl.innerHTML = servers.length ? servers.map(serverCardHtml).join('') : '<div class="muted">Không tham gia server nào</div>';

            // Mở modal
            const modal = document.getElementById('userDetailModal');
            modal?.classList.add('active');

        } catch (e) {
            console.error('Open user detail error:', e);
            toastErr(e.message || 'Không tải được chi tiết user');
        }
    }

    function setText(id, text) {
        const el = document.getElementById(id);
        if (el) el.innerText = text ?? '-';
    }
    function safeStr(v) {
        if (v === null || v === undefined) return '-';
        return String(v);
    }

    function formatDate(dateVal) {
        if (!dateVal) return '-';
        try {
            // Hỗ trợ kiểu ISO string hoặc LocalDate (yyyy-MM-dd)
            const d = typeof dateVal === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateVal)
                ? new Date(dateVal + 'T00:00:00Z')
                : new Date(dateVal);
            return d.toLocaleDateString();
        } catch { return safeStr(dateVal); }
    }
    function formatInstant(isoVal) {
        if (!isoVal) return '-';
        try {
            const d = new Date(isoVal);
            return d.toLocaleString();
        } catch { return safeStr(isoVal); }
    }

    function openUnmuteModal(userId, username) {
        const modal = document.getElementById('unmuteModal'); if (!modal) return;
        modal.classList.add('active');
        const idInput = document.getElementById('unmuteUserId');
        const nameLabel = document.getElementById('unmuteUsername');
        if (idInput) idInput.value = userId;
        if (nameLabel) nameLabel.innerText = username || '';
    }

    function serverCardHtml(s) {
        const status = s?.status === 'FREEZE'
            ? '<span class="badge-status freeze">Freeze</span>'
            : '<span class="badge-status active">Active</span>';
        const iconStyle = s?.iconUrl ? `style="background-image:url('${escapeHtml(s.iconUrl)}')"` : '';
        const owner = safeStr(s?.ownerUsername || 'N/A');
        const name = escapeHtml(s?.name || '');
        const channels = Number.isFinite(s?.channelCount) ? s.channelCount : 0;
        const members = Number.isFinite(s?.memberCount) ? s.memberCount : 0;

        return `
      <div class="server-card">
        <div class="icon" ${iconStyle}></div>
        <div>
          <div style="color:#fff;font-weight:600;">${name}</div>
          <div class="server-meta">
            <span class="badge">Owner: ${escapeHtml(owner)}</span>
            <span class="badge">Channels: ${channels}</span>
            <span class="badge">Members: ${members}</span>
            ${status}
          </div>
        </div>
      </div>
    `;
    }

    function attachAdminUserHandlers() {
        // Users: Ban
        const btnBanConfirm = document.getElementById('btnBanConfirm');
        btnBanConfirm?.addEventListener('click', async () => {
            const id = document.getElementById('banUserId')?.value;
            if (!id) return;
            try {
                await Api.post(`/api/admin/users/${id}/ban`, {});
                closeModal('banModal');

                // Reload section to reflect changes perfectly or manually update DOM
                // Reloading is safer for complex state updates
                const cur = document.querySelector('.admin-pagination-link.active') || { dataset: { page: 0, keyword: '' } };
                loadSection('users', { page: cur.dataset.page || 0, keyword: cur.dataset.keyword || '' });

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

                const cur = document.querySelector('.admin-pagination-link.active') || { dataset: { page: 0, keyword: '' } };
                loadSection('users', { page: cur.dataset.page || 0, keyword: cur.dataset.keyword || '' });

                toastOk('Đã mở khóa tài khoản');
            } catch (e) { toastErr(e.message || 'Mở khóa thất bại'); }
        });

        // Users: Unmute
        const btnUnmuteConfirm = document.getElementById('btnUnmuteConfirm');
        btnUnmuteConfirm?.addEventListener('click', async () => {
            const id = document.getElementById('unmuteUserId')?.value;
            if (!id) return;
            try {
                await Api.post(`/api/admin/users/${id}/unmute`, {});
                closeModal('unmuteModal');

                // Reload to refresh status
                const cur = document.querySelector('.admin-pagination-link.active') || { dataset: { page: 0, keyword: '' } };
                loadSection('users', { page: cur.dataset.page || 0, keyword: cur.dataset.keyword || '' });

                toastOk('Đã bỏ cấm chat (Unmute)');
            } catch (e) { toastErr(e.message || 'Unmute thất bại'); }
        });

        // Users: Save edit (displayName + roles)
        const btnEditSave = document.getElementById('btnEditSave');
        btnEditSave?.addEventListener('click', async () => {
            if (btnEditSave.disabled) return;
            const id = document.getElementById('editUserId')?.value;
            const displayName = document.getElementById('editDisplayName')?.value?.trim();
            const roles = Array.from(document.querySelectorAll('input[name="roleNames"]:checked')).map(cb => cb.value);
            if (!id) return;
            try {
                await Api.patch(`/api/admin/users/${id}`, { displayName, roles });

                const displayCell = document.querySelector(`[data-user-display="${id}"]`);
                if (displayCell) displayCell.innerText = displayName || '';

                const hiddenDisplay = document.getElementById('displayName-' + id);
                if (hiddenDisplay) hiddenDisplay.value = displayName || '';

                const rolesCell = document.querySelector(`[data-user-roles-cell="${id}"]`);
                if (rolesCell) {
                    const set = new Set(roles.map(String));
                    let label = 'DEFAULT', cls = 'default';
                    if (set.has('ADMIN')) { label = 'ADMIN'; cls = 'admin'; }
                    else if (set.has('USER_PREMIUM')) { label = 'PREMIUM'; cls = 'premium'; }
                    else if (set.has('USER_DEFAULT')) { label = 'DEFAULT'; cls = 'default'; }
                    rolesCell.innerHTML = `<span class="badge-role ${cls}">${label}</span>`;
                }

                closeModal('editModal');
                toastOk('Đã cập nhật người dùng');
            } catch (e) {
                toastErr(e.message || 'Cập nhật người dùng thất bại');
            }
        });

        // User Detail: gắn cho nút/username có data-user-id
        document.querySelectorAll('[data-user-id]').forEach(el => {
            if (el.dataset.boundDetail === '1') return;
            el.dataset.boundDetail = '1';
            el.addEventListener('click', () => {
                const uid = el.getAttribute('data-user-id');
                if (uid) openUserDetail(uid);
            });
        });
    }

    // Expose cho template
    window.openBanModal = openBanModal;
    window.openUnbanModal = openUnbanModal;
    window.openUnmuteModal = openUnmuteModal;
    window.openEditModal = openEditModal;
    window.openUserDetail = openUserDetail;
    window.attachAdminUserHandlers = attachAdminUserHandlers;
})();