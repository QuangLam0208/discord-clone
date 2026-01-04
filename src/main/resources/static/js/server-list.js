
// --- SERVER LIST & SIDEBAR LOGIC ---

// 1. LOAD SERVERS
window.loadServers = async function () {
    if (!elements || !elements.sidebar) return;
    try {
        const servers = await Api.get('/api/servers/me');
        if (servers && Array.isArray(servers)) {
            renderServerSidebar(servers);
        }
    } catch (e) {
        console.error("Lỗi load servers:", e);
    }
}

window.renderServerSidebar = function (servers) {
    const children = Array.from(elements.sidebar.children);
    children.forEach(child => {
        if (child.id === 'btn-dm' || child.classList.contains('separator') || child.id === 'createServerBtn') {
            // Keep static items
        } else {
            child.remove();
        }
    });

    const createBtn = document.getElementById('createServerBtn');
    servers.forEach(server => {
        const serverEl = createServerIcon(server);
        elements.sidebar.insertBefore(serverEl, createBtn);
    });
}

function createServerIcon(server) {
    const div = document.createElement('div');
    div.className = 'sidebar-item';
    div.dataset.serverId = server.id;
    div.dataset.ownerId = server.ownerId;
    div.title = server.name;

    let contentHtml = '';
    if (server.iconUrl) {
        contentHtml = `<img src="${server.iconUrl}">`;
    } else {
        const acronym = server.name.substring(0, 2).toUpperCase();
        contentHtml = acronym;
    }

    div.innerHTML = `
        <div class="pill"></div>
        <div class="sidebar-icon">${contentHtml}</div>
        <span class="tooltip">${server.name}</span>
    `;

    div.onclick = function () {
        document.querySelectorAll('.sidebar-item').forEach(e => e.classList.remove('active'));
        div.classList.add('active');

        // Use global switch function if available (usually in channels or main layout)
        if (window.switchToServer) window.switchToServer(div);

        if (window.onClickServer) {
            window.onClickServer(server.id);
        }
    };
    return div;
}

window.onClickServer = function (serverId) {
    if (state.currentServerId === serverId) return;
    state.currentServerId = serverId;

    const headerName = document.querySelector('.channel-header h4');
    if (headerName) {
        const serverItem = document.querySelector(`.sidebar-item[data-server-id="${serverId}"]`);
        if (serverItem) headerName.innerText = serverItem.getAttribute('title');
    }

    // Load categories via channel.js
    if (window.loadCategoriesAndChannels) {
        loadCategoriesAndChannels(serverId);
    }
}

// 2. CREATE / JOIN SERVER UI
document.addEventListener('DOMContentLoaded', () => {
    const createServerBtn = document.getElementById('createServerBtn');
    if (createServerBtn) {
        createServerBtn.onclick = async () => {
            const result = await Swal.fire({
                title: 'Thêm máy chủ',
                text: 'Bạn muốn tạo máy chủ mới hay tham gia máy chủ có sẵn?',
                showCancelButton: true,
                showDenyButton: true,
                confirmButtonText: 'Tạo mới',
                denyButtonText: 'Tham gia',
                cancelButtonText: 'Hủy',
                confirmButtonColor: '#23a559',
                denyButtonColor: '#5865f2',
                background: '#313338',
                color: '#dbdee1'
            });

            if (result.isConfirmed) {
                showCreateServerDialog();
            } else if (result.isDenied) {
                showJoinServerDialog();
            }
        };
    }
});

