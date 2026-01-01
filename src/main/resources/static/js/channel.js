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

        const categoryMap = {};
        const nullCategoryChannels = [];

        if (categories && Array.isArray(categories)) {
            categories.forEach(cat => { categoryMap[cat.id] = { ...cat, channelsList: [] }; });
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

        // ADD CREATE CATEGORY BUTTON
        if (state.currentServerId) {
            const createCatBtn = document.createElement('div');
            createCatBtn.className = 'channel-item';
            createCatBtn.style.color = '#aaa';
            createCatBtn.style.cursor = 'pointer';
            createCatBtn.innerHTML = '<i class="fa-solid fa-folder-plus"></i> Tạo Category mới';
            createCatBtn.onclick = createCategory;
            channelArea.appendChild(createCatBtn);
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
    header.innerHTML = `
        <div class="cat-info"><i class="fa-solid fa-angle-down"></i><span>${categoryData.name.toUpperCase()}</span></div>
        <i class="fa-solid fa-plus add-channel-icon" title="Tạo kênh" onclick="openCreateChannelModal(event, '${categoryData.name}', '${categoryData.id}')"></i>
    `;
    header.querySelector('.cat-info').onclick = () => {
        const content = wrapper.querySelector('.category-content');
        const icon = header.querySelector('i');
        if (content.style.display === 'none') {
            content.style.display = 'block';
            icon.className = 'fa-solid fa-angle-down';
        } else {
            content.style.display = 'none';
            icon.className = 'fa-solid fa-angle-right';
        }
    };
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

// 2. MODAL HELPERS
window.openCreateChannelModal = function (event, categoryName, categoryId) {
    if (event) event.stopPropagation();
    const modal = document.getElementById('createChannelInCatModal');
    if (modal) {
        document.getElementById('modal-category-name').innerText = categoryName;
        state.tempCategoryId = categoryId;
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

            if (!name) {
                alert("Tên kênh không được để trống");
                return;
            }

            try {
                await Api.post(`/api/servers/${state.currentServerId}/channels`, {
                    name: name,
                    type: type,
                    categoryId: state.tempCategoryId,
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
            const isPrivateInput = modal.querySelector('.switch input');

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
                    ctxMenu.innerHTML += `
                        <div class="menu-item" onclick="editServer('${serverId}')">
                            <span>Sửa Server</span><i class="fa-solid fa-pen"></i>
                        </div>
                        <div class="menu-separator"></div>
                        <div class="menu-item logout" onclick="deleteServer('${serverId}')">
                            <span>Xóa Server</span><i class="fa-solid fa-trash"></i>
                        </div>`;
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
                        <div class="menu-item" onclick="editChannel('${chanId}')">
                            <span>Sửa Kênh</span><i class="fa-solid fa-pen"></i>
                        </div>
                        <div class="menu-item logout" onclick="deleteChannel('${chanId}')">
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
