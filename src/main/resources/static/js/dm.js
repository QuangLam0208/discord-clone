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

  function highlightServerLogo() {
      // Tìm Logo Home (Server item đầu tiên hoặc nút có switchToDM)
      document.querySelectorAll('.sidebar .sidebar-item').forEach(e => e.classList.remove('active'));
      const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
      if (homeLogo) homeLogo.classList.add('active');
  }

  // Logic: Quay về trang chủ
  function goHome() {
      st.activeFriendId = null;
      st.activeFriendName = '';
      st.conversationId = null;

      // 1. Hiện Dashboard, ẩn chat
      DMUI.showDashboard();

      // 2. Highlight nút Bạn bè (Hàm này sẽ tự reset highlight các user)
      DMUI.highlightFriendsButton();

      // 3. Highlight Logo Server bên trái
      highlightServerLogo();

      console.log('[DM] Go Home -> Dashboard Active');
  }

  async function onSelectFriend(friendId, friendName) {
    st.activeFriendId = friendId;
    st.activeFriendName = friendName || ('User ' + friendId);

    st.unreadCounts.set(friendId, 0);
    DMUI.updateSidebarItem(friendId, false);

    DMUI.setActiveFriendName(st.activeFriendName);
    DMUI.showConversation();

    // Vẫn giữ sáng Logo Server khi chat DM
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
          if(tempEl) tempEl.style.opacity = '0.5';
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

  async function init() {
    try {
      st.currentUserId = window.state.currentUser?.id || parseJwt(DMApi.getToken())?.id;

      const { dmSendBtn, dmInput } = DMUI.els();
      const newBtn = dmSendBtn?.cloneNode(true);
      if(dmSendBtn && newBtn) {
          dmSendBtn.parentNode.replaceChild(newBtn, dmSendBtn);
          newBtn.addEventListener('click', onSend);
      }
      const newInput = dmInput?.cloneNode(true);
      if(dmInput && newInput) {
          dmInput.parentNode.replaceChild(newInput, dmInput);
          newInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });
      }

      // === SỰ KIỆN NÚT HOME VÀ FRIENDS ===

      // 1. Logo Home (Sidebar trái cùng)
      const homeLogo = document.querySelector('.sidebar .sidebar-item[onclick*="switchToDM"]');
      if (homeLogo) {
          homeLogo.addEventListener('click', goHome);
      }

      // 2. Nút "Bạn bè" (Sidebar DM) - Lấy từ DMUI.els() cho chắc ăn
      const { btnFriends } = DMUI.els();
      if (btnFriends) {
          btnFriends.addEventListener('click', goHome);
          // Gán ID nếu chưa có để debug dễ hơn
          if (!btnFriends.id) btnFriends.id = 'btn-friends-auto-found';
      } else {
          console.warn('[DM] Vẫn chưa tìm thấy nút Bạn bè. Kiểm tra lại HTML class="fa-user-group"');
      }

      st.friends = await DMApi.fetchFriends();
      DMUI.renderFriendsSidebar(st.friends, onSelectFriend);
      DMUI.renderFriendsCenter(st.friends, onSelectFriend);

      goHome();

      DMWS.connectWS(onIncomingMessage);

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