// ====== Audit Log Realtime ======
(function () {
    let stompClient = null;

    function connectWs() {
        if (stompClient) return; // already connected
        console.log('[Audit] Connecting to WS...');
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        // stompClient.debug = null; // Enable debug for now

        stompClient.connect({}, function (frame) {
            console.log('[Audit] Connected!');
            updateWsIndicator(true);

            // Subscribe audit
            stompClient.subscribe('/topic/admin/audit', function (msg) {
                console.log('[Audit] Received message:', msg.body);
                const body = JSON.parse(msg.body);
                onAuditReceived(body);
            });
        }, function (err) {
            console.error('[Audit] WS Error:', err);
            updateWsIndicator(false);
        });
    }

    function updateWsIndicator(ok) {
        const ind = document.getElementById('ws-indicator');
        if (ind) {
            ind.style.backgroundColor = ok ? '#23a559' : '#ed4245';
            ind.title = ok ? 'Connected' : 'Disconnected';
        }
    }

    function onAuditReceived(log) {
        // Use flexible selector or ID
        const tableBody = document.getElementById('admin-audit-tbody')
            || document.querySelector('.discord-table tbody');

        if (!tableBody) {
            console.log('[Audit] No table body found. Ignoring.');
            return;
        }

        // Double check it's the right table by checking headers if using class selector
        if (!tableBody.id) {
            const headerRow = document.querySelector('.discord-table thead tr');
            if (!headerRow || headerRow.children.length !== 6) return;
        }

        console.log('[Audit] Rendering row for:', log);

        // Remove "No data" row if exists
        const noData = tableBody.querySelector('.muted');
        if (noData && (noData.innerText.includes('Chưa có dữ liệu') || noData.innerText.includes('No data'))) {
            noData.parentElement.remove();
        }

        const tr = document.createElement('tr');
        tr.className = 'new-row-flash';

        const role = log.role || 'DEFAULT';
        let roleClass = 'role-default';
        if (role === 'ADMIN') roleClass = 'role-admin';
        else if (role === 'PREMIUM') roleClass = 'role-premium';

        tr.innerHTML = `
            <td style="font-size: 0.9em; color: #aaa;">${window.formatInstant ? window.formatInstant(log.createdAt) : log.createdAt}</td>
            <td>${log.adminId || '-'}</td>
            <td>${window.escapeHtml(log.username || '-')}</td>
            <td>
                <span class="${roleClass}" 
                      style="font-weight:bold; font-size: 0.8em; padding: 2px 6px; border-radius: 4px; background: #333;">
                    ${window.escapeHtml(role)}
                </span>
            </td>
            <td style="color: #4CAF50;">${window.escapeHtml(log.action || '')}</td>
            <td style="white-space: pre-wrap; word-break: break-all;">${window.escapeHtml(log.detail || '')}</td>
        `;

        // Prepend
        tableBody.insertBefore(tr, tableBody.firstChild);
    }

    function attachAdminAuditHandlers() {
        console.log('[Audit] Handlers attached');
        connectWs();
    }

    window.attachAdminAuditHandlers = attachAdminAuditHandlers;
    connectWs();
})();
