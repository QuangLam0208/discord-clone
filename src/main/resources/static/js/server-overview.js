// --- SERVER OVERVIEW TAB LOGIC ---

window.updateServerPreviews = function (name, iconUrl) {
    const acronym = name ? name.substring(0, 2).toUpperCase() : 'SV';

    // 1. Settings Panel Preview
    const smallImg = document.getElementById('edit-server-icon-preview');
    const smallPh = document.getElementById('edit-server-icon-placeholder');

    if (smallImg && smallPh) {
        if (iconUrl) {
            smallImg.src = iconUrl;
            smallImg.style.display = 'block';
            smallPh.style.display = 'none';
        } else {
            smallImg.style.display = 'none';
            smallPh.style.display = 'block';
            smallPh.innerText = acronym;
        }
    }

    // 2. Card Preview
    const cardName = document.getElementById('preview-card-name');
    const cardImg = document.getElementById('preview-card-icon-img');
    const cardPh = document.getElementById('preview-card-icon-text');

    if (cardName) cardName.innerText = name || 'Máy Chủ';

    if (cardImg && cardPh) {
        if (iconUrl) {
            cardImg.src = iconUrl;
            cardImg.style.display = 'block';
            cardPh.style.display = 'none';
        } else {
            cardImg.style.display = 'none';
            cardPh.style.display = 'block';
            cardPh.innerText = acronym;
        }
    }

    // 3. Remove Button visibility
    const removeBtn = document.querySelector('.remove-icon-btn');
    if (removeBtn) removeBtn.style.display = (iconUrl ? 'block' : 'none');
}

window.watchServerChanges = function () {
    const inputs = ['edit-server-name', 'edit-server-desc'];
    inputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.oninput = () => {
                checkServerChanges();
                // Live update name on card
                const name = document.getElementById('edit-server-name').value;
                const cardName = document.getElementById('preview-card-name');
                if (cardName) cardName.innerText = name || 'Server Name';

                // Update placeholder text if no icon
                if (!state.tempServerIcon && !state.editingServerData.iconUrl) {
                    const acronym = (name || '').substring(0, 2).toUpperCase();
                    const smallPh = document.getElementById('edit-server-icon-placeholder');
                    const cardPh = document.getElementById('preview-card-icon-text');
                    if (smallPh) smallPh.innerText = acronym;
                    if (cardPh) cardPh.innerText = acronym;
                }
            };
        }
    });

    const fileInput = document.getElementById('editServerIconInput');
    if (fileInput) {
        fileInput.onchange = function (e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (ev) => {
                    updateServerPreviews(document.getElementById('edit-server-name').value, ev.target.result);
                    state.tempServerIcon = file;
                    checkServerChanges();
                };
                reader.readAsDataURL(file);
            }
        };
    }
}

window.removeServerIcon = function () {
    document.getElementById('editServerIconInput').value = '';
    state.tempServerIcon = 'REMOVED';
    updateServerPreviews(document.getElementById('edit-server-name').value, null);
    checkServerChanges();
}

window.checkServerChanges = function () {
    const nameInput = document.getElementById('edit-server-name');
    const descInput = document.getElementById('edit-server-desc');
    if (!nameInput || !descInput) return;

    const currentName = (nameInput.value || '').trim();
    const currentDesc = (descInput.value || '').trim();

    const original = state.editingServerData || {};
    const originalName = (original.name || '').trim();
    const originalDesc = (original.description || '').trim();

    let hasChanges = false;
    if (currentName !== originalName) hasChanges = true;
    if (currentDesc !== originalDesc) hasChanges = true;
    if (state.tempServerIcon) hasChanges = true;

    const bar = document.getElementById('server-save-bar');
    if (bar) {
        if (hasChanges) bar.classList.add('visible');
        else bar.classList.remove('visible');
    }
}

