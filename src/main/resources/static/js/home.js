document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('accessToken');
    const displayNameLS = localStorage.getItem('displayName');
    const usernameLS = localStorage.getItem('username');

    // --- Biến quản lý trạng thái Chat ---
    let currentContext = {
        type: null,      // 'DM'
        id: null,        // conversationId
        receiverId: null // userId người nhận
    };
    let stompClient = null; // Biến giữ kết nối Socket

    if (!token) {
        window.location.href = '/login';
        return;
    }

    // --- Hàm gọi API có kèm Token ---
    function authFetch(url, options = {}) {
        const headers = options.headers instanceof Headers
            ? options.headers
            : new Headers(options.headers || {});

        if (token) {
            headers.set('Authorization', 'Bearer ' + token);
            if (!headers.has('Content-Type') && options.method === 'POST' && !(options.body instanceof FormData)) {
                headers.set('Content-Type', 'application/json');
            }
        }

        return fetch(url, { ...options, headers });
    }
    window.authFetch = authFetch;

    document.getElementById('app').style.display = 'flex';

    // --- Hiển thị User hiện tại ---
    const displayName = displayNameLS || usernameLS || 'User';
    document.getElementById('user-display').innerText = displayName;
    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&color=fff&size=128&bold=true`;

    const currentUserAvatars = document.querySelectorAll('.user-wrapper .user-avatar');
    currentUserAvatars.forEach(avatar => {
        avatar.style.backgroundImage = `url('${avatarUrl}')`;
        avatar.style.backgroundSize = 'cover';
        avatar.style.backgroundPosition = 'center';
        avatar.style.backgroundColor = 'transparent';
        avatar.innerHTML = '';
    });

    // --- Xử lý sự kiện Logout / Settings ---
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
            settingsPopup.classList.toggle('show');
        };

        btnSettings.addEventListener('click', (e) => {
            toggleSettingsPopup(e);
        });

        btnSettings.addEventListener('keydown', (e) => {
            const key = e.key || e.code;
            if (key === 'Enter' || key === ' ' || key === 'Spacebar') {
                e.preventDefault();
                toggleSettingsPopup(e);
            }
        });
    }

    document.addEventListener('click', (e) => {
        if (settingsPopup && !settingsPopup.contains(e.target) && e.target !== btnSettings) {
            settingsPopup.classList.remove('show');
        }
    });

    if(btnLogoutAction) {
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
                background: '#36393f', color: '#fff'
            }).then((result) => {
                if (result.isConfirmed) {
                    localStorage.clear();
                    if(stompClient) stompClient.disconnect();
                    window.location.href = '/login';
                }
            });
        });
    }

    // ======================================================
    // 1. KẾT NỐI WEBSOCKET (REAL-TIME)
    // ======================================================
    function connectWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Tắt log debug cho đỡ rối

        stompClient.connect({'Authorization': 'Bearer ' + token}, function (frame) {
            console.log('Connected WebSocket: ' + frame);

            // Subscribe kênh tin nhắn riêng tư
            stompClient.subscribe('/user/queue/dm', function (messageOutput) {
                const message = JSON.parse(messageOutput.body);
                handleIncomingMessage(message);
            });

        }, function (error) {
            console.error('WebSocket connection error:', error);
            setTimeout(connectWebSocket, 5000);
        });
    }

    // Xử lý khi nhận tin nhắn Real-time
    function handleIncomingMessage(message) {
        console.log("📩 Tin nhắn mới từ WebSocket:", message);

        // Chỉ xử lý nếu đang mở đúng hội thoại này
        if (currentContext.type === 'DM' && currentContext.id === message.conversationId) {
            // Kiểm tra tin nhắn đã hiển thị trên màn hình chưa dựa vào msg-id để tránh lặp tin của mình
            const existingMsg = document.getElementById(`msg-${message.id}`);
            if (!existingMsg) {
                appendMessageToUI(message);
            }
        } else {
            console.log("Tin nhắn từ hội thoại khác hoặc chưa mở khung chat.");
        }
    }

    // ======================================================
    // 2. LOGIC LOAD BẠN BÈ VÀ SIDEBAR
    // ======================================================
    async function loadFriends() {
        try {
            const response = await authFetch('/api/friends');
            if (response.ok) {
                const friends = await response.json();
                renderFriendListDashboard(friends);
                renderSidebarDMs(friends);
            }
        } catch (error) {
            console.error("Lỗi load friends:", error);
        }
    }

    // Render danh sách ở giữa màn hình (Dashboard)
    function renderFriendListDashboard(friends) {
        const container = document.querySelector('.friend-list-section .empty-list-container');
        const countLabel = document.querySelector('.friend-label');
        if (countLabel) countLabel.innerText = `TẤT CẢ BẠN BÈ — ${friends.length}`;
        if (!container) return;

        if (!friends || friends.length === 0) {
            container.innerHTML = '<div style="padding: 20px; color: #b9bbbe; text-align: center;">Chưa có bạn bè nào. Hãy thêm bạn mới!</div>';
            return;
        }

        let html = '';
        friends.forEach(friend => {
            const avatar = friend.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(friend.displayName)}&background=random&color=fff`;
            const safeName = friend.displayName.replace(/'/g, "\\'");

            html += `
                <div class="friend-item" onclick="openDM(${friend.friendUserId}, '${safeName}')"
                     style="display: flex; align-items: center; padding: 10px; cursor: pointer; border-top: 1px solid #3f4147; border-radius: 4px; margin-bottom: 2px;">
                    <div class="user-avatar" style="width: 32px; height: 32px; background-image: url('${avatar}'); background-size: cover; border-radius: 50%; margin-right: 12px;"></div>
                    <div class="friend-info" style="flex: 1;">
                        <div class="friend-name" style="color: #fff; font-weight: 600;">${friend.displayName}</div>
                        <div class="friend-status" style="font-size: 12px; color: #b9bbbe;">Online</div>
                    </div>
                    <button class="btn-icon" style="background: #2f3136; color: #b9bbbe; border: none; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center;"><i class="fa-solid fa-message"></i></button>
                </div>
            `;
        });
        container.innerHTML = html;
        container.querySelectorAll('.friend-item').forEach(item => {
            item.addEventListener('mouseenter', () => item.style.backgroundColor = 'rgba(79, 84, 92, 0.16)');
            item.addEventListener('mouseleave', () => item.style.backgroundColor = 'transparent');
        });
    }

    // Render danh sách bên Sidebar trái
    function renderSidebarDMs(friends) {
        const dmView = document.getElementById('dm-channels-view');
        if (!dmView) return;

        const channelList = dmView.querySelector('.channel-list');
        let dmContainer = document.getElementById('dm-list-sidebar-container');
        if (!dmContainer) {
            dmContainer = document.createElement('div');
            dmContainer.id = 'dm-list-sidebar-container';
            channelList.appendChild(dmContainer);
        }

        let html = '';
        friends.forEach(friend => {
            const avatar = friend.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(friend.displayName)}&background=random&color=fff`;
            const safeName = friend.displayName.replace(/'/g, "\\'");

            html += `
                <div class="channel-item" onclick="openDM(${friend.friendUserId}, '${safeName}')"
                     style="display: flex; align-items: center; padding: 8px; margin-bottom: 2px; cursor: pointer; color: #8e9297; border-radius: 4px;">
                    <div style="width: 32px; height: 32px; background-image: url('${avatar}'); background-size: cover; border-radius: 50%; margin-right: 10px;"></div>
                    <span style="font-weight: 500; color: #dcddde; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${friend.displayName}</span>
                </div>
            `;
        });
        dmContainer.innerHTML = html;

        dmContainer.querySelectorAll('.channel-item').forEach(item => {
            item.addEventListener('mouseenter', () => { item.style.backgroundColor = 'rgba(79, 84, 92, 0.32)'; item.querySelector('span').style.color = '#fff'; });
            item.addEventListener('mouseleave', () => { item.style.backgroundColor = 'transparent'; item.querySelector('span').style.color = '#dcddde'; });
        });
    }

    // ======================================================
    // 3. LOGIC CHAT VÀ GỬI TIN NHẮN
    // ======================================================

    // Hàm được gọi khi click vào bạn bè
    window.openDM = async function(friendId, friendName) {
        try {
            const resp = await authFetch(`/api/direct-messages/conversation/init?receiverId=${friendId}`, { method: 'POST' });
            if (resp.ok) {
                const conversation = await resp.json();

                currentContext = {
                    type: 'DM',
                    id: conversation.id,
                    receiverId: friendId
                };

                switchToChatView(friendName);
                loadMessages(conversation.id);
            }
        } catch (e) {
            console.error(e);
        }
    };

    function switchToChatView(title) {
        document.getElementById('dm-chat-view').style.display = 'none';
        const chatView = document.getElementById('server-chat-view');
        chatView.style.display = 'flex';

        chatView.querySelector('.top-bar').innerHTML = `
            <i class="fa-solid fa-at" style="color: #72767d; margin-right: 8px;"></i>
            <span style="font-weight: bold; color: white;">${title}</span>
        `;

        const chatArea = chatView.querySelector('.chat-area');
        chatArea.innerHTML = '<div style="padding: 20px; color: #b9bbbe; text-align: center;">Đang tải tin nhắn...</div>';

        const input = chatView.querySelector('.input-area input[type="text"]');
        input.placeholder = `Nhắn tin cho @${title}`;
        input.value = '';
        input.focus();
    }

    async function loadMessages(conversationId) {
        const chatArea = document.querySelector('#server-chat-view .chat-area');
        try {
            const resp = await authFetch(`/api/direct-messages/conversation/${conversationId}?page=0&size=50`);
            if (resp.ok) {
                const data = await resp.json();
                chatArea.innerHTML = '';

                if(data.content.length === 0) {
                    chatArea.innerHTML = '<div style="padding: 20px; color: #b9bbbe; text-align: center;">Hãy bắt đầu cuộc trò chuyện!</div>';
                } else {
                    // Đảo ngược để tin cũ lên trên
                    data.content.slice().reverse().forEach(msg => appendMessageToUI(msg));
                }
                chatArea.scrollTop = chatArea.scrollHeight;
            }
        } catch (e) {
            console.error(e);
        }
    }

    function appendMessageToUI(msg) {
        const chatArea = document.querySelector('#server-chat-view .chat-area');
        if (!chatArea) return;

        // Xóa thông báo trống nếu có
        if (chatArea.innerText.includes('Hãy bắt đầu') || chatArea.innerText.includes('Đang tải')) {
            chatArea.innerHTML = '';
        }

        const time = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const senderName = msg.senderName || 'Unknown';
        const avatarUrl = msg.senderAvatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(senderName)}&background=random&color=fff`;

        const msgDiv = document.createElement('div');
        msgDiv.className = 'message';
        msgDiv.id = `msg-${msg.id}`; // [QUAN TRỌNG] Gán ID để tránh lặp tin nhắn
        msgDiv.innerHTML = `
            <div class="user-avatar" style="width: 40px; height: 40px; margin-right: 16px; background-image: url('${avatarUrl}'); background-size: cover; border-radius: 50%;"></div>
            <div>
                <div class="msg-user" style="font-weight: bold; color: white;">
                    ${senderName}
                    <span style="font-size: 12px; color: #b5bac1; font-weight: normal; margin-left: 8px;">${time}</span>
                </div>
                <div class="msg-content" style="color: #dcddde; margin-top: 4px;">${msg.content}</div>
            </div>
        `;
        chatArea.appendChild(msgDiv);
        chatArea.scrollTop = chatArea.scrollHeight;
    }

    // Xử lý Gửi tin nhắn (Enter)
    const chatInput = document.querySelector('#server-chat-view .input-area input[type="text"]');
    if (chatInput) {
        chatInput.addEventListener('keypress', async function (e) {
            if (e.key === 'Enter') {
                const content = chatInput.value.trim();
                if (!content) return;

                if (currentContext.type === 'DM' && currentContext.receiverId) {
                    try {
                        const payload = {
                            receiverId: currentContext.receiverId,
                            content: content,
                            type: 'TEXT'
                        };

                        const resp = await authFetch('/api/direct-messages', {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });

                        if (resp.ok) {
                            const newMsg = await resp.json();
                            chatInput.value = '';
                            // HIỆN NGAY TIN NHẮN VỪA GỬI QUA AJAX
                            appendMessageToUI(newMsg);
                        }
                    } catch (err) {
                        console.error("Lỗi gửi tin nhắn:", err);
                    }
                }
            }
        });
    }

    // --- KHỞI CHẠY ---
    loadFriends();
    connectWebSocket();
});