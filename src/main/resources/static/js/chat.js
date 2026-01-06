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
    if (msg.deleted) return null; // Don't render deleted messages initially

    const div = document.createElement('div');
    div.className = 'message';
    div.id = 'message-' + msg.id;
    div.dataset.timestamp = new Date(msg.createdAt).getTime(); // Add timestamp for Divider logic
    // Format time: Discord style (HH:mm) uses new helper
    const time = formatMessageTime(msg.createdAt);

    const senderName = msg.senderName || msg.senderId;
    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(senderName)}&background=random&color=fff&size=40`;

    // Check ownership for Edit/Delete actions
    // Assume state.currentUser is populated (from home.js/loadMe)
    const currentUserId = state.currentUser ? state.currentUser.id : (localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null);
    const isOwner = currentUserId && (msg.senderId === currentUserId);

    // Permission check
    const perms = state.serverPermissions || new Set();
    const canManage = perms.has('MANAGE_MESSAGES') || perms.has('ADMIN') || perms.has('ADMINISTRATOR');

    // Controls HTML
    let controlsHtml = '';
    // Allow if owner OR has MANAGE_MESSAGES (User requested ability to edit others too)
    if ((isOwner || canManage) && !msg.deleted) {
        controlsHtml = `
            <div class="message-actions">
                <i class="fas fa-pen" onclick="enableEditMessage('${msg.id}')" title="Chỉnh sửa"></i>
                <i class="fas fa-trash trash-icon" onclick="confirmDeleteMessage('${msg.id}')" title="Xóa"></i>
            </div>
        `;
    }

    // Context Menu Trigger
    div.addEventListener('contextmenu', (e) => {
        if ((!isOwner && !canManage) || msg.deleted) return;
        e.preventDefault();
        showContextMenu(e.pageX, e.pageY, msg.id);
    });



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
            <div class="msg-content" id="msg-content-${msg.id}">
                ${msg.deleted ? '<em style="color: #72767d;">Tin nhắn đã bị xóa.</em>' : (msg.content || '')}
                ${(msg.edited && !msg.deleted) ? '<span style="font-size: 10px; color: #72767d; margin-left: 4px;">(đã chỉnh sửa)</span>' : ''}
            </div>
            ${attachmentsHtml}
        </div>
        ${controlsHtml}
    `;

    // Hover effect for actions
    if ((isOwner || canManage) && !msg.deleted) {
        div.style.position = 'relative';
        // CSS will handle hover display via .message:hover .message-actions
    }


    return div;
}

// Global Context Menu Logic
const contextMenuEl = document.createElement('div');
contextMenuEl.className = 'context-menu';
document.body.appendChild(contextMenuEl);

function showContextMenu(x, y, msgId) {
    contextMenuEl.innerHTML = `
        <div class="context-menu-item" onclick="enableEditMessage('${msgId}'); hideContextMenu()">
            <span>Chỉnh sửa tin nhắn</span>
            <i class="fas fa-pen"></i>
        </div>
        <div class="context-menu-item delete" onclick="confirmDeleteMessage('${msgId}'); hideContextMenu()">
            <span>Xóa tin nhắn</span>
            <i class="fas fa-trash"></i>
        </div>
    `;

    // Position check to avoid overflow
    const winWidth = window.innerWidth;
    const winHeight = window.innerHeight;

    let left = x;
    let top = y;

    if (x + 200 > winWidth) left = winWidth - 210;
    if (y + 100 > winHeight) top = winHeight - 110;

    contextMenuEl.style.left = `${left}px`;
    contextMenuEl.style.top = `${top}px`;
    contextMenuEl.classList.add('active');
}

function hideContextMenu() {
    contextMenuEl.classList.remove('active');
}

// Hide menu on click outside
document.addEventListener('click', (e) => {
    if (!contextMenuEl.contains(e.target)) {
        hideContextMenu();
    }
});

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
                if (msg.deleted) return; // Skip deleted

                const msgDate = new Date(msg.createdAt).setHours(0, 0, 0, 0);
                if (lastDate === null || msgDate !== lastDate) {
                    chatArea.appendChild(createDateDivider(msg.createdAt));
                    lastDate = msgDate;
                }
                const msgEl = createMessageElement(msg);
                if (msgEl) chatArea.appendChild(msgEl);
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

    // Safety check
    if (!state.stompClient || !state.stompClient.connected) {
        console.log("WebSocket not ready. connecting...", channelId);
        connectChannelSocket(channelId);
        return;
    }

    if (channelSubscription) {
        channelSubscription.unsubscribe();
    }

    channelSubscription = state.stompClient.subscribe(`/topic/channel/${channelId}`, function (message) {
        const receivedMsg = JSON.parse(message.body);
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

        // 1. DELETE Handling
        if (receivedMsg.deleted) {
            const existingMsgEl = document.getElementById(`msg-content-${receivedMsg.id}`)?.closest('.message');
            if (existingMsgEl) existingMsgEl.remove();
            return;
        }

        // 2. EDIT Handling (Update existing)
        const existingContentEl = document.getElementById(`msg-content-${receivedMsg.id}`);
        if (existingContentEl) {
            existingContentEl.innerHTML = `
                ${receivedMsg.content}
                <span style="font-size: 10px; color: #72767d; margin-left: 4px;">(đã chỉnh sửa)</span>
            `;
            return;
        }

        // 3. NEW MESSAGE Handling
        // Check for date divider
        const msgDate = new Date(receivedMsg.createdAt).setHours(0, 0, 0, 0);
        let shouldAddDivider = true;

        const lastMessage = chatArea.querySelector('.message:last-of-type');
        if (lastMessage) {
            const lastTs = parseInt(lastMessage.dataset.timestamp);
            if (lastTs) {
                const lastDate = new Date(lastTs).setHours(0, 0, 0, 0);
                if (lastDate === msgDate) shouldAddDivider = false;
            }
        }

        if (shouldAddDivider) {
            chatArea.appendChild(createDateDivider(receivedMsg.createdAt));
        }

        const msgEl = createMessageElement(receivedMsg);
        if (msgEl) {
            chatArea.appendChild(msgEl);
            scrollToBottom();
        }
    });

    console.log(`Subscribed to /topic/channel/${channelId}`);
}

