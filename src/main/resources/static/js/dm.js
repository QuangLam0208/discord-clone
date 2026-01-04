const DM = (() => {
    const st = {
        currentUserId: null,
        friends: [],
        blockedUsers: [],
        conversations: [],
        activeFriendId: null,
        activeFriendName: '',
        conversationId: null,
        displayedMsgIds: new Set(),
        unreadCounts: new Map(),
        pendingRetries: new Map()
    };

    function highlightServerLogo() {
        document.querySelectorAll('.sidebar .sidebar-item').forEach(e => e.classList.remove('active'));
        const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
        if (homeLogo) homeLogo.classList.add('active');
    }

    function goHome() {
        st.activeFriendId = null;
        st.activeFriendName = '';
        st.conversationId = null;

        DMUI.showDashboard();
        DMUI.highlightFriendsButton();
        highlightServerLogo();

        console.log('[DM] Switched to Dashboard');
    }

    async function onSelectFriend(friendId, friendName) {
        st.activeFriendId = friendId;
        st.activeFriendName = friendName || ('User ' + friendId);
        st.unreadCounts.set(friendId, 0);

        let friendInfo = st.conversations.find(c => c.friendUserId == friendId);
        if(!friendInfo) {
            friendInfo = st.friends.find(f => f.friendUserId == friendId) || {
                friendUserId: friendId, displayName: friendName, friendUsername: friendName
            };
        }

        DMUI.setActiveFriendName(st.activeFriendName);
        DMUI.showConversation();
        highlightServerLogo();

        try {
            const conv = await DMApi.getOrCreateConversation(friendId);
            st.conversationId = String(conv.id);
            DMWS.subscribeToConversation(st.conversationId, onConversationEvent);
            // Update Sidebar
            const exists = st.conversations.some(c => String(c.conversationId) === st.conversationId);
            if (!exists) {
                const newConvoItem = {
                    conversationId: conv.id,
                    friendUserId: Number(friendId),
                    displayName: friendInfo.displayName || friendInfo.friendUsername || st.activeFriendName,
                    friendUsername: friendInfo.friendUsername || st.activeFriendName,
                    avatarUrl: friendInfo.avatarUrl || null,
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

            // --- 2. LOGIC CHECK STATUS MỚI (CÓ CHECK BLOCK) ---
            const isFriend = st.friends.some(f => f.friendUserId == friendId);
            // Kiểm tra xem ID này có nằm trong danh sách chặn không
            const isBlocked = st.blockedUsers.some(f => String(f.friendUserId) === String(friendId));

            let status = 'NOT_FRIEND';

            if (isBlocked) {
                status = 'BLOCKED';
            } else if (isFriend) {
                status = 'FRIEND';
            }

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

                if (hasReceived) {
                    status = 'RECEIVED_REQUEST';
                } else if (hasSent) {
                    status = 'SENT_REQUEST';
                }
            } else {
                // Nếu là FRIEND hoặc BLOCKED thì cứ tải tin nhắn
                msgs = await DMApi.fetchMessages(st.conversationId, 0, 50);
            }

            // Gửi status chính xác ('BLOCKED') sang cho UI vẽ 2 nút Bỏ chặn & Spam
            DMUI.renderMessages(msgs, st.currentUserId, st.displayedMsgIds, friendInfo, status);

        } catch (e) {
            console.error('Error loading conversation:', e);
        }
    }

    function onConversationEvent(payload) {
        // payload = { type: "DELETE_MESSAGE" | "RESTORE_MESSAGE", ... }

        if (payload.type === 'DELETE_MESSAGE') {
            DMUI.removeMessageElement(payload.messageId);

            // [QUAN TRỌNG] Xóa khỏi danh sách đã hiển thị để nếu sau này có Restore thì UI cho phép vẽ lại
            st.displayedMsgIds.delete(String(payload.messageId));
        }
        else if (payload.type === 'RESTORE_MESSAGE') {
            DMUI.removeMessageElement(payload.id); // Xóa element cũ/lỗi nếu còn sót

            // [QUAN TRỌNG] Xóa ID khỏi Set để hàm appendMessage không chặn việc vẽ lại
            st.displayedMsgIds.delete(String(payload.id));

            // Kiểm tra xem tin nhắn được khôi phục có thuộc hội thoại đang mở không
            if (String(payload.conversationId) === String(st.conversationId)) {
                DMUI.appendMessage(payload, st.currentUserId, st.displayedMsgIds);
            }
        }
    }

    async function onSend() {
        const { dmInput } = DMUI.els();
        if (!dmInput || !st.activeFriendId) return;
        const content = dmInput.value.trim();
        if (!content) return;
        dmInput.value = '';
        await executeSendMessage(st.activeFriendId, content);
    }

    async function executeSendMessage(receiverId, content, existingTempId = null) {
        const tempId = existingTempId || ('temp-' + Date.now() + Math.random().toString(36).substr(2, 9));
        const tempMsg = { id: null, tempId: tempId, senderId: st.currentUserId, receiverId: receiverId, content: content, createdAt: new Date().toISOString(), status: 'pending' };
        if (!existingTempId) DMUI.appendMessage(tempMsg, st.currentUserId, null);
        try {
            const sent = await DMApi.sendMessage(receiverId, content);
            DMUI.replaceTempMessage(tempId, sent, st.currentUserId);
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
                    if(newItemDom) {
                        newItemDom.classList.add('active');
                        const nameText = newItemDom.querySelector('.dm-name-text');
                        if (nameText) {
                            nameText.style.color = '#96989d';
                            nameText.style.fontWeight = '500';
                        }
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
            executeSendMessage(data.receiverId, data.content, tempId);
        }
    }

    async function onIncomingMessage(msg) {
        if (String(msg.senderId) === String(st.currentUserId)) return;

        const msgConvId = String(msg.conversationId || '');
        const currentConvId = String(st.conversationId || '');

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
            DMUI.appendMessage(msg, st.currentUserId, st.displayedMsgIds);
        }
        else {
            const friendId = msg.senderId;
            st.unreadCounts.set(friendId, (st.unreadCounts.get(friendId) || 0) + 1);
            if (conversationExists) {
                const idx = st.conversations.findIndex(c => String(c.conversationId) === msgConvId);
                if (idx >= 0) {
                    if (idx > 0) {
                        const [item] = st.conversations.splice(idx, 1);
                        st.conversations.unshift(item);
                        DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
                    }
                    setTimeout(() => {
                        DMUI.updateSidebarItem(friendId, true);
                    }, 10);
                }
            }
        }
    }

    // --- ACTIONS---
    async function handleBlock(uid) {
        const result = await Swal.fire({
            title: 'Chặn người dùng?',
            text: "Bạn sẽ không nhận được tin nhắn từ họ nữa.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ed4245',
            cancelButtonColor: '#7289da',
            confirmButtonText: 'Chặn',
            cancelButtonText: 'Hủy',
            background: '#36393f',
            color: '#fff'
        });

        if (result.isConfirmed) {
            try {
                await DMApi.blockUser(uid);

                try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch(e) {}
                st.friends = await DMApi.fetchFriends();

                if (String(st.activeFriendId) === String(uid)) {
                    await onSelectFriend(uid);
                }

                Swal.fire({
                    icon: 'success',
                    title: 'Đã chặn',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 3000,
                    background: '#36393f',
                    color: '#fff'
                });
            } catch(e) {
                Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
            }
        }
    }

    async function handleUnblock(uid) {
        try {
            await DMApi.unblockUser(uid);

            try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch(e) {}

            if (String(st.activeFriendId) === String(uid)) {
                await onSelectFriend(uid);
            }

            Swal.fire({
                icon: 'success',
                title: 'Đã bỏ chặn',
                toast: true,
                position: 'bottom-end',
                showConfirmButton: false,
                timer: 3000,
                background: '#36393f',
                color: '#fff'
            });
        } catch(e) {
            Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }

    async function handleUnfriend(uid) {
        const result = await Swal.fire({
            title: 'Xóa bạn bè?',
            text: "Bạn có chắc chắn muốn xóa người này khỏi danh sách bạn bè?",
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#ed4245',
            cancelButtonColor: '#7289da',
            confirmButtonText: 'Xóa bạn',
            cancelButtonText: 'Hủy',
            background: '#36393f',
            color: '#fff'
        });

        if (result.isConfirmed) {
            try {
                await DMApi.unfriend(uid);

                st.friends = st.friends.filter(f => f.friendUserId != uid);
                if (document.getElementById('dm-all-view').style.display !== 'none') {
                    DMUI.renderFriendsCenter(st.friends, onSelectFriend);
                }
                const countEl = document.getElementById('dm-center-count');
                if(countEl) countEl.textContent = st.friends.length;

                if (String(st.activeFriendId) === String(uid)) {
                    await onSelectFriend(uid);
                }

                Swal.fire({
                    icon: 'success',
                    title: 'Đã xóa bạn',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 3000,
                    background: '#36393f',
                    color: '#fff'
                });
            } catch(e) {
                Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
            }
        }
    }

    async function handleAddFriend(uid) {
        try {
            await DMApi.sendFriendRequestById(uid);

            const btnAdd = document.getElementById('btn-dm-addfriend');
            if (btnAdd) {
                btnAdd.textContent = 'Đã gửi yêu cầu kết bạn';
                btnAdd.disabled = true;
                btnAdd.style.backgroundColor = '#5865F2';
                btnAdd.style.color = '#ffffff';
                btnAdd.style.cursor = 'not-allowed';
                btnAdd.style.opacity = '0.5';
            }
            const btnSpam = document.getElementById('btn-dm-spam');
            if (btnSpam) btnSpam.style.display = 'none';

            Swal.fire({
                icon: 'success',
                title: 'Đã gửi lời mời',
                toast: true,
                position: 'bottom-end',
                showConfirmButton: false,
                timer: 3000,
                background: '#36393f',
                color: '#fff'
            });

        } catch(e) {
            Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }

    function handleSpam(uid, isBlocked) {
        Swal.fire({
            title: 'Báo cáo Spam',
            text: 'Bạn có chắc chắn muốn báo cáo?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Nộp báo cáo',
            confirmButtonColor: '#ed4245',
            cancelButtonText: 'Huỷ'
        }).then(async (res) => {
            if (res.isConfirmed) {
                try {
                    await DMApi.reportUser(uid, 'SPAM');
                    if (isBlocked) {
                        Swal.fire('Đã báo cáo', 'Báo cáo đã được gửi.', 'success');
                    } else {
                        Swal.fire({
                            title: 'Đã báo cáo',
                            text: 'Báo cáo đã gửi. Bạn có muốn chặn luôn người này?',
                            icon: 'question',
                            showCancelButton: true,
                            confirmButtonText: 'Chặn',
                            confirmButtonColor: '#ed4245',
                            cancelButtonText: 'Xong'
                        }).then(async (r2) => {
                            if (r2.isConfirmed) {
                                await handleBlock(uid);
                            } else {
                                onSelectFriend(uid);
                            }
                        });
                    }
                } catch (e) { Swal.fire('Lỗi', e.message, 'error'); }
            }
        });
    }

    function initSearchBar() {
        const input = document.getElementById('dm-sidebar-search');
        const results = document.getElementById('dm-search-results');
        if(!input || !results) return;

        input.addEventListener('input', async (e) => {
            const val = e.target.value.trim();
            if(!val) {
                results.style.display = 'none';
                return;
            }
            try {
                const user = await DMApi.userByUsername(val);
                results.innerHTML = '';
                if(user) {
                    const div = document.createElement('div');
                    div.style.padding = '8px 12px';
                    div.style.color = '#fff';
                    div.style.cursor = 'pointer';
                    div.className = 'search-result-item';
                    div.innerHTML = `<div>${user.username}</div><small style="color:#aaa;">User #${user.id}</small>`;
                    div.onclick = () => {
                        input.value = '';
                        results.style.display = 'none';
                        onSelectFriend(user.id, user.username);
                    };
                    results.appendChild(div);
                } else {
                    results.innerHTML = '<div style="padding:10px; color:#aaa;">Không tìm thấy user</div>';
                }
                results.style.display = 'block';
            } catch(e) { console.error(e); }
        });
        document.addEventListener('click', (e) => {
            if(!input.contains(e.target) && !results.contains(e.target)) {
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
                st.friends = await DMApi.fetchFriends();
                if (String(st.activeFriendId) === String(uid)) {
                    await onSelectFriend(uid);
                }
                Swal.fire({
                    icon: 'success',
                    title: 'Đã chấp nhận kết bạn',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 3000,
                    background: '#36393f',
                    color: '#fff'
                });
            } else {
                Swal.fire({ title: 'Lỗi', text: 'Không tìm thấy yêu cầu kết bạn.', icon: 'error', background: '#36393f', color: '#fff' });
            }
        } catch (e) {
            Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }

    async function handleDeclineRequest(uid) {
        try {
            const inbound = await DMApi.listInboundRequests();
            const req = inbound.find(r => String(r.senderId) === String(uid));

            if (req) {
                await DMApi.declineRequest(req.id);
                if (String(st.activeFriendId) === String(uid)) {
                    await onSelectFriend(uid);
                }
                Swal.fire({
                    icon: 'success',
                    title: 'Đã từ chối',
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 3000,
                    background: '#36393f',
                    color: '#fff'
                });
            }
        } catch (e) {
            Swal.fire({ title: 'Lỗi', text: e.message, icon: 'error', background: '#36393f', color: '#fff' });
        }
    }

    async function onFriendEvent(evt) {
        try {
            console.log('[Friend Event]', evt);

            const type = evt?.type;
            const data = evt?.data;

            st.friends = await DMApi.fetchFriends();
            // Cập nhật danh sách chặn khi có event
            try { st.blockedUsers = await DMApi.fetchBlockedUsers(); } catch(e) {}

            st.conversations = await DMApi.fetchAllConversations();
            DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);

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
            }
            else if (st.activeFriendId && (type === 'UNFRIEND' || type === 'BLOCKED' || type === 'UNBLOCKED')) {
                await onSelectFriend(st.activeFriendId);
            }

        } catch (e) {
            console.error('onFriendEvent error', e);
        }
    }

    // ----- Init -----
    async function init() {
        try {
            st.currentUserId = window.state.currentUser?.id || JSON.parse(atob(DMApi.getToken().split('.')[1]))?.id;

            const { dmSendBtn, dmInput } = DMUI.els();
            if (dmSendBtn) dmSendBtn.addEventListener('click', onSend);
            if (dmInput) dmInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });

            const { btnFriends } = DMUI.els();
            if (btnFriends) {
                const newBtnFriends = btnFriends.cloneNode(true);
                btnFriends.parentNode.replaceChild(newBtnFriends, btnFriends);
                newBtnFriends.addEventListener('click', goHome);
            }

            const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
            if (homeLogo) {
                const newHomeLogo = homeLogo.cloneNode(true);
                homeLogo.parentNode.replaceChild(newHomeLogo, homeLogo);
                newHomeLogo.addEventListener('click', () => {
                    window.switchToDM && window.switchToDM();
                    goHome();
                });
            }

            // --- 3. LOAD DATA ---
            const [convos, friends, blocked] = await Promise.all([
                DMApi.fetchAllConversations(),
                DMApi.fetchFriends(),
                DMApi.fetchBlockedUsers().catch(() => [])
            ]);

            st.conversations = convos;
            st.friends = friends;
            st.blockedUsers = blocked;

            DMUI.renderFriendsSidebar(st.conversations, onSelectFriend);
            DMUI.initFriendsDashboard({ friends: st.friends, onSelectFriend });

            initSearchBar();

            goHome();

            DMWS.connectWS(onIncomingMessage, onFriendEvent);

            window.DM = window.DM || {};
            window.DM.retryMessage = retryMessage;
            window.DM.reset = () => { };
            window.DM.goHome = goHome;
            window.DM.handleBlock = handleBlock;
            window.DM.handleUnblock = handleUnblock;
            window.DM.handleUnfriend = handleUnfriend;
            window.DM.handleAddFriend = handleAddFriend;
            window.DM.handleSpam = handleSpam;
            window.DM.handleAcceptRequest = handleAcceptRequest;
            window.DM.handleDeclineRequest = handleDeclineRequest;
        } catch (err) {
            console.error('[DM.init Error]', err);
        }
    }
    function reset() {
        console.log('DM reset');
    }
    return { init, goHome, retryMessage, reset };
})();