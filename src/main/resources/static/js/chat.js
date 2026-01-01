// CHAT & WEBSOCKET MODULE

// Global variables for Chat module
let channelSubscription = null;

// 1. Create Message Element
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

// 2. Scroll to Bottom
function scrollToBottom() {
    const chatArea = document.querySelector('.chat-area');
    if (chatArea) chatArea.scrollTop = chatArea.scrollHeight;
}

// 3. Load Messages
async function loadMessages(channelId) {
    const chatArea = document.querySelector('.chat-area');
    if (!chatArea) return;

    chatArea.innerHTML = '<div style="color: #aaa; text-align: center; margin-top: 20px;">Đang tải tin nhắn...</div>';

    try {
        // GET /api/channels/{id}/messages?page=0&size=50
        const messages = await Api.get(`/api/channels/${channelId}/messages?size=50`);

        chatArea.innerHTML = ''; // Clear loading

        if (messages && Array.isArray(messages)) {
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

// 4. WebSocket Logic
function connectChannelSocket(channelId) {
    // Nếu chưa kết nối Socket tổng -> connect
    if (!state.stompClient || !state.stompClient.connected) {
        const socket = new SockJS('/ws');
        state.stompClient = Stomp.over(socket);
        state.stompClient.debug = null; // Tắt log debug cho gọn

        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.warn("No token found, redirecting to login.");
            window.location.href = '/login';
            return;
        }

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