// 6. Edit & Delete Logic

window.enableEditMessage = function (msgId) {
    const contentEl = document.getElementById(`msg-content-${msgId}`);
    if (!contentEl) return;

    // Avoid double edit
    if (contentEl.querySelector('input')) return;

    // Get raw text (remove "(đã chỉnh sửa)" if exists)
    let currentText = contentEl.innerText.replace('(đã chỉnh sửa)', '').trim();
    if (currentText === "Tin nhắn đã bị xóa.") return;

    contentEl.innerHTML = `
        <div style="margin-top: 5px;">
            <input type="text" id="edit-input-${msgId}" value="${currentText.replace(/"/g, '&quot;')}" 
                style="width: 100%; padding: 8px; border-radius: 4px; border: none; background: #40444b; color: #dcddde; outline: none; box-sizing: border-box;"
                onkeydown="if(event.key === 'Enter') saveEditMessage('${msgId}')">
            <div style="font-size: 12px; margin-top: 4px;">
                <span style="color: #72767d;">Escape to <a href="#" onclick="cancelEditMessage('${msgId}', '${currentText.replace(/'/g, "\\'")}', event)" style="color: #00b0f4; text-decoration: none;">cancel</a> • Enter to <a href="#" onclick="saveEditMessage('${msgId}', event)" style="color: #00b0f4; text-decoration: none;">save</a></span>
            </div>
        </div>
    `;

    // Focus, move cursor to end
    setTimeout(() => {
        const input = document.getElementById(`edit-input-${msgId}`);
        if (input) {
            input.focus();
            input.setSelectionRange(input.value.length, input.value.length);
            // Escape to cancel
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') cancelEditMessage(msgId, currentText, e);
            });
        }
    }, 0);
}

window.cancelEditMessage = function (msgId, originalText, e) {
    if (e) e.preventDefault();
    const contentEl = document.getElementById(`msg-content-${msgId}`);
    if (!contentEl) return;
    contentEl.innerHTML = originalText;
}

window.saveEditMessage = async function (msgId, e) {
    if (e) e.preventDefault();
    const input = document.getElementById(`edit-input-${msgId}`);
    if (!input) return;

    const newContent = input.value.trim();
    if (!newContent) {
        alert("Tin nhắn không được để trống!");
        return;
    }

    const contentEl = document.getElementById(`msg-content-${msgId}`);
    // Optimistic update: Show "saving..." immediately BEFORE API call
    contentEl.innerHTML = `
        ${newContent}
        <span style="font-size: 10px; color: #72767d;">(đang lưu...)</span>
    `;

    // Call API
    try {
        await Api.put(`/api/messages/${msgId}`, { content: newContent });

        contentEl.innerHTML = `
            ${newContent}
            <span style="font-size: 10px; color: #72767d; margin-left: 4px;">(đã chỉnh sửa)</span>
        `;
    } catch (err) {
        console.error("Failed to edit message", err);
        alert("Không thể sửa tin nhắn: " + (err.message || 'Lỗi không xác định'));

        // On error, let user try again by restoring input
        enableEditMessage(msgId);
        const retryInput = document.getElementById(`edit-input-${msgId}`);
        if (retryInput) retryInput.value = newContent;
    }
}

window.confirmDeleteMessage = function (msgId) {
    Swal.fire({
        title: 'Xóa tin nhắn',
        text: "Bạn có chắc chắn muốn xóa tin nhắn này không?",
        icon: null, // Custom styled
        showCancelButton: true,
        confirmButtonColor: '#ed4245',
        cancelButtonColor: '#4f545c', // Discord grey
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy bỏ',
        background: '#36393f',
        color: '#dcddde', // Light gray text
        customClass: {
            title: 'swal2-title-discord',
            popup: 'swal2-popup-discord'
        }
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                // Optimistic UI update: Remove immediately
                const existingMsgEl = document.getElementById(`msg-content-${msgId}`)?.closest('.message');
                if (existingMsgEl) existingMsgEl.remove();

                await Api.delete(`/api/messages/${msgId}`);
                // WebSocket will also broadcast delete, but we handled it optimistically.
                // The subscriber logic checks if element exists, so it won't error if already removed.
            } catch (err) {
                console.error("Failed to delete message", err);
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: 'Không thể xóa tin nhắn.',
                    background: '#36393f',
                    color: '#fff'
                });
                // In a perfect world we would revert the UI removal here if failed.
            }
        }
    });
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