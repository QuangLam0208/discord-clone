// CHAT & WEBSOCKET MODULE

// Global variables for Chat module
let channelSubscription = null;

// Helper: Format Date like Discord (Vietnamese)
function formatMessageTime(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    // Return only time in HH:mm format (or 12h if preferred, user screenshot shows 12h with SA/CH)
    // 8:57 SA -> 8:57 AM
    // Using default locale but forcing Vietnam might help, or just simple logic
    return date.toLocaleTimeString('vi-VN', { hour: 'numeric', minute: '2-digit' });
}

// Helper: Create Date Divider
function createDateDivider(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const isToday = date.getDate() === now.getDate() &&
        date.getMonth() === now.getMonth() &&
        date.getFullYear() === now.getFullYear();

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday = date.getDate() === yesterday.getDate() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getFullYear() === yesterday.getFullYear();

    let label = '';
    if (isToday) {
        label = "Hôm nay";
    } else if (isYesterday) {
        label = "Hôm qua";
    } else {
        // DD Tháng MM YYYY
        // Vietnamese locale full date often looks like "Thứ Sáu, 20 tháng 7, 2025"
        // User screenshot: "20 Tháng Bảy 2025"
        const day = date.getDate();
        const monthNames = ["Tháng Một", "Tháng Hai", "Tháng Ba", "Tháng Tư", "Tháng Năm", "Tháng Sáu",
            "Tháng Bảy", "Tháng Tám", "Tháng Chín", "Tháng Mười", "Tháng Mười Một", "Tháng Mười Hai"];
        // Format: 22 Tháng Bảy 2025
        const year = date.getFullYear();
        label = `${day} ${monthNames[date.getMonth()]} ${year}`;
    }

    const div = document.createElement('div');
    div.className = 'date-divider';
    div.innerHTML = `<span>${label}</span>`;
    return div;
}

function removeMessageElement(messageId) {
    const el = document.getElementById('message-' + messageId);
    if (el) {
        el.style.transition = 'opacity 0.3s ease';
        el.style.opacity = '0';
        setTimeout(() => el.remove(), 300);
    }
}

