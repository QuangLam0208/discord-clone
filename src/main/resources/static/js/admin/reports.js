const ReportsManager = {
    stompClient: null,
    subscription: null,

    init() {
        console.log("ReportsManager initialized");
        // Ensure elements exist before running logic
        if (!document.getElementById('reports-table-body')) return;

        this.loadReports();
        this.connectSocket();
    },

    connectSocket() {
        if (this.stompClient && this.stompClient.connected) {
            return;
        }

        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Disable debug logs

        this.stompClient.connect({}, (frame) => {
            console.log('[Reports] Connected to WebSocket');
            this.subscribeToReports();
        }, (error) => {
            console.error('[Reports] WebSocket Error:', error);
        });
    },

    subscribeToReports() {
        if (this.subscription) return;

        this.subscription = this.stompClient.subscribe('/topic/admin/reports', (message) => {
            const report = JSON.parse(message.body);

            // Show toast notification
            const Toast = Swal.mixin({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });
            Toast.fire({
                icon: 'info',
                title: 'New Report Received',
                text: `#${report.id} - ${report.type}`
            });

            // If we are on the reports page, prepend the new report
            if (document.getElementById('reports-table-body')) {
                this.prependReportRow(report);
            }
        });
    },

    async loadReports() {
        const tbody = document.getElementById('reports-table-body');
        const loading = document.getElementById('reports-loading');
        const empty = document.getElementById('reports-empty');

        if (!tbody) return;

        tbody.innerHTML = '';

        // FORCE HIDE LOADING (User request)
        if (loading) loading.style.display = 'none';

        // Hide empty initially
        if (empty) empty.style.display = 'none';

        try {
            const reports = await Api.get('/api/admin/reports');

            if (!reports || reports.length === 0) {
                if (empty) empty.style.display = 'block';
                return;
            }

            // Ensure empty is hidden if we have data
            if (empty) empty.style.display = 'none';

            reports.forEach(report => {
                this.appendReportRow(report, tbody);
            });

        } catch (error) {
            console.error("Load reports error detail:", error);
            if (loading) loading.style.display = 'none';
            if (empty) empty.style.display = 'none';

            Swal.fire({
                toast: true,
                position: 'top-end',
                icon: 'error',
                title: 'Error loading reports',
                showConfirmButton: false,
                background: '#313338',
                color: '#fff',
                timer: 3000
            });
        }
    },

    createReportRow(report) {
        // Determine actions column content based on status
        let actionsContent = '';
        if (report.status === 'PENDING') {
            actionsContent = `
                 <div class="btn-group">
                     <button class="btn btn-sm btn-success" title="Accept" onclick="ReportsManager.showProcessModal(${report.id}, '${report.targetUser ? report.targetUser.username : 'Unknown'}', '${report.type}')">
                         <i class="fa-solid fa-check"></i>
                     </button>
                     <button class="btn btn-sm btn-danger" title="Reject" onclick="ReportsManager.rejectReport(${report.id})">
                         <i class="fa-solid fa-xmark"></i>
                     </button>
                 </div>
             `;
        } else {
            actionsContent = `<span class="badge badge-secondary" style="opacity:0.7">Completed</span>`;
        }

        // Status badge styling
        let statusBadge = '';
        if (report.status === 'PENDING') statusBadge = '<span class="badge badge-warning">PENDING</span>';
        else if (report.status === 'ACCEPTED') statusBadge = '<span class="badge badge-success">ACCEPTED</span>';
        else if (report.status === 'REJECTED') statusBadge = '<span class="badge badge-danger">REJECTED</span>';
        else statusBadge = `<span class="badge badge-secondary">${report.status}</span>`;

        const tr = document.createElement('tr');
        tr.id = `report-row-${report.id}`;
        tr.className = 'fade-in-row';
        tr.innerHTML = `
             <td><span class="text-muted">#${report.id}</span></td>
             <td>
                 <div class="user-cell">
                     <div class="user-avatar-small" style="background-image: url('${report.reporter && report.reporter.avatarUrl ? report.reporter.avatarUrl : 'https://ui-avatars.com/api/?name=' + (report.reporter ? report.reporter.username : 'U')}')"></div>
                     <div class="user-info">
                        <span class="username">${report.reporter ? report.reporter.username : 'Unknown'}</span>
                     </div>
                 </div>
             </td>
             <td>
                 <div class="user-cell">
                    <div class="user-avatar-small" style="background-image: url('${report.targetUser && report.targetUser.avatarUrl ? report.targetUser.avatarUrl : 'https://ui-avatars.com/api/?name=' + (report.targetUser ? report.targetUser.username : 'U')}')"></div>
                     <div class="user-info">
                        <span class="username">${report.targetUser ? report.targetUser.username : 'Unknown'}</span>
                     </div>
                 </div>
             </td>
             <td>
                 <div class="report-type-badge">${report.type}</div>
             </td>
             <td>
                 ${statusBadge}
             </td>
             <td class="text-muted small">${new Date(report.createdAt).toLocaleString('vi-VN')}</td>
             <td>
                 ${actionsContent}
             </td>
         `;
        return tr;
    },

    appendReportRow(report, tbody) {
        const tr = this.createReportRow(report);
        tbody.appendChild(tr);
    },

    prependReportRow(report) {
        const tbody = document.getElementById('reports-table-body');
        const empty = document.getElementById('reports-empty');

        if (!tbody) return;

        if (empty && empty.style.display !== 'none') {
            empty.style.display = 'none';
        }

        // Remove existing if any (update case)
        const existing = document.getElementById(`report-row-${report.id}`);
        if (existing) existing.remove();

        const tr = this.createReportRow(report);
        tbody.prepend(tr);

        // Highlight effect
        tr.style.backgroundColor = 'rgba(88, 101, 242, 0.1)';
        setTimeout(() => tr.style.backgroundColor = '', 2000);
    },

    async rejectReport(reportId) {
        if (!await this.confirmAction('Reject this report?', 'This cannot be undone.')) return;

        try {
            await Api.post(`/api/admin/reports/${reportId}/process?accepted=false`);
            Swal.fire('Rejected', 'Report has been rejected', 'success');

            // Update UI directly without full reload
            this.updateRowStatus(reportId, 'REJECTED');
        } catch (error) {
            Swal.fire('Error', 'Failed to reject report', 'error');
        }
    },

    showProcessModal(reportId, targetUsername, reportType) {
        Swal.fire({
            title: 'Process Report',
            html: `
                <div class="text-left">
                    <p class="mb-3">Report against <strong>${targetUsername}</strong> for <strong>${reportType}</strong></p>
                    <label class="form-label">Select Penalty Action:</label>
                    <select id="swal-penalty" class="swal2-input mt-1">
                        <option value="WARN">Send Warning</option>
                        <option value="MUTE">Mute (Disable Chat)</option>
                        <option value="BAN">Ban (7 Days)</option>
                        <option value="GLOBAL_BAN">Global Ban (Permanent)</option>
                    </select>
                </div>
            `,
            showCancelButton: true,
            confirmButtonText: 'Apply Penalty',
            confirmButtonColor: '#d33',
            focusConfirm: false,
            preConfirm: () => {
                return document.getElementById('swal-penalty').value;
            }
        }).then(async (result) => {
            if (result.isConfirmed) {
                const penalty = result.value;
                try {
                    await Api.post(`/api/admin/reports/${reportId}/process?accepted=true&penalty=${penalty}`);
                    Swal.fire('Processed', 'Report accepted and penalty applied', 'success');

                    // Update UI directly
                    this.updateRowStatus(reportId, 'ACCEPTED');
                } catch (error) {
                    Swal.fire('Error', 'Failed to process report', 'error');
                }
            }
        });
    },

    updateRowStatus(reportId, newStatus) {
        const row = document.getElementById(`report-row-${reportId}`);
        if (!row) {
            this.loadReports(); // Fallback
            return;
        }

        // Update badge
        const badgeCell = row.children[4];
        if (newStatus === 'ACCEPTED') badgeCell.innerHTML = '<span class="badge badge-success">ACCEPTED</span>';
        else if (newStatus === 'REJECTED') badgeCell.innerHTML = '<span class="badge badge-danger">REJECTED</span>';

        // Update actions
        const actionCell = row.children[6];
        actionCell.innerHTML = `<span class="badge badge-secondary" style="opacity:0.7">Completed</span>`;
    },

    async confirmAction(title, text) {
        const result = await Swal.fire({
            title: title,
            text: text,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Yes, do it!'
        });
        return result.isConfirmed;
    }
};

// Export handler for boot.js
window.attachAdminReportsHandlers = () => ReportsManager.init();