window.resetServerOverview = function () {
    const original = state.editingServerData;
    if (!original) return;

    document.getElementById('edit-server-name').value = original.name;
    document.getElementById('edit-server-desc').value = original.description;

    state.tempServerIcon = null;
    document.getElementById('editServerIconInput').value = '';

    updateServerPreviews(original.name, original.iconUrl);
    checkServerChanges();

    Swal.fire({
        icon: 'success', title: 'Đã đặt lại!', toast: true,
        position: 'bottom-end', showConfirmButton: false, timer: 1500,
        background: '#1e1f22', color: '#fff'
    });
}

window.saveServerOverview = async function () {
    const nameInput = document.getElementById('edit-server-name');
    const descInput = document.getElementById('edit-server-desc');

    if (!nameInput || !nameInput.value.trim()) {
        Swal.fire('Lỗi', 'Tên máy chủ không được để trống', 'error');
        return;
    }

    let iconUrl = state.editingServerData.iconUrl;

    if (state.tempServerIcon && state.tempServerIcon !== 'REMOVED') {
        const formData = new FormData();
        formData.append('file', state.tempServerIcon);
        try {
            const uploadRes = await Api.upload('/api/upload', formData);
            if (uploadRes && uploadRes.url) {
                iconUrl = uploadRes.url;
            }
        } catch (e) {
            Swal.fire('Lỗi', 'Không thể upload ảnh: ' + e.message, 'error');
            return;
        }
    } else if (state.tempServerIcon === 'REMOVED') {
        iconUrl = null;
    }

    const payload = {
        name: nameInput.value.trim(),
        description: descInput ? descInput.value.trim() : null,
        iconUrl: iconUrl
    };

    try {
        const updatedServer = await Api.put(`/api/servers/${state.editingServerId}`, payload);
        state.editingServerData = updatedServer;
        state.tempServerIcon = null;
        document.getElementById('editServerIconInput').value = '';

        const saveBar = document.getElementById('server-save-bar');
        if (saveBar) saveBar.classList.remove('visible');

        if (window.loadServers) await loadServers();

        Swal.fire({
            icon: 'success', title: 'Đã lưu thay đổi!', toast: true,
            position: 'bottom-end', showConfirmButton: false, timer: 2000,
            background: '#1e1f22', color: '#fff'
        });

    } catch (e) {
        console.error(e);
        Swal.fire('Lỗi', 'Lưu hồ sơ thất bại', 'error');
    }
}

// DELETE SERVER (Delete Tab)
window.confirmDeleteServer = async function () {
    const serverId = state.editingServerId;
    const serverName = state.editingServerData.name;

    const result = await Swal.fire({
        title: 'Xóa máy chủ',
        html: `Bạn có chắc chắn muốn xóa máy chủ này không? Hành động này không thể hoàn tác.<br>Vui lòng nhập <b>${serverName}</b> để xác nhận.`,
        input: 'text',
        inputAttributes: { autocapitalize: 'off', placeholder: serverName },
        showCancelButton: true,
        confirmButtonText: 'Xóa Máy Chủ',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#da373c',
        background: '#313338',
        color: '#dbdee1',
        preConfirm: (inputValue) => {
            if (inputValue !== serverName) {
                Swal.showValidationMessage('Tên máy chủ không khớp. Vui lòng thử lại.');
            }
        }
    });

    if (result.isConfirmed) {
        try {
            const success = await Api.delete(`/api/servers/${serverId}`);
            if (success) {
                closeModal('serverSettingsModal');
                Swal.fire({
                    title: 'Đã xóa', text: 'Máy chủ đã được xóa thành công.', icon: 'success',
                    timer: 2000, showConfirmButton: false, background: '#313338', color: '#dbdee1'
                }).then(() => {
                    window.location.href = '/channels/@me';
                });
            } else {
                Swal.fire('Lỗi', 'Không thể xóa server', 'error');
            }
        } catch (e) {
            Swal.fire('Lỗi', e.message, 'error');
        }
    }
};