// 1. Create Message Element
function createMessageElement(msg) {
    const div = document.createElement('div');
    div.className = 'message';
    div.id = 'message-' + msg.id;
    div.dataset.timestamp = new Date(msg.createdAt).getTime(); // Add timestamp for Divider logic
    // Format time: Discord style (HH:mm) uses new helper
    const time = formatMessageTime(msg.createdAt);

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

            let lastDate = null;
            sortedMsgs.forEach(msg => {
                const msgDate = new Date(msg.createdAt).setHours(0, 0, 0, 0);
                if (lastDate === null || msgDate !== lastDate) {
                    chatArea.appendChild(createDateDivider(msg.createdAt));
                    lastDate = msgDate;
                }
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
// Flag to prevent race conditions
state.isConnecting = false;

function connectChannelSocket(channelId) {
    if (state.stompClient && state.stompClient.connected) {
        subscribeToChannel(channelId);
        return;
    }

    if (state.isConnecting) {
        // Queue the subscription or just wait?
        // Simple retry logic: wait 500ms and try again
        console.log("Socket is connecting... waiting to subscribe", channelId);
        setTimeout(() => connectChannelSocket(channelId), 500);
        return;
    }

    state.isConnecting = true;
    const socket = new SockJS('/ws');
    state.stompClient = Stomp.over(socket);
    state.stompClient.debug = null;

    const token = localStorage.getItem('accessToken');
    if (!token) {
        console.warn("No token found, redirecting to login.");
        window.location.href = '/login';
        return;
    }

    state.stompClient.connect({ 'Authorization': `Bearer ${token}` }, function (frame) {
        console.log('Connected to WebSocket');
        state.isConnecting = false;
        subscribeToChannel(channelId);
    }, function (error) {
        console.error('WebSocket Error:', error);
        state.isConnecting = false;
        // Auto reconnect sau 5s nếu mất kết nối
        setTimeout(() => {
            console.log("Reconnecting WebSocket...");
            connectChannelSocket(channelId);
        }, 5000);
    });
}

// 1. Export connect function if needed, or ensuring subscribe handles connection
window.connectChannelSocket = connectChannelSocket; // Expose if needed

function subscribeToChannel(channelId) {
    if (!channelId) return;

    // Safety check: specific fix for "Cannot read properties of null (reading 'subscribe')"
    if (!state.stompClient || !state.stompClient.connected) {
        console.log("WebSocket not ready. connecting...", channelId);
        connectChannelSocket(channelId);
        return;
    }

    // Hủy đăng ký channel cũ nếu có
    if (channelSubscription) {
        channelSubscription.unsubscribe();
    }

    // Subscribe channel mới: /topic/channel/{id}
    channelSubscription = state.stompClient.subscribe(`/topic/channel/${channelId}`, function (message) {
        const receivedMsg = JSON.parse(message.body);
        // Render tin nhắn mới nhận được
        const chatArea = document.querySelector('.chat-area');
        if (!chatArea) return;

        if (receivedMsg.type === 'DELETE_MESSAGE') {
            removeMessageElement(receivedMsg.messageId);
            return; // Dừng xử lý, không render gì thêm
        }

        if (receivedMsg.type === 'RESTORE_MESSAGE') {
            // Xóa cũ nếu tồn tại (tránh trùng lặp) rồi render lại như tin mới
            removeMessageElement(receivedMsg.id);
            // Sau đó để code chạy tiếp xuống dưới để render lại tin nhắn này
        }

        // Nếu đây là tin nhắn đầu tiên (đang hiện text "Chưa có tin nhắn...") -> clear đi
        if (chatArea.innerText.includes("Chưa có tin nhắn")) chatArea.innerHTML = '';

        // Check if we need a divider
        const msgDate = new Date(receivedMsg.createdAt).setHours(0, 0, 0, 0);

        // Find last message date from DOM ?? simpler: compare with "today" if real-time
        // Or check last divider? 
        // Best approach: Check the timestamp of the last .message element
        const lastMsgEl = chatArea.querySelector('.message:last-child');
        let lastDate = null;
        if (lastMsgEl) {
            // We don't store raw date on DOM, but we can infer or store it.
            // actually, easier: if it's a new day vs "now", append divider.
            // But if user scrolls up? this is only for new incoming messages essentially "now".
            // So if now is different day than the last received message.
            // Let's assume real-time messages are "now". But wait, `receivedMsg` has `createdAt`.
            // Let's compare `receivedMsg.createdAt` with the last message's time. 
            // We can't easily get last message time from DOM unless we store it.
            // Hack: look at the last divider? No.
            // Correct way: The incoming message IS sorted by time (usually).
            // Let's just assume we need to check against the previous element in DOM.
        }

        // Logic: Always check if the date differs from the previous visible message.
        // Implementation:
        // Get all dividers. Get last divider. Parse text? Too hard.
        // Simplified: Store lastRenderedDate in a global/state? Vulnerable to channel switch.
        // Better: When appending, check if the last element in chatArea is from same day.
        // We can attach data-date to .message elements?
        /* 
           We will modify createMessageElement to add data-timestamp to the div.
        */

        let shouldAddDivider = true;

        // Try to find the last message element
        const messages = chatArea.querySelectorAll('.message');
        if (messages.length > 0) {
            const lastMsg = messages[messages.length - 1];
            // We need to add data-timestamp to message elements to make this robust
            // BUT, since we can't easily modify createMessageElement in this same tool call safely without complex referencing,
            // we will rely on creating the message element temporarily or just state.
            // Actually, I should update createMessageElement to include data-timestamp.
        }

        // Wait, I can update createMessageElement in this tool call too! I will do that.

        // Revised Subscribe Logic assuming createMessageElement adds data-timestamp
        const lastMessage = chatArea.lastElementChild; // Could be message or divider
        if (lastMessage && lastMessage.classList.contains('message')) {
            const lastTs = parseInt(lastMessage.dataset.timestamp);
            if (lastTs) {
                const lastDateObj = new Date(lastTs);
                if (lastDateObj.setHours(0, 0, 0, 0) === msgDate) {
                    shouldAddDivider = false;
                }
            }
        }

        if (shouldAddDivider) {
            chatArea.appendChild(createDateDivider(receivedMsg.createdAt));
        }

        const msgEl = createMessageElement(receivedMsg);
        chatArea.appendChild(msgEl);
        scrollToBottom();
    });

    console.log(`Subscribed to /topic/channel/${channelId}`);
}
// Export globally
window.subscribeToChannel = subscribeToChannel;

// 5. Send Message (Global)
window.sendMessage = function (content, attachments = []) {
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