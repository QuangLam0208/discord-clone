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
        const idInput = document.getElementById('transferServerId');
        const nameInput = document.getElementById('transferServerName');
        const ownerInput = document.getElementById('newOwnerInput');
        if (idInput) idInput.value = id;
        if (nameInput) nameInput.value = name || '';
        if (ownerInput) { ownerInput.value = ''; ownerInput.focus(); }
    }

    function toggleStatusMenu(btn, serverId) {
        const menu = document.getElementById(`status-menu-${serverId}`);
        if (!menu) return;

        // Đóng các menu khác và reset style
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

        // Toggle open/close
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

        // Định vị bằng fixed để thoát clipping
        const rect = btn.getBoundingClientRect();
        const gap = 6;

        menu.style.position = 'fixed';
        menu.style.minWidth = `${Math.max(rect.width, 180)}px`;

        // Mặc định hiển thị dưới nút
        let top = rect.bottom + gap;
        let left = rect.left;

        // Tạm set để đo kích thước
        menu.style.top = `${top}px`;
        menu.style.left = `${left}px`;
        menu.style.right = 'auto';

        // Nếu tràn đáy viewport → drop-up
        const bottom = top + menu.offsetHeight;
        if (bottom > window.innerHeight) {
            const upTop = rect.top - menu.offsetHeight - gap;
            menu.style.top = `${Math.max(8, upTop)}px`;
            menu.classList.add('drop-up');
        } else {
            menu.classList.remove('drop-up');
        }

        // Nếu tràn bên phải → nẹp vào lề phải
        const overflowRight = left + menu.offsetWidth > window.innerWidth - 12;
        if (overflowRight) {
            const safeLeft = Math.max(12, window.innerWidth - menu.offsetWidth - 12);
            menu.style.left = `${safeLeft}px`;
        }

        // Auto-close khi scroll/resize/click ngoài
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
            if (status !== 'ACTIVE' && status !== 'FREEZE') return;

            await Api.patch(`/api/admin/servers/${serverId}/status`, { status });

            const cell = document.querySelector(`[data-server-status="${serverId}"]`);
            if (cell) {
                if (status === 'ACTIVE') cell.innerHTML = '<span class="badge-status active">Active</span>';
                else if (status === 'FREEZE') cell.innerHTML = '<span class="badge-status freeze">Freeze</span>';
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
    }

    // Expose cho template
    window.openDeleteModal = openDeleteModal;
    window.openTransferModal = openTransferModal;
    window.toggleStatusMenu = toggleStatusMenu;
    window.updateServerStatus = updateServerStatus;
    window.attachAdminServerHandlers = attachAdminServerHandlers;
})();