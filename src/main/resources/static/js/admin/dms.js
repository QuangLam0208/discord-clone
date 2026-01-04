// Admin Direct Messages: search, paginate, soft delete / restore
(function () {
    let curPage = 0;
    const pageSize = 20;

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
            tbody.innerHTML = `<tr><td colspan="7" class="muted" style="text-align:center;color:#ed4245;padding:20px;">${escapeHtml(e.message||'Error')}</td></tr>`;
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

        tbody.innerHTML = items.map(m => {
            const idShort = escapeHtml(String(m.id||'').slice(0,8));
            const content = escapeHtml(m.content || '').slice(0, 160);
            const participants = `${escapeHtml(m.userAUsername || String(m.userAId||'-'))} ↔ ${escapeHtml(m.userBUsername || String(m.userBId||'-'))}`;
            const sr = `${escapeHtml(m.senderUsername || String(m.senderId||'-'))} → ${escapeHtml(m.receiverUsername || String(m.receiverId||'-'))}`;
            const created = m.createdAt ? new Date(m.createdAt).toLocaleString() : '-';
            const statusBadge = m.deleted ? '<span class="badge-status banned">Deleted</span>' : '<span class="badge-status active">Active</span>';
            const btn = m.deleted
                ? `<button type="button" class="btn-mini edit" onclick="window.restoreAdminDM('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-rotate-left"></i> Restore</button>`
                : `<button type="button" class="btn-mini ban" onclick="window.deleteAdminDM('${escapeJsAttr(m.id)}')"><i class="fa-solid fa-trash"></i> Delete</button>`;
            return `
        <tr>
          <td style="font-family:monospace;color:#b5bac1">#${idShort}</td>
          <td>${content}</td>
          <td>${sr}</td>
          <td>${participants}</td>
          <td>${created}</td>
          <td>${statusBadge}</td>
          <td class="action-buttons-wrapper">${btn}</td>
        </tr>
      `;
        }).join('');

        curPage = page?.page ?? 0;
        const totalPages = page?.totalPages ?? 1;
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

    // Helpers
    function txt(id){ const v = document.getElementById(id)?.value || ''; return v.trim() || undefined; }
    function val(id){ return document.getElementById(id)?.value; }
    function num(id){ const s = (document.getElementById(id)?.value || '').trim(); if(!s) return undefined; const n = Number(s); return Number.isFinite(n)?n:undefined; }
    function dt(id){ const s = (document.getElementById(id)?.value || '').trim(); return s || undefined; }
    function set(p,k,v){ if(v!==undefined && v!==null && !(typeof v==='string' && v.trim()==='')) p.set(k, v); }

    async function deleteAdminDM(id) {
        const ok = await Swal.fire({ title:'Xóa DM?', text:'Xóa mềm: tin nhắn sẽ bị ẩn khỏi người dùng.', icon:'warning', showCancelButton:true, confirmButtonText:'Xóa', cancelButtonText:'Hủy', confirmButtonColor:'#ed4245', background:'#313338', color:'#fff' }).then(r=>r.isConfirmed);
        if (!ok) return;
        try { await Api.patch(`/api/admin/direct-messages/${id}/delete`, {}); toastOk('Đã xóa DM'); searchAdminDM(false); }
        catch (e) { toastErr(e.message||'Xóa DM thất bại'); }
    }
    async function restoreAdminDM(id) {
        const ok = await Swal.fire({ title:'Khôi phục DM?', icon:'question', showCancelButton:true, confirmButtonText:'Khôi phục', cancelButtonText:'Hủy', confirmButtonColor:'#5865F2', background:'#313338', color:'#fff' }).then(r=>r.isConfirmed);
        if (!ok) return;
        try { await Api.patch(`/api/admin/direct-messages/${id}/restore`, {}); toastOk('Đã khôi phục DM'); searchAdminDM(false); }
        catch (e) { toastErr(e.message||'Khôi phục DM thất bại'); }
    }

    function attachAdminDMHandlers() {
        if (document.getElementById('admin-dm-tbody')) {
            searchAdminDM(true);
        }
    }

    window.searchAdminDM = searchAdminDM;
    window.pageAdminDM = function(delta){ const next = curPage + delta; if(next<0) return false; curPage = next; searchAdminDM(false); return false; };
    window.deleteAdminDM = deleteAdminDM;
    window.restoreAdminDM = restoreAdminDM;
    window.attachAdminDMHandlers = attachAdminDMHandlers;
})();