// Admin Direct Messages: search, paginate, soft delete / restore
(function () {
    let curPage = 0;
    let totalPages = 1;
    const pageSize = 20;
    let stompClient = null;

    function connectAdminDmsSocket() {
        if (stompClient && stompClient.connected) return;

        // Đảm bảo SockJS đã được load ở layout (index.html)
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Tắt log debug cho gọn

        stompClient.connect({}, function () {
            console.log('[Admin DMs] Connected to WebSocket');

            // A. Nghe tin nhắn DM mới
            stompClient.subscribe('/topic/admin/dms/create', function (payload) {
                // Chỉ thêm vào bảng nếu đang ở trang 1 (trang mới nhất)
                if (curPage !== 0) return;

                const newMsg = JSON.parse(payload.body);
                prependDmRow(newMsg);
            });

            // B. Nghe thay đổi trạng thái (Delete/Restore)
            stompClient.subscribe('/topic/admin/dms/update', function (payload) {
                const data = JSON.parse(payload.body);
                updateDmRowStatus(data);
            });
        }, function (err) {
            console.error('[Admin DMs] WebSocket Error:', err);
        });
    }

    function prependDmRow(msg) {
        const tbody = document.getElementById('admin-dm-tbody');
        if (!tbody) return;

        // Xóa dòng "Không có DM" nếu có
        const empty = tbody.querySelector('.muted');
        if (empty) tbody.innerHTML = '';

        // Tạo HTML dòng mới
        const html = rowHtml(msg);
        const temp = document.createElement('tbody');
        temp.innerHTML = html;
        const newRow = temp.firstElementChild;

        // Hiệu ứng màu xanh nhạt báo hiệu tin mới
        newRow.style.backgroundColor = '#3ba55d33';
        newRow.style.transition = 'background-color 2s ease';

        tbody.prepend(newRow);

        setTimeout(() => newRow.style.backgroundColor = '', 2000);

        // Giới hạn số dòng hiển thị để không quá dài
        if (tbody.children.length > pageSize) {
            tbody.lastElementChild.remove();
        }
    }

    // Cập nhật trạng thái dòng hiện tại
    function updateDmRowStatus(data) {
        const row = document.getElementById('dm-row-' + data.id);
        if (!row) return; // Dòng không nằm trong trang hiện tại

        // EDIT
        if (data.type === 'EDIT') {
            const contentCell = row.cells[1];
            // Cập nhật nội dung + nhãn (edited)
            contentCell.innerHTML = escapeHtml(data.content) + ' <span style="color:#b5bac1; font-size:0.85em;">(edited)</span>';

            // Hiệu ứng nháy vàng
            row.style.backgroundColor = '#faa61a33';
            setTimeout(() => row.style.backgroundColor = '', 1500);
            return;
        }
        // Cập nhật Badge (Cột 6)
        const badgeCell = row.cells[5];
        if (data.status === 'DELETED') {
            badgeCell.innerHTML = '<span class="badge-status banned">Deleted</span>';
        } else {
            badgeCell.innerHTML = '<span class="badge-status active">Active</span>';
        }

        // Cập nhật Nút bấm (Cột 7)
        const actionCell = row.querySelector('.action-buttons-wrapper');
        if (data.status === 'DELETED') {
            actionCell.innerHTML = `<button type="button" class="btn-mini edit" onclick="window.restoreAdminDM('${escapeJsAttr(data.id)}')"><i class="fa-solid fa-rotate-left"></i> Restore</button>`;
        } else {
            actionCell.innerHTML = `<button type="button" class="btn-mini ban" onclick="window.deleteAdminDM('${escapeJsAttr(data.id)}')"><i class="fa-solid fa-trash"></i> Delete</button>`;
        }
    }

    async function searchAdminDM(reset = true) {
        if (reset) curPage = 0;

        const status = val('dmStatus') || 'all';
        const conversationId = txt('dmConversationId');
        const userAId = num('dmUserA');
        const userBId = num('dmUserB');
        const userId = num('dmUserId');
        const senderUsername = txt('dmSenderUsername');
        const keyword = txt('dmKeyword');
        const from = dt('dmFrom');
        const to = dt('dmTo');

        const params = new URLSearchParams();
        set(params, 'status', status);
        set(params, 'conversationId', conversationId);
        set(params, 'userAId', userAId);
        set(params, 'userBId', userBId);
        set(params, 'userId', userId);
        set(params, 'senderUsername', senderUsername);
        set(params, 'keyword', keyword);
        set(params, 'from', from);
        set(params, 'to', to);
        params.set('page', curPage);
        params.set('size', pageSize);

        const tbody = document.getElementById('admin-dm-tbody');
        if (!tbody) return;
        tbody.innerHTML = `<tr><td colspan="7" class="muted" style="text-align:center;padding:20px;">Đang tải...</td></tr>`;

        try {
            const page = await Api.get(`/api/admin/direct-messages?${params.toString()}`);
            render(page);
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="7" class="muted" style="text-align:center;color:#ed4245;padding:20px;">${escapeHtml(e.message || 'Error')}</td></tr>`;
            console.error('[admin direct messages] load error', e);
        }
    }

    function render(page) {
        const items = Array.isArray(page?.items) ? page.items : [];
        const tbody = document.getElementById('admin-dm-tbody');
        if (!tbody) return;

        if (!items.length) {
            tbody.innerHTML = `<tr><td colspan="7" class="muted" style="text-align:center;padding:20px;">Không có DM</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(rowHtml).join('');

        curPage = page?.page ?? 0;
        totalPages = page?.totalPages ?? 1; // Update global totalPages
        const pager = document.getElementById('admin-dm-pager');
        if (pager) {
            pager.style.display = totalPages > 1 ? 'flex' : 'none';
            document.getElementById('admin-dm-page').innerText = curPage + 1;
            document.getElementById('admin-dm-total').innerText = totalPages;

            const prev = document.getElementById('admin-dm-prev');
            const next = document.getElementById('admin-dm-next');
            if (prev && next) {
                prev.style.pointerEvents = curPage > 0 ? 'auto' : 'none';
                prev.style.opacity = curPage > 0 ? '1' : '.5';
                next.style.pointerEvents = (curPage + 1) < totalPages ? 'auto' : 'none';
                next.style.opacity = (curPage + 1) < totalPages ? '1' : '.5';
            }
        }
    }

    function rowHtml(m) {
        const idShort = escapeHtml(String(m.id || '').slice(0, 8));
        const content = escapeHtml(m.content || '').slice(0, 160);
        const editedLabel = m.edited ? ' <span style="color:#b5bac1; font-size:0.85em;">(edited)</span>' : '';
        const participants = `${escapeHtml(m.userAUsername || String(m.userAId || '-'))} ↔ ${escapeHtml(m.userBUsername || String(m.userBId || '-'))}`;
        const sr = `${escapeHtml(m.senderUsername || String(m.senderId || '-'))} → ${escapeHtml(m.receiverUsername || String(m.receiverId || '-'))}`;
        const created = m.createdAt ? new Date(m.createdAt).toLocaleString() : '-';

        // Status Badge
        const statusBadge = m.deleted
            ? '<span class="badge-status banned">Deleted</span>'
            : '<span class="badge-status active">Active</span>';

        // Action Button
        const btn = m.deleted
            ? `<button type="button" class="btn-mini edit" onclick="window.restoreAdminDM('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-rotate-left"></i> Restore</button>`
            : `<button type="button" class="btn-mini ban" onclick="window.deleteAdminDM('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-trash"></i> Delete</button>`;

        return `
        <tr id="dm-row-${m.id}">
          <td style="font-family:monospace;color:#b5bac1">#${idShort}</td>
          <td>${content}${editedLabel}</td>
          <td>${sr}</td>
          <td>${participants}</td>
          <td>${created}</td>
          <td>${statusBadge}</td>
          <td class="action-buttons-wrapper">${btn}</td>
        </tr>
      `;
    }

    // Helpers
    function txt(id) { const v = document.getElementById(id)?.value || ''; return v.trim() || undefined; }
    function val(id) { return document.getElementById(id)?.value; }
    function num(id) { const s = (document.getElementById(id)?.value || '').trim(); if (!s) return undefined; const n = Number(s); return Number.isFinite(n) ? n : undefined; }
    function dt(id) { const s = (document.getElementById(id)?.value || '').trim(); return s || undefined; }
    function set(p, k, v) { if (v !== undefined && v !== null && !(typeof v === 'string' && v.trim() === '')) p.set(k, v); }

    async function deleteAdminDM(id) {
        const ok = await Swal.fire({ title: 'Xóa DM?', text: 'Xóa mềm: tin nhắn sẽ bị ẩn khỏi người dùng.', icon: 'warning', showCancelButton: true, confirmButtonText: 'Xóa', cancelButtonText: 'Hủy', confirmButtonColor: '#ed4245', background: '#313338', color: '#fff' }).then(r => r.isConfirmed);
        if (!ok) return;
        try {
            await Api.patch(`/api/admin/direct-messages/${id}/delete`, {});
            toastOk('Đã xóa DM');
            // searchAdminDM(false); // Không cần reload, socket sẽ tự update
        }
        catch (e) { toastErr(e.message || 'Xóa DM thất bại'); }
    }

    async function restoreAdminDM(id) {
        const ok = await Swal.fire({ title: 'Khôi phục DM?', icon: 'question', showCancelButton: true, confirmButtonText: 'Khôi phục', cancelButtonText: 'Hủy', confirmButtonColor: '#5865F2', background: '#313338', color: '#fff' }).then(r => r.isConfirmed);
        if (!ok) return;
        try {
            await Api.patch(`/api/admin/direct-messages/${id}/restore`, {});
            toastOk('Đã khôi phục DM');
            // searchAdminDM(false); // Không cần reload
        }
        catch (e) { toastErr(e.message || 'Khôi phục DM thất bại'); }
    }

    function attachAdminDMHandlers() {
        if (document.getElementById('admin-dm-tbody')) {
            searchAdminDM(true);
            connectAdminDmsSocket(); // Kích hoạt lắng nghe socket
        }
    }

    window.searchAdminDM = searchAdminDM;
    window.pageAdminDM = function (delta) {
        const next = curPage + delta;
        if (next < 0) return false;
        curPage = next;
        searchAdminDM(false);
        return false;
    };
    window.deleteAdminDM = deleteAdminDM;
    window.restoreAdminDM = restoreAdminDM;
    window.attachAdminDMHandlers = attachAdminDMHandlers;
})();
