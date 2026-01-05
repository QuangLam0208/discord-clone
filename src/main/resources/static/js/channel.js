// CHANNEL & CATEGORY MODULE

// 1. LOAD DATA
window.loadCategoriesAndChannels = async function (serverId) {
    const channelArea = document.getElementById('server-channel-area');
    if (!channelArea) return;
    channelArea.innerHTML = '<div style="color: #aaa; padding: 10px;">Loading...</div>';

    try {
        const [categories, channels] = await Promise.all([
            Api.get(`/api/categories/server/${serverId}`),
            Api.get(`/api/servers/${serverId}/channels`)
        ]);

        // Ensure WebSocket is connected even if no channel is selected yet
        if (window.connectChannelSocket) {
            window.connectChannelSocket(null);
        }

        const categoryMap = {};
        const nullCategoryChannels = [];

        if (categories && Array.isArray(categories)) {
            categories.forEach(cat => {
                // Robust ID detection
                const realId = cat.id || cat.categoryId || cat.uuid || cat._id;

                if (!realId) {
                    console.error("Category missing ID:", cat);
                    // FORCE ALERT TO DEBUG
                    alert("DEBUG: Server gửi dữ liệu Category thiếu ID. Các trường hiện có: " + Object.keys(cat).join(", "));
                } else {
                    // Normalize ID
                    cat.id = realId;
                    categoryMap[realId] = { ...cat, channelsList: [] };
                }
            });
        }

        if (channels && Array.isArray(channels)) {
            channels.forEach(ch => {
                if (ch.categoryId && categoryMap[ch.categoryId]) {
                    categoryMap[ch.categoryId].channelsList.push(ch);
                } else {
                    nullCategoryChannels.push(ch);
                }
            });
        }

        channelArea.innerHTML = '';

        if (nullCategoryChannels.length > 0) {
            const unCatDiv = document.createElement('div');
            unCatDiv.className = 'uncategorized-channels';
            nullCategoryChannels.forEach(ch => { unCatDiv.appendChild(createChannelItem(ch)); });
            channelArea.appendChild(unCatDiv);
        }

        if (categories && Array.isArray(categories)) {
            categories.forEach(cat => {
                const catData = categoryMap[cat.id];
                channelArea.appendChild(createCategoryElement(catData));
            });
        }

        // Auto Select First Channel
        const firstChannel = channelArea.querySelector('.channel-item:not([style*="color: #aaa"])');
        if (firstChannel) firstChannel.click();

    } catch (e) {
        console.error("Lỗi load channels:", e);
        channelArea.innerHTML = '<div style="color: red; padding: 10px;">Lỗi tải danh sách kênh.</div>';
    }
}

function createCategoryElement(categoryData) {
    const wrapper = document.createElement('div');
    wrapper.className = 'category-wrapper';
    wrapper.dataset.categoryId = categoryData.id;
    const header = document.createElement('div');
    header.className = 'category-header';

    const catInfo = document.createElement('div');
    catInfo.className = 'cat-info';
    catInfo.innerHTML = `<i class="fa-solid fa-angle-down"></i><span>${categoryData.name.toUpperCase()}</span>`;

    // Toggle Collapse
    catInfo.onclick = () => {
        const content = wrapper.querySelector('.category-content');
        const icon = catInfo.querySelector('i');
        if (content.style.display === 'none') {
            content.style.display = 'block';
            icon.className = 'fa-solid fa-angle-down';
        } else {
            content.style.display = 'none';
            icon.className = 'fa-solid fa-angle-right';
        }
    };

    const addIcon = document.createElement('i');
    addIcon.className = 'fa-solid fa-plus add-channel-icon';
    addIcon.setAttribute('title', 'Tạo kênh');
    // Bind click event: Traverse DOM to find ID
    addIcon.onclick = (e) => {
        e.stopPropagation();
        const wrapper = e.target.closest('.category-wrapper');
        const idFromDom = wrapper ? wrapper.dataset.categoryId : null;

        console.log("Clicked + btn. DOM ID:", idFromDom, "Closure ID:", categoryData.id);

        openCreateChannelModal(e, categoryData.name, idFromDom || categoryData.id);
    };

    header.appendChild(catInfo);
    header.appendChild(addIcon);

    const content = document.createElement('div');
    content.className = 'category-content';
    categoryData.channelsList.forEach(ch => { content.appendChild(createChannelItem(ch)); });
    wrapper.appendChild(header);
    wrapper.appendChild(content);
    return wrapper;
}

