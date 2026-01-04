// Admin Messages: search, paginate, soft delete / restore
(function () {
    let curPage = 0;
    const pageSize = 20;
    let lastQuery = {};
    let stompClient = null;

    // --- 1. LOGIC WEBSOCKET ---
    function connectAdminMessagesSocket() {
        // Nếu đã kết nối rồi thì thôi
        if (stompClient && stompClient.connected) return;

        // Giả sử bạn đã import SockJS và Stomp trong layout chính
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        // Tắt debug log nếu muốn console sạch sẽ
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log('[Admin Messages] Connected to WebSocket');

            // A. Lắng nghe tin nhắn mới (User vừa gửi)
            stompClient.subscribe('/topic/admin/messages/create', function (payload) {
                const newMsg = JSON.parse(payload.body);
                handleNewMessageSocket(newMsg);
            });

            // B. Lắng nghe cập nhật trạng thái (Xóa/Restore)
            stompClient.subscribe('/topic/admin/messages/update', function (payload) {
                const data = JSON.parse(payload.body);
                handleUpdateMessageSocket(data);
            });
        }, function (error) {
            console.error('[Admin Messages] WebSocket Error:', error);
        });
    }

    // Xử lý khi có tin nhắn mới
    function handleNewMessageSocket(msg) {
        // Chỉ thêm vào nếu đang ở trang đầu tiên (Page 0)
        // Nếu admin đang xem trang 2, 3... thì không nên chèn vào gây nhảy trang
        if (curPage !== 0) return;

        const tbody = document.getElementById('admin-msg-tbody');
        if (!tbody) return;

        // Nếu đang hiển thị "Không có tin nhắn" hoặc "Đang tải", xóa nó đi
        const emptyRow = tbody.querySelector('.muted');
        if (emptyRow) tbody.innerHTML = '';

        // Tạo HTML dòng mới
        const html = rowHtml(msg);

        // Chèn vào đầu bảng (prepend) + Hiệu ứng nháy màu nhẹ để Admin nhận biết
        const tempDiv = document.createElement('tbody');
        tempDiv.innerHTML = html;
        const newRow = tempDiv.firstElementChild;

        newRow.style.backgroundColor = '#3ba55d33'; // Màu xanh nhạt
        newRow.style.transition = 'background-color 2s ease';

        tbody.prepend(newRow);

        // Sau 2s trả lại màu nền cũ
        setTimeout(() => {
            newRow.style.backgroundColor = '';
        }, 2000);

        // (Optional) Xóa dòng cuối cùng nếu quá số lượng pageSize để giữ UI gọn
        if (tbody.children.length > pageSize) {
            tbody.lastElementChild.remove();
        }
    }

    // Xử lý khi tin nhắn bị Xóa/Khôi phục
    function handleUpdateMessageSocket(data) {
        // data format: { id: "...", status: "DELETED" | "ACTIVE" }
        const row = document.getElementById('msg-row-' + data.id);
        if (!row) return; // Tin nhắn không nằm trong trang hiện tại

        // Cập nhật Badge Trạng thái (Cột thứ 7)
        const badgeCell = row.cells[6];
        if (data.status === 'DELETED') {
            badgeCell.innerHTML = '<span class="badge-status banned">Deleted</span>';
        } else {
            badgeCell.innerHTML = '<span class="badge-status active">Active</span>';
        }

        // Cập nhật Nút bấm (Cột thứ 8)
        const actionCell = row.querySelector('.action-buttons-wrapper');
        if (data.status === 'DELETED') {
            actionCell.innerHTML = `<button type="button" class="btn-mini edit" onclick="window.restoreAdminMessage('${escapeJsAttr(data.id)}')"><i class="fa-solid fa-rotate-left"></i> Restore</button>`;
        } else {
            actionCell.innerHTML = `<button type="button" class="btn-mini ban" onclick="window.deleteAdminMessage('${escapeJsAttr(data.id)}')"><i class="fa-solid fa-trash"></i> Delete</button>`;
        }
    }

    async function searchAdminMessages(resetPage = true) {
        if (resetPage) curPage = 0;

        const keyword = document.getElementById('msgKeyword')?.value?.trim();
        const status = document.getElementById('msgStatus')?.value || 'all';
        const serverId = asNumber(document.getElementById('msgServerId')?.value);
        const channelId = asNumber(document.getElementById('msgChannelId')?.value);
        const senderUsername = document.getElementById('msgSenderUsername')?.value?.trim();

        // Gửi raw 'yyyy-MM-ddTHH:mm' để backend parse linh hoạt
        const from = (document.getElementById('msgFrom')?.value || '').trim() || undefined;
        const to = (document.getElementById('msgTo')?.value || '').trim() || undefined;

        const query = { keyword, status, serverId, channelId, senderUsername, from, to, page: curPage, size: pageSize };
        lastQuery = query;

        const params = new URLSearchParams();
        Object.entries(query).forEach(([k, v]) => {
            // KHÔNG gửi null/undefined/chuỗi rỗng
            if (v !== undefined && v !== null && !(typeof v === 'string' && v.trim() === '')) {
                params.set(k, v);
            }
        });

        const tbody = document.getElementById('admin-msg-tbody');
        if (!tbody) return;
        tbody.innerHTML = `<tr><td colspan="8" class="muted" style="text-align:center;padding:20px;">Đang tải...</td></tr>`;

        try {
            const resp = await Api.get(`/api/admin/messages?${params.toString()}`);
            const page = (resp && typeof resp === 'object' && 'items' in resp) ? resp
                : (resp && typeof resp === 'object' && 'data' in resp) ? resp.data
                    : resp;
            renderAdminMessages(page);
        } catch (e) {
            const msg = e?.status === 403 ? 'Bạn không có quyền ADMIN' : (e.message || 'Error');
            tbody.innerHTML = `<tr><td colspan="8" class="muted" style="color:#ed4245;text-align:center;padding:20px;">Lỗi tải dữ liệu: ${escapeHtml(msg)}</td></tr>`;
            console.error('[admin messages] load error', e);
        }
    }

    function renderAdminMessages(page) {
        const items = Array.isArray(page?.items) ? page.items : [];
        const tbody = document.getElementById('admin-msg-tbody');

        if (!tbody) return;

        if (!items.length) {
            tbody.innerHTML = `<tr><td colspan="8" class="muted" style="text-align:center;padding:20px;">Không có tin nhắn</td></tr>`;
        } else {
            tbody.innerHTML = items.map(rowHtml).join('');
        }

        curPage = page?.page ?? 0;
        const totalPages = page?.totalPages ?? 1;
        const pager = document.getElementById('admin-msg-pager');
        if (pager) {
            pager.style.display = totalPages > 1 ? 'flex' : 'none';
            document.getElementById('admin-msg-page').innerText = curPage + 1;
            document.getElementById('admin-msg-total').innerText = totalPages;

            const prev = document.getElementById('admin-msg-prev');
            const next = document.getElementById('admin-msg-next');
            if (prev && next) {
                prev.style.pointerEvents = curPage > 0 ? 'auto' : 'none';
                prev.style.opacity = curPage > 0 ? '1' : '.5';
                next.style.pointerEvents = (curPage + 1) < totalPages ? 'auto' : 'none';
                next.style.opacity = (curPage + 1) < totalPages ? '1' : '.5';
            }
        }
    }

    function rowHtml(m) {
        const idShort = escapeHtml(m.id?.substring(0, 8) || '');
        const content = escapeHtml(m.content || '').slice(0, 120);
        const sender = escapeHtml(m.senderUsername || '-') + (m.senderDisplayName ? ` <span style="color:#b5bac1">(${escapeHtml(m.senderDisplayName)})</span>` : '');
        const channel = `${escapeHtml(m.channelName || '-')} <span style="color:#b5bac1">#${m.channelId ?? ''}</span>`;
        const server = `${escapeHtml(m.serverName || '-')} <span style="color:#b5bac1">#${m.serverId ?? ''}</span>`;
        const created = formatInstant(m.createdAt);
        const statusBadge = m.deleted
            ? '<span class="badge-status banned">Deleted</span>'
            : '<span class="badge-status active">Active</span>';

        const btn = m.deleted
            ? `<button type="button" class="btn-mini edit" onclick="window.restoreAdminMessage('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-rotate-left"></i> Restore</button>`
            : `<button type="button" class="btn-mini ban" onclick="window.deleteAdminMessage('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-trash"></i> Delete</button>`;

        return `
      <tr id="msg-row-${m.id}">
        <td style="font-family:monospace;color:#b5bac1">#${idShort}</td>
        <td>${content}</td>
        <td>${sender}</td>
        <td>${channel}</td>
        <td>${server}</td>
        <td>${created}</td>
        <td>${statusBadge}</td>
        <td class="action-buttons-wrapper">${btn}</td>
      </tr>
    `;
    }

    function formatInstant(isoVal) {
        if (!isoVal) return '-';
        try { return new Date(isoVal).toLocaleString(); }
        catch { return String(isoVal); } }

    function asNumber(v) {
        if (v === undefined || v === null) return undefined;
        const s = String(v).trim();
        if (s === '') return undefined; // KHÔNG trả 0 khi input trống
        const n = Number(s);
        return Number.isFinite(n) ? n : undefined;
    }

    async function deleteAdminMessage(id) {
        const ok = await Swal.fire({
            title: 'Xóa tin nhắn?',
            text: 'Xóa mềm: tin nhắn sẽ bị ẩn khỏi người dùng.',
            icon: 'warning', showCancelButton: true, confirmButtonText: 'Xóa', cancelButtonText: 'Hủy',
            confirmButtonColor: '#ed4245', background: '#313338', color: '#fff'
        }).then(r => r.isConfirmed);
        if (!ok) return;
        try {
            await Api.patch(`/api/admin/messages/${id}/delete`, {});
            toastOk('Đã xóa tin nhắn');
            // searchAdminMessages(false);
        } catch (e) { toastErr(e.message || 'Xóa tin nhắn thất bại'); }
    }

    async function restoreAdminMessage(id) {
        const ok = await Swal.fire({
            title: 'Khôi phục tin nhắn?',
            icon: 'question', showCancelButton: true, confirmButtonText: 'Khôi phục', cancelButtonText: 'Hủy',
            confirmButtonColor: '#5865F2', background: '#313338', color: '#fff'
        }).then(r => r.isConfirmed);
        if (!ok) return;
        try {
            await Api.patch(`/api/admin/messages/${id}/restore`, {});
            toastOk('Đã khôi phục tin nhắn');
            //searchAdminMessages(false);
        } catch (e) { toastErr(e.message || 'Khôi phục thất bại'); }
    }

    function attachAdminMessageHandlers() {
        if (document.getElementById('admin-msg-tbody')) {
            searchAdminMessages(true);
            connectAdminMessagesSocket();
        }
    }

    // Expose
    window.searchAdminMessages = searchAdminMessages;
    window.pageAdminMessages = function (delta) {
        const next = curPage + delta;
        if (next < 0) return false;
        curPage = next;
        searchAdminMessages(false);
        return false;
    };
    window.deleteAdminMessage = deleteAdminMessage;
    window.restoreAdminMessage = restoreAdminMessage;
    window.attachAdminMessageHandlers = attachAdminMessageHandlers;
})();