// ====== Servers (delete/transfer/status dropdown) ======
(function () {

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
        document.getElementById('transferServerId').value = id;

        // Đưa tên server lên giữa header
        const titleNameEl = document.getElementById('transferServerNameTitle');
        if (titleNameEl) titleNameEl.innerText = name || '';

        // Lấy owner hiện tại từ cell trong bảng để fallback nếu API chưa set isOwner
        const ownerCell = document.querySelector(`[data-server-owner="${id}"]`);
        let currentOwnerUsername = (ownerCell?.innerText || '').trim();
        if (!currentOwnerUsername || /^no owner$/i.test(currentOwnerUsername)) currentOwnerUsername = '';

        // Lưu vào dataset modal
        modal.dataset.currentOwnerUsername = currentOwnerUsername;

        // Reset lựa chọn và disable nút xác nhận
        modal.dataset.selectedMemberId = '';
        const btnConfirm = document.getElementById('btnConfirmTransfer');
        if (btnConfirm) btnConfirm.disabled = true;

        // Clear và load danh sách
        const list = document.getElementById('transferMemberList');
        list.innerHTML = '<div class="muted" style="padding:12px;">Đang tải danh sách thành viên...</div>';

        loadTransferMembers(id);
    }

    async function loadTransferMembers(serverId) {
        try {
            const members = await Api.get(`/api/admin/servers/${serverId}/members`);
            renderTransferMemberList(members || []);
        } catch (e) {
            document.getElementById('transferMemberList').innerHTML =
                `<div class="muted" style="padding:12px;color:#ed4245">Không tải được danh sách: ${escapeHtml(e.message || 'Error')}</div>`;
        }
    }

    function renderTransferMemberList(members) {
        const list = document.getElementById('transferMemberList');
        const modal = document.getElementById('transferModal');
        const currentOwnerUsername = (modal?.dataset?.currentOwnerUsername || '').toLowerCase();

        if (!Array.isArray(members) || !members.length) {
            list.innerHTML = '<div class="muted" style="padding:12px;">Không có thành viên</div>';
            return;
        }

        list.innerHTML = members.map(m => {
            const username = String(m.username || '').trim();
            const isOwnerApi = !!m.isOwner; // từ API nếu có
            const isOwnerByName = currentOwnerUsername && username.toLowerCase() === currentOwnerUsername;
            const isOwner = isOwnerApi || isOwnerByName;

            const disabledAttr = isOwner ? 'aria-disabled="true" title="Owner hiện tại - không thể chọn"' : '';
            const ownerChip = isOwner ? '<span class="owner-chip">Owner hiện tại</span>' : '';
            const adminBadge = m.isAdminRole ? '<span class="badge-role admin" style="font-size:10px;">ADMIN</span>' : '';

            return `
        <div class="transfer-member-row"
             data-member-id="${m.memberId}"
             data-username="${escapeHtml(username)}"
             data-display="${escapeHtml(m.displayName || '')}"
             data-owner="${isOwner ? '1' : '0'}"
             ${disabledAttr}
             style="display:flex;align-items:center;gap:10px;padding:8px;cursor:pointer;border-bottom:1px solid #2b2d31;">
          <img src="${escapeHtml(m.avatarUrl || '/images/default.png')}" style="width:28px;height:28px;border-radius:50%;object-fit:cover;">
          <div style="flex:1;">
            <div style="color:#fff;font-weight:600;display:flex;align-items:center;gap:6px;">
              ${escapeHtml(username)}
              ${ownerChip}
            </div>
            <div style="color:#b5bac1;font-size:12px;">${escapeHtml(m.displayName || '')}</div>
          </div>
          ${adminBadge}
        </div>
      `;
        }).join('');

        // Bind selection (không cho chọn owner hiện tại)
        list.querySelectorAll('.transfer-member-row').forEach(row => {
            row.addEventListener('click', () => {
                const isOwner = row.getAttribute('data-owner') === '1' || row.getAttribute('aria-disabled') === 'true';
                if (isOwner) {
                    toastErr('Đây là owner hiện tại của server');
                    return;
                }

                // Visual select
                list.querySelectorAll('.transfer-member-row').forEach(r => r.style.background = '');
                row.style.background = '#35373c';

                // Save selected + enable nút xác nhận
                const modal = document.getElementById('transferModal');
                modal.dataset.selectedMemberId = row.getAttribute('data-member-id');

                const btnConfirm = document.getElementById('btnConfirmTransfer');
                if (btnConfirm) btnConfirm.disabled = false;
            });
        });
    }

    function filterTransferMemberList() {
        const keyword = (document.getElementById('transferMemberSearch').value || '').trim().toLowerCase();
        const list = document.getElementById('transferMemberList');
        list.querySelectorAll('.transfer-member-row').forEach(row => {
            const u = (row.getAttribute('data-username') || '').toLowerCase();
            const d = (row.getAttribute('data-display') || '').toLowerCase();
            const show = !keyword || u.includes(keyword) || d.includes(keyword);
            row.style.display = show ? 'flex' : 'none';
        });
    }

    function toggleStatusMenu(btn, serverId) {
        const menu = document.getElementById(`status-menu-${serverId}`);
        if (!menu) return;

        document.querySelectorAll('.status-menu.open').forEach(el => {
            if (el !== menu) {
                el.classList.remove('open', 'drop-up');
                el.style.position = '';
                el.style.top = '';
                el.style.left = '';
                el.style.right = '';
                el.style.minWidth = '';
            }
        });

        const willOpen = !menu.classList.contains('open');
        if (!willOpen) {
            menu.classList.remove('open', 'drop-up');
            menu.style.position = '';
            menu.style.top = '';
            menu.style.left = '';
            menu.style.right = '';
            menu.style.minWidth = '';
            return;
        }
        menu.classList.add('open');

        const rect = btn.getBoundingClientRect();
        const gap = 6;

        menu.style.position = 'fixed';
        menu.style.minWidth = `${Math.max(rect.width, 180)}px`;

        let top = rect.bottom + gap;
        let left = rect.left;

        menu.style.top = `${top}px`;
        menu.style.left = `${left}px`;
        menu.style.right = 'auto';

        const bottom = top + menu.offsetHeight;
        if (bottom > window.innerHeight) {
            const upTop = rect.top - menu.offsetHeight - gap;
            menu.style.top = `${Math.max(8, upTop)}px`;
            menu.classList.add('drop-up');
        } else {
            menu.classList.remove('drop-up');
        }

        const overflowRight = left + menu.offsetWidth > window.innerWidth - 12;
        if (overflowRight) {
            const safeLeft = Math.max(12, window.innerWidth - menu.offsetWidth - 12);
            menu.style.left = `${safeLeft}px`;
        }

        const closeOnOutside = (e) => {
            if (!e.target.closest('.status-menu') && !e.target.closest('.btn-mini.warn')) {
                menu.classList.remove('open', 'drop-up');
                menu.style.position = '';
                menu.style.top = '';
                menu.style.left = '';
                menu.style.right = '';
                menu.style.minWidth = '';
                window.removeEventListener('scroll', closeOnOutside, true);
                window.removeEventListener('resize', closeOnOutside, true);
                document.removeEventListener('click', closeOnOutside, true);
            }
        };
        window.addEventListener('scroll', closeOnOutside, true);
        window.addEventListener('resize', closeOnOutside, true);
        document.addEventListener('click', closeOnOutside, true);
    }

    async function updateServerStatus(serverId, status) {
        try {
            if (status !== 'ACTIVE' && status !== 'FREEZE' && status !== 'SHADOW') return;

            await Api.patch(`/api/admin/servers/${serverId}/status`, { status });

            const cell = document.querySelector(`[data-server-status="${serverId}"]`);
            if (cell) {
                if (status === 'ACTIVE') cell.innerHTML = '<span class="badge-status active">Active</span>';
                else if (status === 'FREEZE') cell.innerHTML = '<span class="badge-status freeze">Freeze</span>';
                else if (status === 'SHADOW') cell.innerHTML = '<span class="badge-status shadow" style="background:#555;color:#bbb">Shadow</span>';
            }

            const menu = document.getElementById(`status-menu-${serverId}`);
            if (menu) {
                menu.classList.remove('open', 'drop-up');
                menu.style.position = '';
                menu.style.top = '';
                menu.style.left = '';
                menu.style.right = '';
                menu.style.minWidth = '';
            }

            toastOk('Đã cập nhật trạng thái server');
        } catch (e) {
            toastErr(e.message || 'Cập nhật trạng thái thất bại');
        }
    }

    function attachAdminServerHandlers() {
        // Xóa server
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

        // Chuyển owner
        const btnConfirmTransfer = document.getElementById('btnConfirmTransfer');
        btnConfirmTransfer?.addEventListener('click', async () => {
            const serverId = document.getElementById('transferServerId')?.value;
            const modal = document.getElementById('transferModal');
            const memberId = modal?.dataset?.selectedMemberId;
            if (!serverId) { toastErr('Thiếu serverId'); return; }
            if (!memberId) { toastErr('Vui lòng chọn một thành viên để chuyển'); return; }
            try {
                await Api.post(`/api/admin/servers/${serverId}/transfer-owner`, { newOwnerMemberId: Number(memberId) });
                closeModal('transferModal');

                // Cập nhật hiển thị owner
                const list = document.getElementById('transferMemberList');
                const selected = list?.querySelector(`.transfer-member-row[data-member-id="${memberId}"]`);
                const newOwnerUsername = selected?.getAttribute('data-username') || '';
                const ownerCell = document.querySelector(`[data-server-owner="${serverId}"]`);
                if (ownerCell) ownerCell.innerText = newOwnerUsername;

                toastOk('Đã chuyển quyền owner');
            } catch (e) { toastErr(e.message || 'Chuyển owner thất bại'); }
        });
    }

    // ====== Server Detail Modal ======
    async function openServerDetailModal(serverId) {
        const modal = document.getElementById('detailServerModal');
        if (!modal) return;

        // Reset UI
        document.getElementById('detailServerName').innerText = 'Loading...';
        document.getElementById('detailServerId').innerText = `ID: #${serverId}`;
        document.getElementById('detailServerDesc').innerText = '...';
        document.getElementById('detailServerOwner').innerText = '...';
        document.getElementById('detailMemberCount').innerText = '-';
        document.getElementById('detailChannelCount').innerText = '-';
        document.getElementById('detailRoleCount').innerText = '-';
        document.getElementById('detailMembersList').innerHTML = '';
        document.getElementById('detailChannelsList').innerHTML = '';
        document.getElementById('detailRolesList').innerHTML = '';
        document.getElementById('detailServerStatus').className = 'badge-status';
        document.getElementById('detailServerStatus').innerText = '...';

        modal.classList.add('active');

        try {
            const data = await Api.get(`/api/admin/servers/${serverId}`);
            if (!data) throw new Error("No data");

            // Fill header
            document.getElementById('detailServerName').innerText = data.name || 'Unnamed';
            document.getElementById('detailServerIcon').src = data.iconUrl || '/images/default.png';
            document.getElementById('detailServerDesc').innerText = data.description || 'Không có mô tả';

            // Status
            const statusEl = document.getElementById('detailServerStatus');
            if (data.status === 'ACTIVE') {
                statusEl.className = 'badge-status active';
                statusEl.innerText = 'ACTIVE';
            } else if (data.status === 'FREEZE') {
                statusEl.className = 'badge-status freeze';
                statusEl.innerText = 'FREEZE';
            } else if (data.status === 'SHADOW') {
                statusEl.className = 'badge-status shadow';
                statusEl.innerText = 'SHADOW';
                statusEl.style.background = '#555';
                statusEl.style.color = '#bbb';
            } else {
                statusEl.className = 'badge-status';
                statusEl.innerText = data.status;
            }

            // Owner
            document.getElementById('detailServerOwner').innerText = data.ownerUsername || 'No Owner';

            // Counts
            document.getElementById('detailMemberCount').innerText = data.memberCount || 0;
            document.getElementById('detailChannelCount').innerText = data.channelCount || 0;
            document.getElementById('detailRoleCount').innerText = data.roleCount || 0;

            // Lists
            const membersHtml = (data.memberNames || []).map(n => `<div style="padding:2px 0;">${escapeHtml(n)}</div>`).join('');
            document.getElementById('detailMembersList').innerHTML = membersHtml || '<div class="muted">Empty</div>';

            const channelsHtml = (data.channelNames || []).map(n => `<div style="padding:2px 0;"># ${escapeHtml(n)}</div>`).join('');
            document.getElementById('detailChannelsList').innerHTML = channelsHtml || '<div class="muted">Empty</div>';

            const rolesHtml = (data.roleNames || []).map(n => `<div style="padding:2px 0;">@ ${escapeHtml(n)}</div>`).join('');
            document.getElementById('detailRolesList').innerHTML = rolesHtml || '<div class="muted">Empty</div>';

        } catch (e) {
            console.error(e);
            toastErr('Không lấy được thông tin server');
            closeModal('detailServerModal');
        }
    }

    // Expose cho template
    window.openDeleteModal = openDeleteModal;
    window.openTransferModal = openTransferModal;
    window.toggleStatusMenu = toggleStatusMenu;
    window.updateServerStatus = updateServerStatus;
    window.attachAdminServerHandlers = attachAdminServerHandlers;
    window.filterTransferMemberList = filterTransferMemberList;
    window.openServerDetailModal = openServerDetailModal;
})();