function createChannelItem(channel) {
    const div = document.createElement('div');
    div.className = 'channel-item';
    div.dataset.channelId = channel.id;
    const iconClass = (channel.type === 'VOICE') ? 'fa-volume-high' : 'fa-hashtag';
    div.innerHTML = `<i class="fa-solid ${iconClass}"></i> ${channel.name}`;
    div.onclick = () => {
        document.querySelectorAll('.channel-item').forEach(el => el.classList.remove('active'));
        div.classList.add('active');
        onSelectChannel(channel);
    };
    return div;
}

window.onSelectChannel = function (channel) {
    console.log("Selected channel:", channel);
    state.currentChannelId = channel.id;

    // Update Header UI
    const topBar = document.querySelector('.top-bar');
    if (topBar) {
        // Selector for "Server Chat View" top bar
        // Structure: <div class="top-bar"> <i class="fa-solid fa-hashtag" ...></i> <span>name</span> </div>

        const titleSpan = topBar.querySelector('span'); // First span usually name
        const icon = topBar.querySelector('i');         // First i usually icon

        if (titleSpan) titleSpan.innerText = channel.name;

        if (icon) {
            icon.className = (channel.type === 'VOICE') ? 'fa-solid fa-volume-high' : 'fa-solid fa-hashtag';
            // Ensure style consistency
            icon.style.color = '#72767d';
            icon.style.marginRight = '8px';
        }
    }

    // Subscribe to STOMP topic
    if (window.subscribeToChannel) {
        window.subscribeToChannel(channel.id);
    }

    // Load Message History
    if (window.loadMessages) {
        window.loadMessages(channel.id);
    } else {
        console.warn("loadMessages function not found");
    }
}

// 2. MODAL HELPERS
window.openCreateChannelModal = function (event, categoryName, categoryId) {
    if (event) event.stopPropagation();

    // Validate ID immediately
    if (!categoryId) {
        console.error("openCreateChannelModal called with null ID");
        // alert("Lỗi hệ thống: Category ID không hợp lệ."); // Optional
        return;
    }

    const modal = document.getElementById('createChannelInCatModal');
    if (modal) {
        document.getElementById('modal-category-name').innerText = categoryName;
        state.tempCategoryId = categoryId; // Keep for backup
        modal.dataset.categoryId = categoryId; // Store reliably in DOM
        console.log("Opening modal for cat:", categoryId);
        modal.classList.add('active');
    }
}

window.createCategory = function () {
    if (!state.currentServerId) return;
    const modal = document.getElementById('createCategoryModal');
    if (modal) modal.classList.add('active');
}

