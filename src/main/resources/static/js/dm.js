const DM = (() => {
    const st = {
        currentUserId: null,
        currentUser: null,
        friends: [],
        blockedUsers: [],
        conversations: [],
        activeFriendId: null,
        activeFriendName: '',
        activeFriendAvatar: null,
        conversationId: null,
        displayedMsgIds: new Set(),
        unreadCounts: new Map(),
        pendingRetries: new Map(),

        replyingToId: null,
        filesToUpload: []
    };

    function highlightServerLogo() {
        document.querySelectorAll('.sidebar .sidebar-item').forEach(e => e.classList.remove('active'));
        const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
        if (homeLogo) homeLogo.classList.add('active');
    }

    function goHome() {
        st.activeFriendId = null;
        st.activeFriendName = '';
        st.activeFriendAvatar = null;
        st.conversationId = null;

        DMUI.showDashboard();
        DMUI.highlightFriendsButton();
        highlightServerLogo();

        console.log('[DM] Switched to Dashboard');
    }

    async function onSelectFriend(friendId, friendName, friendAvatar = null) {
        st.activeFriendId = friendId;
        let friendObj = st.friends.find(f => String(f.friendUserId) === String(friendId));
        if (!friendObj) {
            friendObj = st.conversations.find(c => String(c.friendUserId) === String(friendId));
        }

        const realDisplayName = friendObj ? (friendObj.displayName || friendObj.friendUsername) : (friendName || 'User ' + friendId);
        const realAvatar = friendObj ? friendObj.avatarUrl : friendAvatar;
        const realUsername = friendObj ? friendObj.friendUsername : (friendName || 'User' + friendId);

        st.activeFriendName = realDisplayName;
        st.activeFriendAvatar = realAvatar;
        st.unreadCounts.set(friendId, 0);

        let friendInfo = st.conversations.find(c => c.friendUserId == friendId);
        if (!friendInfo) {
            friendInfo = {
                friendUserId: friendId,
                displayName: realDisplayName,
                friendUsername: realUsername,
                avatarUrl: realAvatar
            };
        }

        DMUI.setActiveFriendName(st.activeFriendName);
        DMUI.showConversation();
        highlightServerLogo();

        try {
            const conv = await DMApi.getOrCreateConversation(friendId);
            st.conversationId = String(conv.id);

            if (window.DMWS && DMWS.subscribeToConversation) {
                DMWS.subscribeToConversation(st.conversationId, onConversationEvent);
            }

            const exists = st.conversations.some(c => String(c.conversationId) === st.conversationId);
            if (!exists) {
                const newConvoItem = {
                    conversationId: conv.id,
                    friendUserId: Number(friendId),
                    displayName: realDisplayName,
                    friendUsername: realUsername,
                    avatarUrl: realAvatar,
                    unreadCount: 0,
                    updatedAt: conv.updatedAt || new Date().toISOString()
                };
                st.conversations.unshift(newConvoItem);
                DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);

                setTimeout(() => {
                    const newItemDom = document.getElementById(`dm-item-${friendId}`);
                    if (newItemDom) {
                        DMUI.resetActiveItems();
                        newItemDom.classList.add('active');
                    }
                }, 50);
            } else {
                DMUI.updateSidebarItem(friendId, false);
            }

            const isFriend = st.friends.some(f => f.friendUserId == friendId);
            const isBlocked = st.blockedUsers.some(f => String(f.friendUserId) === String(friendId));
            let status = isBlocked ? 'BLOCKED' : (isFriend ? 'FRIEND' : 'NOT_FRIEND');

            let msgs = [];
            if (status === 'NOT_FRIEND') {
                const [messages, outboundRequests, inboundRequests] = await Promise.all([
                    DMApi.fetchMessages(st.conversationId, 0, 50),
                    DMApi.listOutboundRequests().catch(() => []),
                    DMApi.listInboundRequests().catch(() => [])
                ]);
                msgs = messages;
                const hasSent = outboundRequests.some(r => String(r.recipientId) === String(friendId));
                const hasReceived = inboundRequests.some(r => String(r.senderId) === String(friendId));
                if (hasReceived) status = 'RECEIVED_REQUEST';
                else if (hasSent) status = 'SENT_REQUEST';
            } else {
                msgs = await DMApi.fetchMessages(st.conversationId, 0, 50);
            }

            const avatarMap = {
                [st.currentUserId]: st.currentUser?.avatarUrl,
                [friendId]: st.activeFriendAvatar
            };

            DMUI.renderMessages(msgs, st.currentUserId, st.displayedMsgIds, friendInfo, status, avatarMap, st.activeFriendName);

        } catch (e) {
            console.error('Error loading conversation:', e);
        }
    }

    function onConversationEvent(payload) {
        // Admin/User xóa tin nhắn
        if (payload.type === 'DELETE_MESSAGE') {
            DMUI.removeMessageElement(payload.messageId);
            st.displayedMsgIds.delete(String(payload.messageId));
            return;
        }
        // Admin khôi phục tin nhắn
        if (payload.type === 'RESTORE_MESSAGE') {
            DMUI.removeMessageElement(payload.id); // Xóa bản cũ nếu lỗi
            st.displayedMsgIds.delete(String(payload.id)); // Cho phép vẽ lại

            if (String(payload.conversationId) === String(st.conversationId)) {
                // Bổ sung avatarMap và friendName để hiển thị đầy đủ
                const avatarMap = {
                    [st.currentUserId]: st.currentUser?.avatarUrl,
                    [payload.senderId]: st.activeFriendAvatar
                };
                DMUI.appendMessage(payload, st.currentUserId, st.displayedMsgIds, avatarMap, st.activeFriendName);
            }
            return;
        }
        // User chỉnh sửa tin nhắn (Realtime Update UI)
        if (payload.id && !payload.type) { // Tin nhắn thường hoặc tin nhắn đã sửa
            const existingEl = document.getElementById(`msg-${payload.id}`);

            if (existingEl) {
                const contentDiv = existingEl.querySelector('div[style*="white-space"]');
                if (contentDiv) {
                    contentDiv.innerHTML = (payload.content || '').replace(/[&<>"']/g, s => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]));
                }
                const metaDiv = existingEl.querySelector('div[style*="font-size:12px"]');
                if (metaDiv && !metaDiv.innerHTML.includes('(đã chỉnh sửa)') && payload.edited) {
                    metaDiv.insertAdjacentHTML('beforeend', '<span style="font-size: 10px; color: #b9bbbe; margin-left: 4px;">(đã chỉnh sửa)</span>');
                }
            } else {
                // Tin nhắn mới -> có thể vẽ bổ sung nếu cần
            }
        }
    }

    function prepareReply(msgId, senderId, content) {
        st.replyingToId = msgId;
        const { dmInput } = DMUI.els();
        const replyPreview = document.getElementById('reply-preview-bar');
        if (replyPreview) {
            replyPreview.style.display = 'flex';
            const replyText = document.getElementById('reply-to-user');
            if (replyText) replyText.textContent = content.substring(0, 30) + (content.length > 30 ? '...' : '');
        }
        dmInput.focus();
    }

    function cancelReply() {
        st.replyingToId = null;
        const replyPreview = document.getElementById('reply-preview-bar');
        if (replyPreview) replyPreview.style.display = 'none';
    }

    async function onSend() {
        try {
            const { dmInput } = DMUI.els();
            if (!dmInput || !st.activeFriendId) return;

            let content = dmInput.value.trim();
            const filesToProcess = [...st.filesToUpload]; // Snapshot

            if (!content && filesToProcess.length === 0) return;

            let attachments = [];
            let uploadFailed = false;

            if (filesToProcess.length > 0) {
                for (let i = 0; i < filesToProcess.length; i++) {
                    try {
                        const formData = new FormData();
                        formData.append('file', filesToProcess[i]);
                        const res = await fetch('/api/upload', {
                            method: 'POST',
                            headers: { 'Authorization': `Bearer ${DMApi.getToken()}` },
                            body: formData
                        });
                        if (res.ok) {
                            const data = await res.json();
                            if (data.url) attachments.push(data.url);
                        } else {
                            console.error('Upload failed status:', res.status);
                            uploadFailed = true;
                        }
                    } catch (e) {
                        console.error("Upload exception", e);
                        uploadFailed = true;
                    }
                }

                if (uploadFailed) {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi tải tệp',
                        text: 'Một số tệp không thể tải lên.',
                        background: '#36393f',
                        color: '#fff'
                    });
                    if (!content && attachments.length === 0) return;
                }

                st.filesToUpload = [];
                DMUI.renderAttachmentPreview([]);
            }

            dmInput.value = '';
            dmInput.focus();

            await executeSendMessage(st.activeFriendId, content, attachments, st.replyingToId);
            cancelReply();

        } catch (err) {
            console.error('[onSend] Critical error:', err);
            Swal.fire({ title: 'Lỗi', text: 'Không thể gửi tin nhắn: ' + err.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }

    async function executeSendMessage(receiverId, content, attachments = [], replyToId = null, existingTempId = null) {
        const tempId = existingTempId || ('temp-' + Date.now() + Math.random().toString(36).substr(2, 9));

        const tempMsg = {
            id: null,
            tempId: tempId,
            senderId: st.currentUserId,
            receiverId: receiverId,
            content: content,
            attachments: attachments,
            replyToId: replyToId,
            replyToMessage: replyToId ? { content: 'Đang trả lời...' } : null,
            createdAt: new Date().toISOString(),
            status: 'pending'
        };

        const avatarMap = {
            [st.currentUserId]: st.currentUser?.avatarUrl,
            [receiverId]: st.activeFriendAvatar
        };

        if (!existingTempId) DMUI.appendMessage(tempMsg, st.currentUserId, null);
        try {
            const res = await fetch('/api/direct-messages', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${DMApi.getToken()}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ receiverId, content, attachments, replyToId })
            });

            if (!res.ok) throw new Error("Send failed");
            const sent = await res.json();

            DMUI.replaceTempMessage(tempId, sent, st.currentUserId, avatarMap, st.activeFriendName);
            if (sent.id) st.displayedMsgIds.add(String(sent.id));

            if (!st.conversationId && sent.conversationId) {
                st.conversationId = String(sent.conversationId);
            }
            if (sent.conversationId) {
                const sentConvIdStr = String(sent.conversationId);
                const existsInSidebar = st.conversations.some(c => String(c.conversationId) === sentConvIdStr);
                if (!existsInSidebar) {
                    const foundFriend = st.friends.find(f => String(f.friendUserId) === String(receiverId)) || {};
                    const newConvoItem = {
                        conversationId: sent.conversationId,
                        friendUserId: Number(receiverId),
                        displayName: foundFriend.displayName || st.activeFriendName || ('User ' + receiverId),
                        friendUsername: foundFriend.friendUsername || st.activeFriendName,
                        avatarUrl: foundFriend.avatarUrl || null,
                        unreadCount: 0,
                        updatedAt: new Date().toISOString()
                    };
                    st.conversations.unshift(newConvoItem);
                    DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                    const newItemDom = document.getElementById(`dm-item-${receiverId}`);
                    if (newItemDom) {
                        newItemDom.classList.add('active');
                        DMUI.updateSidebarItem(receiverId, false);
                    }
                }
            }
            st.pendingRetries.delete(tempId);
        } catch (e) {
            console.error('[Send Fail]', e);
            DMUI.markMessageError(tempId);
            st.pendingRetries.set(tempId, { receiverId, content });
        }
    }

    function retryMessage(tempId) {
        const data = st.pendingRetries.get(tempId);
        if (data) {
            const tempEl = document.getElementById(`msg-${tempId}`);
            if (tempEl) tempEl.style.opacity = '0.5';
            executeSendMessage(data.receiverId, data.content, data.attachments, data.replyToId, tempId);
        }
    }


    function removeAttachment(index) {
        if (index >= 0 && index < st.filesToUpload.length) {
            st.filesToUpload.splice(index, 1);
            DMUI.renderAttachmentPreview(st.filesToUpload);
        }
    }

    function scrollToMessage(msgId) {
        if (!msgId || msgId === 'undefined' || msgId === 'null') return;
        const elId = `msg-${msgId}`;
        const el = document.getElementById(elId);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.style.backgroundColor = 'rgba(88, 101, 242, 0.3)'; // Highlight
            setTimeout(() => {
                el.style.backgroundColor = 'transparent';
            }, 2000);
        } else {
            console.warn(`Message ${msgId} not loaded or found.`);
        }
    }

    // --- HÀM REAL-TIME (UPDATE MỚI NHẤT) ---
    async function onIncomingMessage(msg) {
        // Nếu payload là sự kiện có type -> xử lý như ở onConversationEvent
        if (msg && msg.type === 'DELETE_MESSAGE') {
            DMUI.removeMessageElement(msg.messageId);
            st.displayedMsgIds.delete(String(msg.messageId));
            return;
        }
        if (msg && msg.type === 'RESTORE_MESSAGE') {
            DMUI.removeMessageElement(msg.id);
            st.displayedMsgIds.delete(String(msg.id));
            if (String(msg.conversationId) === String(st.conversationId)) {
                const avatarMap = {
                    [st.currentUserId]: st.currentUser?.avatarUrl,
                    [msg.senderId]: st.activeFriendAvatar
                };
                DMUI.appendMessage(msg, st.currentUserId, st.displayedMsgIds, avatarMap, st.activeFriendName);
            }
            return;
        }

        const msgIdStr = String(msg.id);
        const domId = `msg-${msgIdStr}`;
        const existingEl = document.getElementById(domId);

        const msgConvId = String(msg.conversationId || '');
        const currentConvId = String(st.conversationId || '');

        // 1. UPDATE: Nếu tin nhắn đã có trên màn hình (Edit/Delete) -> Update ngay
        if (existingEl) {
            console.log('[Socket] Updating existing message:', msg);

            const avatarMap = {
                [st.currentUserId]: st.currentUser?.avatarUrl,
                [st.activeFriendId]: st.activeFriendAvatar
            };
            if (String(msg.senderId) === String(st.currentUserId)) {
                avatarMap[msg.senderId] = st.currentUser?.avatarUrl;
            }

            DMUI.replaceTempMessage(msgIdStr, msg, st.currentUserId, avatarMap, st.activeFriendName);
            return;
        }

        // 2. Nếu tin mới của mình -> Bỏ qua
        if (String(msg.senderId) === String(st.currentUserId)) {
            return;
        }

        // 3. Tin mới của người khác
        let conversationExists = st.conversations.some(c => String(c.conversationId) === msgConvId);
        if (!conversationExists) {
            try {
                const newConvos = await DMApi.fetchAllConversations();
                st.conversations = newConvos;
                DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                conversationExists = true;
            } catch (e) { console.error('Error syncing new conversation:', e); }
        }

        if (st.activeFriendId && msgConvId === currentConvId) {
            const avatarMap = {
                [st.currentUserId]: st.currentUser?.avatarUrl,
                [msg.senderId]: st.activeFriendAvatar
            };
            DMUI.appendMessage(msg, st.currentUserId, st.displayedMsgIds, avatarMap, st.activeFriendName);
        } else {
            const friendId = msg.senderId;
            if (!msg.deleted) {
                st.unreadCounts.set(friendId, (st.unreadCounts.get(friendId) || 0) + 1);
                if (conversationExists) {
                    const idx = st.conversations.findIndex(c => String(c.conversationId) === msgConvId);
                    if (idx >= 0) {
                        if (idx > 0) {
                            const [item] = st.conversations.splice(idx, 1);
                            st.conversations.unshift(item);
                            DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                        }
                        setTimeout(() => DMUI.updateSidebarItem(friendId, true), 10);
                    }
                }
            }
        }
    }

    // --- ACTIONS ---

    function handleEditMessage(msgId, content) {
        Swal.fire({
            title: 'Chỉnh sửa tin nhắn',
            input: 'text',
            inputValue: content,
            showCancelButton: true,
            confirmButtonText: 'Lưu',
            cancelButtonText: 'Hủy',
            background: '#36393f',
            color: '#fff',
            preConfirm: (newContent) => {
                if (!newContent || !newContent.trim()) {
                    Swal.showValidationMessage('Nội dung không thể trống');
                    return false;
                }
                return newContent;
            }
        }).then(async (result) => {
            if (result.isConfirmed) {
                try {
                    await DMApi.editMessage(msgId, result.value);
                } catch (e) {
                    Swal.fire('Lỗi', e.message, 'error');
                }
            }
        });
    }

    async function handleMoreOptions(msgId, senderId, content) {
        const isMine = String(senderId) === String(st.currentUserId);
        const inputOptions = {};

        inputOptions['reply'] = 'Trả lời';
        inputOptions['copy'] = 'Sao chép văn bản';

        if (isMine) {
            inputOptions['edit'] = 'Chỉnh sửa tin nhắn';
            inputOptions['delete'] = 'Xóa tin nhắn';
        }

        const { value: action } = await Swal.fire({
            title: 'Tùy chọn',
            input: 'radio',
            inputOptions: inputOptions,
            inputValidator: (value) => !value && 'Bạn cần chọn một hành động',
            background: '#36393f',
            color: '#fff',
            confirmButtonColor: '#5865F2',
            confirmButtonText: 'Chọn',
            showCancelButton: true,
            cancelButtonText: 'Hủy'
        });

        if (!action) return;

        if (action === 'reply') {
            DM.prepareReply(msgId, senderId, content);
        }
        else if (action === 'copy') {
            navigator.clipboard.writeText(content).then(() => {
                Swal.fire({ icon: 'success', title: 'Đã sao chép', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 1500, background: '#36393f', color: '#fff' });
            });
        }
        else if (action === 'edit') {
            handleEditMessage(msgId, content);
        }
        else if (action === 'delete') {
            if (confirm('Bạn có chắc muốn xóa tin nhắn này?')) {
                try {
                    await DMApi.deleteMessage(msgId);
                } catch (e) { alert(e.message); }
            }
        }
    }

    async function handleBlock(uid) {
        const result = await Swal.fire({ title: 'Chặn người dùng?', text: "Bạn sẽ không nhận được tin nhắn từ họ nữa.", icon: 'warning', showCancelButton: true, confirmButtonColor: '#ed4245', cancelButtonColor: '#7289da', confirmButtonText: 'Chặn', cancelButtonText: 'Hủy', background: '#36393f', color: '#fff' });
        if (result.isConfirmed) { try { await DMApi.blockUser(uid); try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch (e) { } st.friends = await DMApi.fetchFriends(); if (String(st.activeFriendId) === String(uid)) { await onSelectFriend(uid); } Swal.fire({ icon: 'success', title: 'Đã chặn', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' }); } catch (e) { Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' }); } }
    }
    async function handleUnblock(uid) {
        try { await DMApi.unblockUser(uid); try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch (e) { } if (String(st.activeFriendId) === String(uid)) { await onSelectFriend(uid); } Swal.fire({ icon: 'success', title: 'Đã bỏ chặn', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' }); } catch (e) { Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' }); }
    }
    async function handleUnfriend(uid) {
        const result = await Swal.fire({ title: 'Xóa bạn bè?', text: "Bạn có chắc chắn muốn xóa người này?", icon: 'question', showCancelButton: true, confirmButtonColor: '#ed4245', cancelButtonColor: '#7289da', confirmButtonText: 'Xóa bạn', cancelButtonText: 'Hủy', background: '#36393f', color: '#fff' });
        if (result.isConfirmed) { try { await DMApi.unfriend(uid); st.friends = st.friends.filter(f => f.friendUserId != uid); if (document.getElementById('dm-all-view').style.display !== 'none') { DMUI.renderFriendsCenter(st.friends, onSelectFriend); } const countEl = document.getElementById('dm-center-count'); if (countEl) countEl.textContent = st.friends.length; if (String(st.activeFriendId) === String(uid)) { await onSelectFriend(uid); } Swal.fire({ icon: 'success', title: 'Đã xóa bạn', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' }); } catch (e) { Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' }); } }
    }
    async function handleAddFriend(uid) {
        try { await DMApi.sendFriendRequestById(uid); const btnAdd = document.getElementById('btn-dm-addfriend'); if (btnAdd) { btnAdd.textContent = 'Đã gửi yêu cầu kết bạn'; btnAdd.disabled = true; btnAdd.style.backgroundColor = '#5865F2'; btnAdd.style.color = '#ffffff'; btnAdd.style.cursor = 'not-allowed'; btnAdd.style.opacity = '0.5'; } const btnSpam = document.getElementById('btn-dm-spam'); if (btnSpam) btnSpam.style.display = 'none'; Swal.fire({ icon: 'success', title: 'Đã gửi lời mời', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' }); } catch (e) { Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' }); }
    }
    function handleSpam(uid, isBlocked) {
        Swal.fire({ title: 'Báo cáo Spam', text: 'Bạn có chắc chắn muốn báo cáo?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Nộp báo cáo', confirmButtonColor: '#ed4245', cancelButtonText: 'Huỷ' }).then(async (res) => { if (res.isConfirmed) { try { await DMApi.reportUser(uid, 'SPAM'); if (isBlocked) { Swal.fire('Đã báo cáo', 'Báo cáo đã được gửi.', 'success'); } else { Swal.fire({ title: 'Đã báo cáo', text: 'Báo cáo đã gửi. Bạn có muốn chặn luôn người này?', icon: 'question', showCancelButton: true, confirmButtonText: 'Chặn', confirmButtonColor: '#ed4245', cancelButtonText: 'Xong' }).then(async (r2) => { if (r2.isConfirmed) { await handleBlock(uid); } else { onSelectFriend(uid); } }); } } catch (e) { Swal.fire('Lỗi', e.message, 'error'); } } });
    }
    function initSearchBar() {
        const input = document.getElementById('dm-sidebar-search');
        const results = document.getElementById('dm-search-results');
        if (!input || !results) return;

        input.addEventListener('input', async (e) => {
            const val = e.target.value.trim();
            if (!val) {
                results.style.display = 'none';
                return;
            }
            try {
                const user = await DMApi.userByUsername(val);
                results.innerHTML = '';
                if (user && String(user.id) !== String(st.currentUserId)) {
                    const div = document.createElement('div');
                    div.style.padding = '8px 12px';
                    div.style.color = '#fff';
                    div.style.cursor = 'pointer';
                    div.className = 'search-result-item';

                    const avatarHtml = user.avatarUrl
                        ? `<img src="${user.avatarUrl}" style="width:24px;height:24px;border-radius:50%;object-fit:cover;vertical-align:middle;margin-right:8px;">`
                        : `<span style="display:inline-block;width:24px;height:24px;border-radius:50%;background:#5865F2;margin-right:8px;vertical-align:middle;"></span>`;

                    const displayName = user.displayName || user.username;

                    div.innerHTML = `
                        <div style="display:flex;align-items:center;">
                            ${avatarHtml}
                            <div>
                                <div style="font-weight:500;">${displayName}</div>
                                <small style="color:#aaa;">@${user.username}</small>
                            </div>
                        </div>
                    `;

                    div.onclick = () => {
                        input.value = '';
                        results.style.display = 'none';
                        onSelectFriend(user.id, displayName, user.avatarUrl);
                    };
                    results.appendChild(div);
                } else {
                    results.innerHTML = '<div style="padding:10px; color:#aaa;">Không tìm thấy user</div>';
                }
                results.style.display = 'block';
            } catch (e) {
                console.error(e);
            }
        });

        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !results.contains(e.target)) {
                results.style.display = 'none';
            }
        });
    }

    async function handleAcceptRequest(uid) {
        try {
            const inbound = await DMApi.listInboundRequests();
            const req = inbound.find(r => String(r.senderId) === String(uid));
            if (req) {
                await DMApi.acceptRequest(req.id);
                // Reload state
                st.friends = await DMApi.fetchFriends();
                st.conversations = await DMApi.fetchAllConversations();

                // Update UI
                DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                if (document.getElementById('dm-all-view').style.display !== 'none') {
                    DMUI.renderFriendsCenter(st.friends, onSelectFriend);
                }
                const countEl = document.getElementById('dm-center-count');
                if (countEl) countEl.textContent = st.friends.length;

                if (String(st.activeFriendId) === String(uid)) {
                    await onSelectFriend(uid);
                }
                Swal.fire({ icon: 'success', title: 'Đã chấp nhận kết bạn', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' });
            } else {
                Swal.fire({ title: 'Lỗi', text: 'Không tìm thấy yêu cầu kết bạn.', icon: 'error', background: '#36393f', color: '#fff' });
            }
        } catch (e) {
            Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }
    async function handleDeclineRequest(uid) { try { const inbound = await DMApi.listInboundRequests(); const req = inbound.find(r => String(r.senderId) === String(uid)); if (req) { await DMApi.declineRequest(req.id); if (String(st.activeFriendId) === String(uid)) { await onSelectFriend(uid); } Swal.fire({ icon: 'success', title: 'Đã từ chối', toast: true, position: 'bottom-end', showConfirmButton: false, timer: 3000, background: '#36393f', color: '#fff' }); } } catch (e) { Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' }); } }
    async function onFriendEvent(evt) {
        try {
            console.log('[Friend Event]', evt);
            const type = evt?.type;
            const data = evt?.data;

            if (type === 'USER_ONLINE' || type === 'USER_OFFLINE') {
                const userId = evt.userId;
                const isOnline = (type === 'USER_ONLINE');

                const fIndex = st.friends.findIndex(f => String(f.friendUserId) === String(userId));
                if (fIndex !== -1) {
                    st.friends[fIndex].online = isOnline;

                    if (document.getElementById('dm-all-view').style.display !== 'none') {
                        DMUI.renderFriendsCenter(st.friends, onSelectFriend);
                    }
                    DMUI.renderActiveNow(st.friends, onSelectFriend);
                }

                // 2. Update Sidebar List (Conversations)
                const cIndex = st.conversations.findIndex(c => String(c.friendUserId) === String(userId));
                if (cIndex !== -1) {
                    st.conversations[cIndex].online = isOnline;
                    DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                }

                return;
            }

            st.friends = await DMApi.fetchFriends();
            try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch (e) { }
            st.conversations = await DMApi.fetchAllConversations();

            // Sync online status from friends to conversations
            syncConversationsWithFriends();

            // Re-render
            DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);

            // Update center list if visible
            if (document.getElementById('dm-all-view').style.display !== 'none') {
                DMUI.renderFriendsCenter(st.friends, onSelectFriend);
            }
            const countEl = document.getElementById('dm-center-count');
            if (countEl) countEl.textContent = st.friends.length;

            const pendingVisible = document.getElementById('dm-pending-view')?.style.display !== 'none';
            if (pendingVisible) {
                const inbound = await DMApi.listInboundRequests();
                const outbound = await DMApi.listOutboundRequests();
                DMUI.renderPending(inbound, outbound);
            }

            if (st.activeFriendId && data) {
                const currentActiveId = Number(st.activeFriendId);
                const eventSenderId = Number(data.senderId);
                const eventRecipientId = Number(data.recipientId);
                if (currentActiveId === eventSenderId || currentActiveId === eventRecipientId) {
                    await onSelectFriend(st.activeFriendId);
                }
            } else if (st.activeFriendId && (type === 'UNFRIEND' || type === 'BLOCKED' || type === 'UNBLOCKED')) {
                await onSelectFriend(st.activeFriendId);
            }
        } catch (e) {
            console.error('onFriendEvent error', e);
        }
    }

    // 1. Vào chế độ chỉnh sửa
    function enterEditMode(msgId, content) {
        const contentEl = document.getElementById(`msg-content-${msgId}`);
        if (!contentEl) return;

        let originalContent = contentEl.getAttribute('data-raw') || contentEl.innerText;
        if (originalContent) originalContent = originalContent.trim();

        contentEl.dataset.backupHtml = contentEl.innerHTML;

        contentEl.removeAttribute('style');
        contentEl.style.display = 'block';
        contentEl.style.width = '100%';
        contentEl.style.whiteSpace = 'normal';
        contentEl.style.height = 'auto';
        contentEl.style.minHeight = '0';
        contentEl.style.margin = '0';
        contentEl.style.padding = '0';

        contentEl.innerHTML = `
           <div class="edit-mode-container" style="width: 100%; margin: 0; padding: 0; display: flex; flex-direction: column;">
               <textarea id="edit-input-${msgId}" rows="1"
                   style="
                      width: 100%;
                      background: #40444b;
                      border: none;
                      color: #dcddde;
                      padding: 8px 10px;
                      border-radius: 4px;
                      resize: none;
                      overflow: hidden;
                      font-family: inherit;
                      font-size: 1rem;
                      line-height: 1.375rem;
                      box-sizing: border-box;
                      min-height: 0 !important;
                      height: 32px;
                      margin: 0;
                      display: block;
                      outline: none;
                      box-shadow: 0 0 0 1px #202225;
                   ">${originalContent}</textarea>
               <div style="
                  font-size: 12px;
                  line-height: 16px;
                  color: #b9bbbe;
                  margin-top: 4px;
                  margin-bottom: 0;
                  user-select: none;
               ">
                   nhấn escape để hủy • nhấn enter để lưu
               </div>
           </div>
       `;

        const textarea = document.getElementById(`edit-input-${msgId}`);

        const adjustHeight = () => {
            textarea.style.height = '1px';
            textarea.style.height = (textarea.scrollHeight) + 'px';
        };

        adjustHeight();

        textarea.focus();
        textarea.setSelectionRange(textarea.value.length, textarea.value.length);

        textarea.addEventListener('input', adjustHeight);

        textarea.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                finishEdit(msgId);
            }
            if (e.key === 'Escape') {
                e.preventDefault();
                cancelEdit(msgId);
            }
        });
    }

    // 2. Hủy chỉnh sửa (Quay về trạng thái cũ)
    function cancelEdit(msgId) {
        const contentEl = document.getElementById(`msg-content-${msgId}`);
        if (contentEl && contentEl.dataset.backupHtml) {
            contentEl.innerHTML = contentEl.dataset.backupHtml;
        }
    }

    // 3. Lưu chỉnh sửa (Gọi API)
    async function finishEdit(msgId) {
        const textarea = document.getElementById(`edit-input-${msgId}`);
        if (!textarea) return;

        const newContent = textarea.value.trim();
        if (!newContent) {
            if (confirm("Tin nhắn rỗng. Bạn có muốn xóa tin nhắn này không?")) {
                deleteMessageConfirm(msgId);
            }
            return;
        }

        const contentEl = document.getElementById(`msg-content-${msgId}`);
        const oldContent = contentEl.getAttribute('data-raw');
        if (newContent === oldContent) {
            cancelEdit(msgId);
            return;
        }

        try {
            await DMApi.editMessage(msgId, newContent);
        } catch (e) {
            alert('Lỗi sửa tin nhắn: ' + e.message);
            cancelEdit(msgId);
        }
    }

    // 4. Toggle Menu Popup
    function toggleMenu(msgId) {
        const menu = document.getElementById(`msg-menu-${msgId}`);
        if (!menu) return;
        const isCurrentlyOpen = menu.style.display === 'block';
        document.querySelectorAll('.msg-menu-popover').forEach(el => el.style.display = 'none');
        if (!isCurrentlyOpen) {
            menu.style.display = 'block';
        }
    }

    // 5. Xác nhận xóa
    async function deleteMessageConfirm(msgId) {
        const result = await Swal.fire({
            title: 'Xóa tin nhắn?',
            text: "Bạn có chắc chắn muốn xóa tin nhắn này không?",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ed4245',
            cancelButtonColor: '#7289da',
            confirmButtonText: 'Xóa',
            cancelButtonText: 'Hủy',
            background: '#36393f',
            color: '#fff'
        });

        if (result.isConfirmed) {
            try {
                await DMApi.deleteMessage(msgId);
            } catch (e) {
                Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
            }
        }
    }

    // Click ra ngoài để đóng menu
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.msg-menu-popover') && !e.target.closest('.btn-more-options')) {
            document.querySelectorAll('.msg-menu-popover').forEach(el => el.style.display = 'none');
        }
    });

    // ----- Init -----
    async function init() {
        try {
            st.currentUserId = window.state.currentUser?.id || JSON.parse(atob(DMApi.getToken().split('.')[1]))?.id;
            try { st.currentUser = await DMApi.getMe(); } catch (e) { st.currentUser = { id: st.currentUserId, avatarUrl: null }; }
            const { dmSendBtn, dmInput } = DMUI.els();
            if (dmSendBtn) dmSendBtn.addEventListener('click', onSend);
            if (dmInput) dmInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });

            const { dmFileInput } = DMUI.els();
            if (dmFileInput) {
                dmFileInput.addEventListener('change', (e) => {
                    if (dmFileInput.files && dmFileInput.files.length > 0) {
                        Array.from(dmFileInput.files).forEach(f => st.filesToUpload.push(f));
                        DMUI.renderAttachmentPreview(st.filesToUpload);
                        dmFileInput.value = ''; // Reset to allow re-selecting same file
                        if (dmInput) dmInput.focus();
                    }
                });
            }

            const { btnFriends } = DMUI.els();
            if (btnFriends) { const newBtnFriends = btnFriends.cloneNode(true); btnFriends.parentNode.replaceChild(newBtnFriends, btnFriends); newBtnFriends.addEventListener('click', goHome); }
            const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
            if (homeLogo) { const newHomeLogo = homeLogo.cloneNode(true); homeLogo.parentNode.replaceChild(newHomeLogo, homeLogo); newHomeLogo.addEventListener('click', () => { window.switchToDM && window.switchToDM(); goHome(); }); }
            const [convos, friends, blocked] = await Promise.all([DMApi.fetchAllConversations(), DMApi.fetchFriends(), DMApi.fetchBlockedUsers().catch(() => [])]);
            st.conversations = convos; st.friends = friends; st.blockedUsers = blocked;
            syncConversationsWithFriends();
            DMUI.renderFriendsSidebar(st.conversations, onSelectFriend); DMUI.initFriendsDashboard({ friends: st.friends, onSelectFriend });
            initSearchBar(); goHome(); DMWS.connectWS(onIncomingMessage, onFriendEvent);

            // Export functions to global scope
            window.DM = {
                init, goHome, retryMessage, reset: () => { },
                handleBlock, handleUnblock, handleUnfriend, handleAddFriend, handleSpam, handleAcceptRequest, handleDeclineRequest,
                prepareReply, cancelReply,
                enterEditMode, cancelEdit, finishEdit, toggleMenu, deleteMessageConfirm,
                removeAttachment, scrollToMessage
            };

        } catch (err) { console.error('[DM.init Error]', err); }
    }
    function syncConversationsWithFriends() {
        st.conversations.forEach(c => {
            const f = st.friends.find(fr => String(fr.friendUserId) === String(c.friendUserId));
            if (f) {
                c.online = f.online;
            }
        });
    }

    function reset() { console.log('DM reset'); }
    return { init, goHome, retryMessage, reset, handleBlock, handleUnblock, handleUnfriend, handleAddFriend, handleSpam, handleAcceptRequest, handleDeclineRequest, prepareReply, cancelReply, enterEditMode, cancelEdit, finishEdit, toggleMenu, deleteMessageConfirm, syncConversationsWithFriends };
})();