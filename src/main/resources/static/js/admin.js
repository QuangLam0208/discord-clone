/* =========================================================
   ADMIN DASHBOARD LOGIC
   File: static/js/admin.js
   ========================================================= */

/**
 * Đóng Modal theo ID
 */
function closeModal(modalId) {
    var modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
}

/**
 * Tự động đóng Modal khi click ra ngoài vùng nội dung (Overlay)
 */
window.onclick = function(event) {
    if (event.target.classList.contains('modal-overlay')) {
        event.target.classList.remove('active');
    }
}

/* ================= LOGIC TRANG QUẢN LÝ SERVERS ================= */

function openDeleteModal(id, name) {
    var modal = document.getElementById('deleteModal');
    if (modal) {
        modal.classList.add('active');
        document.getElementById('deleteServerId').value = id;
        document.getElementById('deleteServerName').innerText = name;
    }
}

function openTransferModal(id, name) {
    var modal = document.getElementById('transferModal');
    if (modal) {
        modal.classList.add('active');
        document.getElementById('transferServerId').value = id;

        // Hiển thị tên server vào ô input readonly
        var nameInput = document.getElementById('transferServerName');
        if (nameInput) nameInput.value = name;

        // Reset ô nhập username mới
        var ownerInput = document.getElementById('newOwnerInput');
        if (ownerInput) {
            ownerInput.value = '';
            ownerInput.focus();
        }
    }
}

/* ================= LOGIC TRANG QUẢN LÝ USERS ================= */

function openBanModal(userId, username) {
    var modal = document.getElementById('banModal');
    if (modal) {
        modal.classList.add('active');
        document.getElementById('banUserId').value = userId;
        document.getElementById('banUsername').innerText = username;
    }
}

function openEditModal(userId, username) {
    var modal = document.getElementById('editModal');
    if (modal) {
        modal.classList.add('active');

        // 1. Điền ID và Username
        document.getElementById('editUserId').value = userId;
        document.getElementById('editUsername').value = username;

        // 2. Lấy Display Name từ input ẩn trong bảng HTML
        var displayNameInput = document.getElementById('displayName-' + userId);
        if (displayNameInput) {
            document.getElementById('editDisplayName').value = displayNameInput.value;
        }

        // 3. Xử lý Checkbox Roles
        // Reset tất cả checkbox về false trước
        var checkboxes = document.querySelectorAll('input[name="roleNames"]');
        checkboxes.forEach(function(cb) { cb.checked = false; });

        // Lấy danh sách role của user này từ div ẩn
        var userRolesDiv = document.getElementById('roles-' + userId);
        if (userRolesDiv) {
            var rolesStr = userRolesDiv.innerText; // Ví dụ: "ADMIN, USER_DEFAULT,"
            checkboxes.forEach(function(cb) {
                // Nếu trong chuỗi roles có chứa value của checkbox -> tick vào
                if (rolesStr.includes(cb.value)) {
                    cb.checked = true;
                }
            });
        }
    }
}