async function showJoinServerDialog() {
    const { value: code } = await Swal.fire({
        title: 'Tham gia máy chủ',
        input: 'text',
        inputLabel: 'Nhập mã mời hoặc liên kết',
        inputPlaceholder: 'https://discord.gg/... hoặc mã code',
        showCancelButton: true,
        confirmButtonText: 'Tham gia',
        cancelButtonText: 'Hủy',
        background: '#313338',
        color: '#dbdee1',
        confirmButtonColor: '#5865f2',
        inputValidator: (value) => {
            if (!value) {
                return 'Bạn cần nhập mã mời!';
            }
        }
    });

    if (code) {
        let inviteCode = code;
        if (code.includes('/invite/')) {
            const parts = code.split('/invite/');
            if (parts.length > 1) inviteCode = parts[1];
        } else if (code.includes('/')) {
            const parts = code.split('/');
            inviteCode = parts[parts.length - 1];
        }

        if (inviteCode && inviteCode.includes('?')) {
            inviteCode = inviteCode.split('?')[0];
        }
        inviteCode = inviteCode ? inviteCode.trim() : '';

        if (!inviteCode || inviteCode === 'undefined') {
            Swal.fire('Lỗi', 'Mã mời không hợp lệ', 'error');
            return;
        }

        try {
            const response = await Api.post(`/api/invites/join/${inviteCode}`);
            Swal.fire({
                icon: 'success',
                title: 'Thành công',
                text: 'Bạn đã tham gia máy chủ!',
                background: '#313338',
                color: '#dbdee1',
                timer: 1500,
                showConfirmButton: false
            });
            await loadServers();

            if (response && response.serverId) {
                setTimeout(() => onClickServer(response.serverId), 500);
            }
        } catch (e) {
            console.error(e);
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                text: e.message || 'Mã mời không hợp lệ hoặc đã hết hạn.',
                background: '#313338',
                color: '#dbdee1'
            });
        }
    }
}

async function showCreateServerDialog() {
    await Swal.fire({
        title: 'Tạo máy chủ mới',
        html: `
            <div style="display: flex; flex-direction: column; align-items: center; gap: 15px; margin-bottom: 10px;">
                <div id="swal-preview" style="width: 80px; height: 80px; border-radius: 50%; background-color: #333; display: flex; justify-content: center; align-items: center; overflow: hidden; border: 2px dashed #99aab5; cursor: pointer; position: relative;">
                    <span style="font-size: 10px; color: #b9bbbe; font-weight: bold; text-align: center;">UPLOAD<br>ICON</span>
                    <img id="swal-preview-img" style="width: 100%; height: 100%; object-fit: cover; display: none; position: absolute; top:0; left:0;">
                </div>
                <input type="file" id="swal-file" style="display: none" accept="image/*">
                <input id="swal-input1" class="swal2-input" placeholder="Tên máy chủ" style="margin: 0 !important; width: 80%; background: #1e1f22; color: #dbdee1; border: none;">
            </div>
        `,
        background: '#313338',
        color: '#dbdee1',
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: 'Tạo',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#23a559',
        didOpen: () => {
            const preview = document.getElementById('swal-preview');
            const fileInput = document.getElementById('swal-file');
            const previewImg = document.getElementById('swal-preview-img');

            preview.onclick = () => fileInput.click();

            fileInput.onchange = () => {
                const file = fileInput.files[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        previewImg.src = e.target.result;
                        previewImg.style.display = 'block';
                    }
                    reader.readAsDataURL(file);
                }
            };
        },
        preConfirm: async () => {
            const name = document.getElementById('swal-input1').value;
            const file = document.getElementById('swal-file').files[0];

            if (!name) {
                Swal.showValidationMessage('Vui lòng nhập tên máy chủ');
                return false;
            }

            let iconUrl = null;
            if (file) {
                if (file.size > 10 * 1024 * 1024) {
                    Swal.showValidationMessage('File quá lớn (Max 10MB)');
                    return false;
                }
                try {
                    const formData = new FormData();
                    formData.append('file', file);
                    const res = await Api.upload('/api/upload', formData);
                    iconUrl = res.url;
                } catch (e) {
                    Swal.showValidationMessage('Lỗi upload ảnh: ' + e.message);
                    return false;
                }
            }

            return { name, iconUrl };
        }
    }).then(async (result) => {
        if (result.isConfirmed) {
            const { name, iconUrl } = result.value;
            try {
                const newServer = await Api.post('/api/servers', { name: name, iconUrl: iconUrl });
                if (newServer) {
                    Swal.fire({
                        icon: 'success', title: 'Thành công', text: 'Máy chủ đã được tạo!',
                        background: '#313338', color: '#dbdee1'
                    });
                    await loadServers();
                    onClickServer(newServer.id);
                }
            } catch (e) {
                console.error("Create server error:", e);
                Swal.fire({
                    icon: 'error', title: 'Lỗi', text: 'Không thể tạo máy chủ: ' + e.message,
                    background: '#313338', color: '#dbdee1'
                });
            }
        }
    });
}