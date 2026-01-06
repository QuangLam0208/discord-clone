const ReportsManager = {
    init() {
        console.log("ReportsManager initialized");
        this.loadReports();
        this.connectSocket();
    },

    connectSocket() {
        if (window.stompClient && window.stompClient.connected) {
            this.subscribeToReports();
        } else {
            document.addEventListener('stompConnected', () => this.subscribeToReports());
        }
    },

    subscribeToReports() {
        if (this.subscription) return; // already subscribed
        this.subscription = window.stompClient.subscribe('/topic/admin/reports', (message) => {
            // Reload list when new report comes or status changes (simplest approach)
            // Or parse message to prepend/remove row
            this.loadReports();

            // Show notification
            const report = JSON.parse(message.body);
            // Example: Toast notification
            // Swal.fire({ toast: true, position: 'top-end', icon: 'info', title: `New Report update: #${report.id}`, showConfirmButton: false, timer: 3000 });
        });
    },

    async loadReports() {
        const tbody = document.getElementById('reports-table-body');
        const loading = document.getElementById('reports-loading');
        const empty = document.getElementById('reports-empty');

        if (!tbody) return;

        tbody.innerHTML = '';
        loading.classList.remove('hidden');
        empty.classList.add('hidden');

        try {
            // Fetch all reports (no status filter)
            const reports = await Api.get('/api/admin/reports');
            loading.classList.add('hidden');

            if (!reports || reports.length === 0) {
                empty.classList.remove('hidden');
                return;
            }

            reports.forEach(report => {
                const tr = document.createElement('tr');

                // Determine actions column content based on status
                let actionsContent = '';
                if (report.status === 'PENDING') {
                    actionsContent = `
                        <div class="btn-group">
                            <button class="btn btn-sm btn-success" onclick="ReportsManager.showProcessModal(${report.id}, '${report.targetUser ? report.targetUser.username : 'Unknown'}', '${report.type}')">
                                <i class="fa-solid fa-check"></i> Accept
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="ReportsManager.rejectReport(${report.id})">
                                <i class="fa-solid fa-xmark"></i> Reject
                            </button>
                        </div>
                    `;
                } else {
                    actionsContent = `<span class="text-muted">Completed</span>`;
                }

                // Status badge styling
                let statusBadge = '';
                if (report.status === 'PENDING') statusBadge = '<span class="badge badge-warning">PENDING</span>';
                else if (report.status === 'ACCEPTED') statusBadge = '<span class="badge badge-success">ACCEPTED</span>';
                else if (report.status === 'REJECTED') statusBadge = '<span class="badge badge-danger">REJECTED</span>';
                else statusBadge = `<span class="badge badge-secondary">${report.status}</span>`;

                tr.innerHTML = `
                    <td>#${report.id}</td>
                    <td>
                        <div class="user-cell">
                            <span class="username">${report.reporter ? report.reporter.username : 'Unknown'}</span>
                        </div>
                    </td>
                    <td>
                        <div class="user-cell">
                             <span class="username">${report.targetUser ? report.targetUser.username : 'Unknown'}</span>
                        </div>
                    </td>
                    <td>
                        <span class="badge badge-info">${report.type}</span>
                    </td>
                    <td>
                        ${statusBadge}
                    </td>
                    <td>${new Date(report.createdAt).toLocaleString('vi-VN')}</td>
                    <td>
                        ${actionsContent}
                    </td>
                `;
                tbody.appendChild(tr);
            });

        } catch (error) {
            console.error("Load reports error detail:", error);
            const msg = error.userMessage || error.message || 'Unknown error';
            Swal.fire('Error', 'Failed to load reports: ' + msg, 'error');
            loading.classList.add('hidden');
        }
    },

    async rejectReport(reportId) {
        if (!confirm('Are you sure you want to REJECT this report?')) return;

        try {
            await Api.post(`/api/admin/reports/${reportId}/process?accepted=false`);
            Swal.fire('Rejected', 'Report has been rejected', 'success');
            this.loadReports();
        } catch (error) {
            Swal.fire('Error', 'Failed to reject report', 'error');
        }
    },

    showProcessModal(reportId, targetUsername, reportType) {
        Swal.fire({
            title: 'Process Report',
            html: `
                <p>Report against <strong>${targetUsername}</strong> for <strong>${reportType}</strong></p>
                <div class="form-group" style="text-align:left; margin-top:15px;">
                    <label>Select Penalty:</label>
                    <select id="swal-penalty" class="swal2-select" style="display:block; width:100%; margin:10px 0;">
                        <option value="WARN">WARN (Send Warning)</option>
                        <option value="MUTE">MUTE (Disable Chat)</option>
                        <option value="BAN">BAN (7 Days)</option>
                        <option value="GLOBAL_BAN">GLOBAL BAN (Permanent)</option>
                    </select>
                </div>
            `,
            showCancelButton: true,
            confirmButtonText: 'Apply Penalty',
            confirmButtonColor: '#d33',
            preConfirm: () => {
                return document.getElementById('swal-penalty').value;
            }
        }).then(async (result) => {
            if (result.isConfirmed) {
                const penalty = result.value;
                try {
                    await Api.post(`/api/admin/reports/${reportId}/process?accepted=true&penalty=${penalty}`);
                    Swal.fire('Processed', 'Report accepted and penalty applied', 'success');
                    this.loadReports();
                } catch (error) {
                    Swal.fire('Error', 'Failed to process report', 'error');
                }
            }
        });
    }
};
