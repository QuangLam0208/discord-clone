document.addEventListener('DOMContentLoaded', async function () {
    // 1. GLOBAL STATE & ELEMENTS
    const state = {
        currentServerId: null,
        currentChannelId: null,
        currentUser: null,
        stompClient: null
    };

    const elements = {
        app: document.getElementById('app'),
        userDisplay: document.getElementById('user-display'),
        userAvatars: document.querySelectorAll('.user-avatar'),
        sidebar: document.querySelector('.sidebar'), // Container chứa server list
    };

    // 2. INIT APPLICATION
    async function init() {
        if (elements.app) elements.app.style.display = 'flex';
        await loadMe();
        await loadServers();
        console.log("App Initialized & UI Synced");
    }

    // 3. LOAD USER INFO
    async function loadMe() {
        try {
            const user = await Api.get('/api/users/me');
            if (user) {
                state.currentUser = user;
                if (elements.userDisplay) elements.userDisplay.innerText = user.displayName || user.username;
                const nameForAvatar = user.displayName || user.username || 'User';
                const avatarUrl = user.avatarUrl
                    ? user.avatarUrl
                    : `https://ui-avatars.com/api/?name=${encodeURIComponent(nameForAvatar)}&background=random&color=fff&size=128&bold=true`;
                elements.userAvatars.forEach(el => {
                    el.style.backgroundImage = `url('${avatarUrl}')`;
                    el.style.backgroundSize = 'cover';
                    el.style.backgroundPosition = 'center';
                    el.style.backgroundColor = 'transparent';
                    el.innerHTML = '';
                });
            }
        } catch (e) {
            console.error("Lỗi load user:", e);
        }
    }

    // 4. LOAD SERVERS
    async function loadServers() {
        if (!elements.sidebar) return;
        try {
            const servers = await Api.get('/api/servers/me');
            if (servers && Array.isArray(servers)) {
                renderServerSidebar(servers);
            }
        } catch (e) {
            console.error("Lỗi load servers:", e);
        }
    }

    function renderServerSidebar(servers) {
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
        div.title = server.name;
        let contentHtml = '';
        if (server.iconUrl) {
            contentHtml = `<img src="${server.iconUrl}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
        } else {
            const acronym = server.name.substring(0, 2).toUpperCase();
            contentHtml = `<div class="sidebar-icon">${acronym}</div>`;
        }
        div.innerHTML = `<div class="pill"></div>${contentHtml}<span class="tooltip">${server.name}</span>`;
        div.onclick = function () {
            document.querySelectorAll('.sidebar-item').forEach(e => e.classList.remove('active'));
            div.classList.add('active');
            if (window.switchToServer) window.switchToServer(div);
            onClickServer(server.id);
        };
        return div;
    }

    // 5. EVENT HANDLERS
    const createServerBtn = document.getElementById('createServerBtn');
    if (createServerBtn) {
        createServerBtn.onclick = async () => {
            await Swal.fire({
                title: 'Tạo máy chủ mới',
                html: `
                    <div style="display: flex; flex-direction: column; align-items: center; gap: 15px; margin-bottom: 10px;">
                        <div id="swal-preview" style="width: 80px; height: 80px; border-radius: 50%; background-color: #333; display: flex; justify-content: center; align-items: center; overflow: hidden; border: 2px dashed #99aab5; cursor: pointer; position: relative;">
                            <span style="font-size: 10px; color: #b9bbbe; font-weight: bold; text-align: center;">UPLOAD<br>ICON</span>
                            <img id="swal-preview-img" style="width: 100%; height: 100%; object-fit: cover; display: none; position: absolute; top:0; left:0;">
                        </div>
                        <input type="file" id="swal-file" style="display: none" accept="image/*">
                        <input id="swal-input1" class="swal2-input" placeholder="Tên máy chủ" style="margin: 0 !important; width: 80%;">
                    </div>
                `,
                focusConfirm: false,
                showCancelButton: true,
                confirmButtonText: 'Tạo',
                cancelButtonText: 'Hủy',
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
                        // Validate file if needed (size etc handled by backend or api helper, but basic check is good)
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
                        const newServer = await Api.post('/api/servers', {
                            name: name,
                            iconUrl: iconUrl
                        });

                        if (newServer) {
                            Swal.fire('Thành công', 'Máy chủ đã được tạo!', 'success');
                            await loadServers();
                            onClickServer(newServer.id);
                        }
                    } catch (e) {
                        console.error("Create server error:", e);
                        Swal.fire('Lỗi', 'Không thể tạo máy chủ: ' + e.message, 'error');
                    }
                }
            });
        };
    }

    window.onClickServer = function (serverId) {
        if (state.currentServerId === serverId) return;
        state.currentServerId = serverId;
        const headerName = document.querySelector('.channel-header h4');
        if (headerName) {
            const serverItem = document.querySelector(`.sidebar-item[data-server-id="${serverId}"]`);
            if (serverItem) headerName.innerText = serverItem.getAttribute('title');
        }
        loadCategoriesAndChannels(serverId);
    }

    // 6. LOAD CATEGORIES & CHANNELS (Đã sửa lại vị trí đúng)
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

    // CREATE CHANNEL MODAL (HTML)
    window.openCreateChannelModal = function (event, categoryName, categoryId) {
        if (event) event.stopPropagation();
        const modal = document.getElementById('createChannelInCatModal');
        if (modal) {
            document.getElementById('modal-category-name').innerText = categoryName;
            state.tempCategoryId = categoryId; // Store temporarily
            modal.classList.add('active');
        }
    }

    // CREATE CATEGORY (HTML)
    window.createCategory = function () {
        if (!state.currentServerId) return;
        const modal = document.getElementById('createCategoryModal');
        if (modal) modal.classList.add('active');
    }

    // BIND EVENT LISTENERS FOR MODALS
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
                nameInput.value = ''; // Reset
                loadCategoriesAndChannels(state.currentServerId);
            } catch (e) {
                console.error(e);
                alert("Lỗi tạo kênh: " + e.message);
            }
        };
    }

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

    // 7. CHANNEL SELECTION & MESSAGE LOAD
    window.onSelectChannel = async function (channel) {
        if (state.currentChannelId === channel.id) return;

        state.currentChannelId = channel.id;
        console.log("Joined Channel:", channel.name);

        // 1. Update UI Header
        const topBarTitle = document.querySelector('.top-bar span');
        const topBarIcon = document.querySelector('.top-bar i');
        if (topBarTitle) topBarTitle.innerText = channel.name;
        if (topBarIcon) topBarIcon.className = (channel.type === 'VOICE') ? 'fa-solid fa-volume-high' : 'fa-solid fa-hashtag';

        // 2. Load Message History
        await loadMessages(channel.id);

        // 3. Connect/Switch WebSocket Subscription
        connectChannelSocket(channel.id);
    }

    async function loadMessages(channelId) {
        const chatArea = document.querySelector('.chat-area');
        if (!chatArea) return;

        chatArea.innerHTML = '<div style="color: #aaa; text-align: center; margin-top: 20px;">Đang tải tin nhắn...</div>';

        try {
            // GET /api/channels/{id}/messages?page=0&size=50
            const messages = await Api.get(`/api/channels/${channelId}/messages?size=50`);

            chatArea.innerHTML = ''; // Clear loading

            if (messages && Array.isArray(messages)) {
                // API thường trả về mới nhất trước -> Javascript cần reverse để hiển thị đúng thứ tự thời gian (cũ trên, mới dưới)
                // Tuy nhiên nếu API của bạn đã sort ASC theo time thì ko cần reverse.
                // Thường chat app: tin mới nhất ở dưới cùng.
                const sortedMsgs = messages.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

                sortedMsgs.forEach(msg => {
                    const msgEl = createMessageElement(msg);
                    chatArea.appendChild(msgEl);
                });

                scrollToBottom();
            } else {
                chatArea.innerHTML = '<div style="color: #72767d; text-align: center; margin-top: 20px;">Chưa có tin nhắn nào. Hãy bắt đầu trò chuyện!</div>';
            }
        } catch (e) {
            console.error("Lỗi load tin nhắn:", e);
            chatArea.innerHTML = '<div style="color: #ed4245; text-align: center;">Không thể tải tin nhắn.</div>';
        }
    }

    function createMessageElement(msg) {
        const div = document.createElement('div');
        div.className = 'message';
        // Format time: HH:mm
        const time = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const senderName = msg.senderName || msg.senderId;
        // Avatar giả lập
        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(senderName)}&background=random&color=fff&size=40`;

        let attachmentsHtml = '';
        if (msg.attachments && Array.isArray(msg.attachments) && msg.attachments.length > 0) {
            msg.attachments.forEach(url => {
                attachmentsHtml += `<div style="margin-top: 5px;"><img src="${url}" style="max-width: 300px; max-height: 300px; border-radius: 4px;" loading="lazy"></div>`;
            });
        }

        div.innerHTML = `
            <div class="user-avatar" style="width: 40px; height: 40px; margin-right: 16px; background-image: url('${avatarUrl}'); background-size: cover; border-radius: 50%;"></div>
            <div>
                <div class="msg-user">
                    ${senderName} 
                    <span style="font-size: 12px; color: #b5bac1; font-weight: normal; margin-left: 5px;">${time}</span>
                </div>
                <div class="msg-content">${msg.content || ''}</div>
                ${attachmentsHtml}
            </div>
        `;
        return div;
    }

    function scrollToBottom() {
        const chatArea = document.querySelector('.chat-area');
        if (chatArea) chatArea.scrollTop = chatArea.scrollHeight;
    }

    // 8. WEBSOCKET LOGIC (STOMP)
    let channelSubscription = null;

    function connectChannelSocket(channelId) {
        // Nếu chưa kết nối Socket tổng -> connect
        if (!state.stompClient || !state.stompClient.connected) {
            const socket = new SockJS('/ws');
            state.stompClient = Stomp.over(socket);
            state.stompClient.debug = null; // Tắt log debug cho gọn

            const token = localStorage.getItem('accessToken');
            state.stompClient.connect({ 'Authorization': `Bearer ${token}` }, function (frame) {
                console.log('Connected to WebSocket');
                subscribeToChannel(channelId);
            }, function (error) {
                console.error('WebSocket Error:', error);
                // Auto reconnect sau 5s nếu mất kết nối
                setTimeout(() => {
                    console.log("Reconnecting WebSocket...");
                    connectChannelSocket(channelId);
                }, 5000);
            });
        } else {
            // Đã connect -> chỉ cần subscribe channel mới
            subscribeToChannel(channelId);
        }
    }

    function subscribeToChannel(channelId) {
        // Hủy đăng ký channel cũ nếu có
        if (channelSubscription) {
            channelSubscription.unsubscribe();
        }

        // Subscribe channel mới: /topic/channel/{id}
        channelSubscription = state.stompClient.subscribe(`/topic/channel/${channelId}`, function (message) {
            const receivedMsg = JSON.parse(message.body);
            // Render tin nhắn mới nhận được
            const chatArea = document.querySelector('.chat-area');

            // Nếu đây là tin nhắn đầu tiên (đang hiện text "Chưa có tin nhắn...") -> clear đi
            if (chatArea.innerText.includes("Chưa có tin nhắn")) chatArea.innerHTML = '';

            const msgEl = createMessageElement(receivedMsg);
            chatArea.appendChild(msgEl);
            scrollToBottom();
        });

        console.log(`Subscribed to /topic/channel/${channelId}`);
    }

    // 9. SEND MESSAGE
    // 9. UPLOAD & SEND MESSAGE
    const chatInput = document.getElementById('chat-input');
    const uploadBtn = document.querySelector('.upload-icon-btn');
    const fileInput = document.getElementById('hidden-file-upload');

    // Store pending attachments
    let pendingAttachments = [];

    // Create Preview Container (Dynamic)
    const inputArea = document.querySelector('.input-area');
    const previewContainer = document.createElement('div');
    previewContainer.className = 'attachment-preview-area';
    previewContainer.style.display = 'none';
    previewContainer.style.padding = '10px 16px 0';
    previewContainer.style.gap = '10px';
    previewContainer.style.display = 'flex';
    previewContainer.style.flexWrap = 'wrap';

    // Insert preview before input wrapper
    if (inputArea) {
        inputArea.insertBefore(previewContainer, inputArea.firstChild);
    }

    function renderPreviews() {
        previewContainer.innerHTML = '';
        if (pendingAttachments.length === 0) {
            previewContainer.style.display = 'none';
            return;
        }
        previewContainer.style.display = 'flex';

        pendingAttachments.forEach((url, index) => {
            const wrapper = document.createElement('div');
            wrapper.style.position = 'relative';
            wrapper.style.width = '100px';
            wrapper.style.height = '100px';
            wrapper.style.borderRadius = '4px';
            wrapper.style.overflow = 'hidden';
            wrapper.style.border = '1px solid #202225';

            const img = document.createElement('img');
            img.src = url;
            img.style.width = '100%';
            img.style.height = '100%';
            img.style.objectFit = 'cover';

            const removeBtn = document.createElement('div');
            removeBtn.innerHTML = '<i class="fa-solid fa-xmark"></i>';
            removeBtn.style.position = 'absolute';
            removeBtn.style.top = '2px';
            removeBtn.style.right = '2px';
            removeBtn.style.backgroundColor = '#ed4245';
            removeBtn.style.color = 'white';
            removeBtn.style.borderRadius = '50%';
            removeBtn.style.width = '18px';
            removeBtn.style.height = '18px';
            removeBtn.style.display = 'flex';
            removeBtn.style.justifyContent = 'center';
            removeBtn.style.alignItems = 'center';
            removeBtn.style.fontSize = '10px';
            removeBtn.style.cursor = 'pointer';

            removeBtn.onclick = () => {
                pendingAttachments.splice(index, 1);
                renderPreviews();
            };

            wrapper.appendChild(img);
            wrapper.appendChild(removeBtn);
            previewContainer.appendChild(wrapper);
        });
    }

    // Trigger file input
    if (uploadBtn && fileInput) {
        uploadBtn.addEventListener('click', (e) => {
            e.preventDefault(); // Ngăn chặn hành vi mặc định (nếu có)
            e.stopPropagation(); // Ngăn chặn nổi bọt sự kiện
            fileInput.click();
        });

        fileInput.addEventListener('change', async () => {
            const file = fileInput.files[0];
            if (!file) return;

            // Validate size (max 10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert("File quá lớn (Max 10MB).");
                return;
            }

            const formData = new FormData();
            formData.append('file', file);

            try {
                // Upload immediately but don't send message yet
                const response = await Api.upload('/api/upload', formData);
                if (response && response.url) {
                    console.log("Uploaded pending:", response.url);
                    pendingAttachments.push(response.url);
                    renderPreviews();
                    chatInput.focus(); // Focus back to input
                }
            } catch (e) {
                console.error("Upload error:", e);
                alert("Lỗi upload file: " + e.message);
            } finally {
                fileInput.value = '';
            }
        });
    }

    if (chatInput) {
        chatInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                const content = chatInput.value.trim();
                // Send if content exists OR there are attachments
                if ((content || pendingAttachments.length > 0) && state.currentChannelId) {
                    sendMessage(content, [...pendingAttachments]); // Send copy
                    chatInput.value = '';

                    // Clear pending
                    pendingAttachments = [];
                    renderPreviews();
                }
            }
        });
    }

    function sendMessage(content, attachments = []) {
        if (!state.stompClient || !state.stompClient.connected) {
            alert("Mất kết nối máy chủ. Vui lòng reload.");
            return;
        }

        if (!content && (!attachments || attachments.length === 0)) return;

        const payload = {
            channelId: state.currentChannelId,
            content: content || '',
            attachments: attachments
        };

        state.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(payload));
    }
    // 10. USER SETTINGS & LOGOUT
    const btnSettings = document.getElementById('btn-settings');
    const settingsPopup = document.getElementById('settings-popup');
    const btnLogoutAction = document.getElementById('btn-logout-action');

    if (btnSettings && settingsPopup) {
        if (!btnSettings.hasAttribute('tabindex')) {
            btnSettings.setAttribute('tabindex', '0');
        }
        if (!btnSettings.hasAttribute('role')) {
            btnSettings.setAttribute('role', 'button');
        }

        const toggleSettingsPopup = (event) => {
            event.stopPropagation();
            // User cũ dùng class 'show', không phải 'active'
            settingsPopup.classList.toggle('show');
        };

        btnSettings.addEventListener('click', (e) => {
            toggleSettingsPopup(e);
        });
    }

    // Đóng popup khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (settingsPopup && !settingsPopup.contains(e.target) && e.target !== btnSettings) {
            settingsPopup.classList.remove('show');
        }
    });

    // Logout với SweetAlert2
    if (btnLogoutAction) {
        btnLogoutAction.addEventListener('click', () => {
            Swal.fire({
                title: 'Đăng xuất?',
                text: "Bạn có chắc chắn muốn đăng xuất không?",
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#ed4245',
                cancelButtonColor: '#7289da',
                confirmButtonText: 'Đăng xuất',
                cancelButtonText: 'Hủy',
                background: '#36393f',
                color: '#fff'
            }).then((result) => {
                if (result.isConfirmed) {
                    localStorage.clear();
                    window.location.href = '/login';
                }
            });
        });
    }

    // 11. CONTEXT MENU & CRUD
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
                            <span>Đổi tên Login</span><i class="fa-solid fa-pen"></i>
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
        }
    });

    // CRUD FUNCTIONS (GLOBAL)
    window.editServer = async (id) => {
        const { value: name } = await Swal.fire({ title: 'Đổi tên Server', input: 'text', showCancelButton: true });
        if (name) {
            try { await Api.put(`/api/servers/${id}`, { name }); await loadServers(); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
        }
    };
    window.deleteServer = async (id) => {
        const res = await Swal.fire({ title: 'Xóa Server?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33' });
        if (res.isConfirmed) {
            try { await Api.delete(`/api/servers/${id}`); location.reload(); } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
        }
    };

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

    // 12. START APP
    init();
});