// 3. LISTENERS
document.addEventListener('DOMContentLoaded', () => {
    // A. CREATE CHANNEL IN CATEGORY
    const btnCreateChannelSubmit = document.querySelector('#createChannelInCatModal .btn-primary');
    if (btnCreateChannelSubmit) {
        btnCreateChannelSubmit.onclick = async () => {
            const modal = document.getElementById('createChannelInCatModal');
            const nameInput = modal.querySelector('input[type="text"]');
            const typeInput = modal.querySelector('input[name="channelTypeCat"]:checked');
            const isPrivateInput = modal.querySelector('.switch input');

            const name = nameInput.value.trim();
            const type = typeInput ? typeInput.value : 'TEXT';
            const isPrivate = isPrivateInput ? isPrivateInput.checked : false;

            // USE DATASET ID with Fallback
            const categoryId = modal.dataset.categoryId || state.tempCategoryId;

            if (!name) {
                alert("Tên kênh không được để trống");
                return;
            }

            if (!categoryId) {
                console.error("ERROR: Category ID missing from modal dataset and state");
                alert("Lỗi: Không tìm thấy Category ID. Vui lòng reload trang.");
                return;
            }

            try {
                await Api.post(`/api/servers/${state.currentServerId}/channels`, {
                    name: name,
                    type: type,
                    categoryId: categoryId,
                    isPrivate: isPrivate
                });
                modal.classList.remove('active');
                nameInput.value = '';
                loadCategoriesAndChannels(state.currentServerId);
            } catch (e) {
                console.error(e);
                alert("Lỗi tạo kênh: " + e.message);
            }
        };
    }

    // B. CREATE CATEGORY
    const btnCreateCategorySubmit = document.querySelector('#createCategoryModal .btn-primary');
    if (btnCreateCategorySubmit) {
        btnCreateCategorySubmit.onclick = async () => {
            const modal = document.getElementById('createCategoryModal');
            const nameInput = modal.querySelector('input[type="text"]');

            const name = nameInput.value.trim();
            if (!name) {
                alert("Tên danh mục không được để trống");
                return;
            }

            try {
                await Api.post('/api/categories', {
                    name: name,
                    serverId: state.currentServerId
                });
                modal.classList.remove('active');
                nameInput.value = '';
                loadCategoriesAndChannels(state.currentServerId);
            } catch (e) {
                console.error(e);
                alert("Lỗi tạo danh mục: " + e.message);
            }
        };
    }

    // C. CREATE CHANNEL ROOT
    const btnCreateChannelRootSubmit = document.querySelector('#createChannelRootModal .btn-primary');
    if (btnCreateChannelRootSubmit) {
        btnCreateChannelRootSubmit.onclick = async () => {
            const modal = document.getElementById('createChannelRootModal');
            const nameInput = modal.querySelector('input[type="text"]');
            const typeInput = modal.querySelector('input[name="channelTypeRoot"]:checked');
            const isPrivateInput = modal.querySelector('.switch input');

            const name = nameInput.value.trim();
            const type = typeInput ? typeInput.value : 'TEXT';
            const isPrivate = isPrivateInput ? isPrivateInput.checked : false;

            if (!name) {
                alert("Tên kênh không được để trống");
                return;
            }

            try {
                // Root channel -> categoryId: null
                await Api.post(`/api/servers/${state.currentServerId}/channels`, {
                    name: name,
                    type: type,
                    categoryId: null,
                    isPrivate: isPrivate
                });
                modal.classList.remove('active');
                nameInput.value = ''; // Reset
                loadCategoriesAndChannels(state.currentServerId);
            } catch (e) {
                console.error(e);
                alert("Lỗi tạo kênh root: " + e.message);
            }
        };
    }
});

