const DM = (() => {
  const st = {
    currentUserId: null,
    friends: [],
    activeFriendId: null,
    activeFriendName: '',
    conversationId: null,
    displayedMsgIds: new Set(),
    unreadCounts: new Map(),
    pendingRetries: new Map()
  };

  // Logic: Highlight Server Sidebar (Cột bên trái cùng)
  function highlightServerLogo() {
    document.querySelectorAll('.sidebar .sidebar-item').forEach(e => e.classList.remove('active'));
    const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
    if (homeLogo) homeLogo.classList.add('active');
  }

  // Logic: Quay về trang chủ (Dashboard)
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
    DMUI.updateSidebarItem(friendId, false);
    DMUI.setActiveFriendName(st.activeFriendName);
    DMUI.showConversation();
    highlightServerLogo();

    try {
      const conv = await DMApi.getOrCreateConversation(friendId);
      st.conversationId = String(conv.id);
      const msgs = await DMApi.fetchMessages(st.conversationId, 0, 50);
      DMUI.renderMessages(msgs, st.currentUserId, st.displayedMsgIds);
    } catch (e) {
      console.error('Error loading conversation:', e);
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
      if (!st.conversationId && sent.conversationId) st.conversationId = String(sent.conversationId);
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

  function onIncomingMessage(msg) {
    if (String(msg.senderId) === String(st.currentUserId)) return;
    const msgConvId = String(msg.conversationId || '');
    const currentConvId = String(st.conversationId || '');

    if (st.activeFriendId && msgConvId === currentConvId) {
      DMUI.appendMessage(msg, st.currentUserId, st.displayedMsgIds);
    } else {
      const friendId = msg.senderId;
      st.unreadCounts.set(friendId, (st.unreadCounts.get(friendId) || 0) + 1);
      DMUI.updateSidebarItem(friendId, true);
    }
  }

  // Friends WS event handler: auto reload cho sender/receiver
  async function onFriendEvent(evt) {
    try {
      const type = evt?.type;
      // Tự reload friends (sidebar + center)
      await DMUI.reloadFriends();
      // Nếu đang ở tab Pending -> reload pending list
      const pendingVisible = document.getElementById('dm-pending-view')?.style.display !== 'none';
      if (pendingVisible) {
        const inbound = await DMApi.listInboundRequests();
        const outbound = await DMApi.listOutboundRequests();
        DMUI.renderPending(inbound, outbound);
      }
      if (type === 'FRIEND_REQUEST_ACCEPTED') {
        console.log('[Friends] Yêu cầu đã được chấp nhận.');
      }
    } catch (e) {
      console.error('onFriendEvent error', e);
    }
  }

  function parseJwt (token) {
    try { return JSON.parse(atob(token.split('.')[1])); } catch (e) { return null; }
  }

  function reset() {
    DMWS.disconnect();
    st.currentUserId = null;
    st.friends = [];
    st.activeFriendId = null;
    st.conversationId = null;
    st.displayedMsgIds.clear();
    st.unreadCounts.clear();
    st.pendingRetries.clear();
  }

  // ----- Init -----
  async function init() {
    try {
      st.currentUserId = window.state.currentUser?.id || parseJwt(DMApi.getToken())?.id;

      const { dmSendBtn, dmInput } = DMUI.els();

      // Gán sự kiện cho nút Gửi
      const newBtn = dmSendBtn?.cloneNode(true);
      if (dmSendBtn && newBtn) {
        dmSendBtn.parentNode.replaceChild(newBtn, dmSendBtn);
        newBtn.addEventListener('click', onSend);
      }

      // Gán sự kiện cho ô Input
      const newInput = dmInput?.cloneNode(true);
      if (dmInput && newInput) {
        dmInput.parentNode.replaceChild(newInput, dmInput);
        newInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });
      }

      // Nút Home & Bạn bè
      const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
      if (homeLogo) {
        const newHomeLogo = homeLogo.cloneNode(true);
        homeLogo.parentNode.replaceChild(newHomeLogo, homeLogo);
        newHomeLogo.addEventListener('click', goHome);
      }

      const { btnFriends } = DMUI.els();
      if (btnFriends) {
        const newBtnFriends = btnFriends.cloneNode(true);
        btnFriends.parentNode.replaceChild(newBtnFriends, btnFriends);
        newBtnFriends.addEventListener('click', goHome);
      }

      st.friends = await DMApi.fetchFriends();
      DMUI.renderFriendsSidebar(st.friends, onSelectFriend);
      DMUI.renderFriendsCenter(st.friends, onSelectFriend);
      DMUI.initFriendsDashboard({ friends: st.friends, onSelectFriend });

      goHome();

      // WS connect: DM + Friends
      DMWS.connectWS(onIncomingMessage, onFriendEvent);

      window.DM = window.DM || {};
      window.DM.retryMessage = retryMessage;
      window.DM.reset = reset;
      window.DM.goHome = goHome;

    } catch (err) {
      console.error('[DM.init Error]', err);
    }
  }

  return { init, reset, retryMessage, goHome };
})();