// 4. EDIT & DELETE
window.editCategory = async (id) => {
    const { value: name } = await Swal.fire({ title: 'Đổi tên Category', input: 'text', showCancelButton: true });
    if (name) {
        try { await Api.put(`/api/categories/${id}`, { name }); loadCategoriesAndChannels(state.currentServerId); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
    }
};
window.deleteCategory = async (id) => {
    const res = await Swal.fire({ title: 'Xóa Category?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33' });
    if (res.isConfirmed) {
        try { await Api.delete(`/api/categories/${id}`); loadCategoriesAndChannels(state.currentServerId); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
    }
};

window.editChannel = async (id) => {
    const { value: name } = await Swal.fire({ title: 'Đổi tên Kênh', input: 'text', showCancelButton: true });
    if (name) {
        try { await Api.patch(`/api/channels/${id}`, { name }); loadCategoriesAndChannels(state.currentServerId); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
    }
};
window.deleteChannel = async (id) => {
    const res = await Swal.fire({ title: 'Xóa Kênh?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33' });
    if (res.isConfirmed) {
        try { await Api.delete(`/api/channels/${id}`); loadCategoriesAndChannels(state.currentServerId); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
    }
};

// 5. CONTEXT MENU LOGIC
document.addEventListener('DOMContentLoaded', () => {
    const ctxMenu = document.getElementById('context-menu');

    document.addEventListener('click', () => {
        if (ctxMenu) ctxMenu.classList.remove('visible');
    });

    document.addEventListener('contextmenu', (e) => {
        const targetServer = e.target.closest('.sidebar-item[data-server-id]');
        const targetCategory = e.target.closest('.category-header');
        const targetChannel = e.target.closest('.channel-item[data-channel-id]');

        if (targetServer || targetCategory || targetChannel) {
            e.preventDefault();
            if (ctxMenu) {
                ctxMenu.innerHTML = '';

                // 1. SERVER MENU
                if (targetServer) {
                    const serverId = targetServer.dataset.serverId;
                    const ownerId = parseInt(targetServer.dataset.ownerId);
                    const isOwner = state.currentUser && state.currentUser.id === ownerId;

                    // Luôn cho phép tạo Category (hoặc check permission nếu cần)
                    if (isOwner || true) { // Tạm thời cho phép all, hoặc check permission
                        ctxMenu.innerHTML += `
                            <div class="menu-item" onclick="createCategory()">
                                <span>Tạo Category</span><i class="fa-solid fa-folder-plus"></i>
                            </div>
                            <div class="menu-separator"></div>
                        `;
                    }

                    // Luôn hiển thị nút Cài đặt cho mọi người để xem danh sách thành viên
                    ctxMenu.innerHTML += `
                        <div class="menu-item" onclick="openServerSettings('${serverId}')">
                            <span>Cài đặt</span><i class="fa-solid fa-gear"></i>
                        </div>
                        <div class="menu-separator"></div>`;
                }

                // 2. CATEGORY MENU
                else if (targetCategory) {
                    const catId = targetCategory.parentElement.dataset.categoryId;
                    ctxMenu.innerHTML += `
                        <div class="menu-item" onclick="editCategory('${catId}')">
                            <span>Đổi tên</span><i class="fa-solid fa-pen"></i>
                        </div>
                        <div class="menu-item logout" onclick="deleteCategory('${catId}')">
                            <span>Xóa Category</span><i class="fa-solid fa-trash"></i>
                        </div>`;
                }

                // 3. CHANNEL MENU
                else if (targetChannel) {
                    const chanId = targetChannel.dataset.channelId;
                    ctxMenu.innerHTML += `
                        <div class="menu-item" onclick="openChannelSettings('${chanId}')">
                            <span>Cài đặt</span><i class="fa-solid fa-gear"></i>
                        </div>
                        <div class="menu-separator"></div>
                        <div class="menu-item logout" onclick="openChannelSettings('${chanId}', 'delete')">
                            <span>Xóa Kênh</span><i class="fa-solid fa-trash"></i>
                        </div>`;
                }

                ctxMenu.style.top = `${e.clientY}px`;
                ctxMenu.style.left = `${e.clientX}px`;
                ctxMenu.classList.add('visible');
            }
        } else {
            // Check if clicked clearly on empty area of sidebar
            const targetChannelArea = e.target.closest('#server-channel-area');
            if (targetChannelArea && ctxMenu) {
                e.preventDefault();
                ctxMenu.innerHTML = `
                     <div class="menu-item" onclick="triggerInvite()">
                         <span>Invite to Server</span>
                         <i class="fa-solid fa-user-plus icon-right"></i>
                     </div>
                     <div class="menu-separator"></div>
                     <div class="menu-item" onclick="triggerCreateChannelRoot()" id="createChannelRootBtn">
                         <span>Create Channel</span>
                         <i class="fa-solid fa-circle-plus icon-right"></i>
                     </div>
                     <div class="menu-item" onclick="triggerCreateCategory()" id="createCategoryBtn">
                         <span>Create Category</span>
                         <i class="fa-solid fa-folder-plus icon-right"></i>
                     </div>
                  `;
                ctxMenu.style.top = `${e.clientY}px`;
                ctxMenu.style.left = `${e.clientX}px`;
                ctxMenu.classList.add('visible');
            }
        }
    });
});

// Helper to get channel type from UI if missing in API response
function getChannelTypeFromUI(channelId) {
    const channelEl = document.querySelector(`.channel-item[data-channel-id="${channelId}"] i`);
    if (channelEl) {
        if (channelEl.classList.contains('fa-hashtag')) return 'TEXT';
        if (channelEl.classList.contains('fa-volume-high')) return 'VOICE';
    }
    return 'TEXT'; // Default
}

// 6. CHANNEL SETTINGS MODAL LOGIC
window.openChannelSettings = async function (channelId, initialTab = 'overview') {
    // Hide Context Menu
    const ctxMenu = document.getElementById('context-menu');
    if (ctxMenu) ctxMenu.classList.remove('visible');

    // Load Channel Details
    try {
        const channel = await Api.get(`/api/channels/${channelId}`);
        state.editingChannel = channel;
        state.editingChannelId = channelId;

        // Ensure type is present
        if (!state.editingChannel.type) {
            state.editingChannel.type = getChannelTypeFromUI(channelId);
        }

        // Populate Fields
        document.getElementById('nav-overview').click(); // Default UI state
        document.getElementById('edit-channel-name').value = channel.name || '';
        document.getElementById('edit-channel-desc').value = channel.description || '';
        document.getElementById('edit-channel-private').checked = channel.isPrivate || false;

        // Delete Tab info
        document.getElementById('delete-channel-name-display').innerText = channel.name;

        // Open Modal
        switchSettingsTab(initialTab);
        document.getElementById('channelSettingsModal').classList.add('active');

        // Reset save bar
        document.getElementById('channel-save-bar').classList.remove('visible');

        // Watch for changes
        watchForChanges();

    } catch (e) {
        console.error("Lỗi tải thông tin kênh:", e);
        alert("Không thể tải thông tin kênh.");
    }
}

window.switchSettingsTab = function (tabName) {
    // Update Sidebar
    document.querySelectorAll('.settings-nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`nav-${tabName}`).classList.add('active');

    // Update Content
    document.querySelectorAll('.settings-tab-content').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tabName}`).classList.add('active');
}

function watchForChanges() {
    const inputs = ['edit-channel-name', 'edit-channel-desc', 'edit-channel-private'];
    inputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.oninput = () => showSaveBar();
            el.onchange = () => showSaveBar();
        }
    });
}

function showSaveBar() {
    document.getElementById('channel-save-bar').classList.add('visible');
}

window.resetChannelSettings = function () {
    if (!state.editingChannel) return;
    document.getElementById('edit-channel-name').value = state.editingChannel.name || '';
    document.getElementById('edit-channel-desc').value = state.editingChannel.description || '';
    document.getElementById('edit-channel-private').checked = state.editingChannel.isPrivate || false;
    document.getElementById('channel-save-bar').classList.remove('visible');
}

window.saveChannelSettings = async function () {
    const name = document.getElementById('edit-channel-name').value;
    const desc = document.getElementById('edit-channel-desc').value;
    const isPrivate = document.getElementById('edit-channel-private').checked;

    try {
        await Api.patch(`/api/channels/${state.editingChannelId}`, {
            name: name,
            description: desc,
            isPrivate: isPrivate,
            categoryId: state.editingChannel.categoryId,
            type: state.editingChannel.type
        });

        // Success
        document.getElementById('channel-save-bar').classList.remove('visible');
        closeModal('channelSettingsModal'); // Đóng modal ngay lập tức
        loadCategoriesAndChannels(state.currentServerId);

        // Update local state to avoid "reset" reverting to old values
        state.editingChannel.name = name;
        state.editingChannel.description = desc;
        state.editingChannel.isPrivate = isPrivate;

        // Notification
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'success',
                title: 'Thành công',
                text: 'Cập nhật kênh thành công!',
                timer: 1500,
                showConfirmButton: false,
                background: '#313338',
                color: '#dbdee1'
            });
        }

    } catch (e) {
        console.error(e);
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                text: e.message || 'Không thể lưu thay đổi',
                background: '#313338',
                color: '#dbdee1'
            });
        } else {
            alert("Lỗi lưu thay đổi: " + e.message);
        }
    }
}

window.confirmDeleteChannel = async function () {
    try {
        await Api.delete(`/api/channels/${state.editingChannelId}`);
        closeModal('channelSettingsModal');
        loadCategoriesAndChannels(state.currentServerId);
    } catch (e) {
        alert("Lỗi xóa kênh: " + e.message);
    }
}