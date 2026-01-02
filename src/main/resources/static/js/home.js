window.state = {
  currentServerId: null,
  currentChannelId: null,
  currentUser: null,
  stompClient: null,
  tempCategoryId: null,
  pendingAttachments: []
};

window.elements = {
  app: document.getElementById('app'),
  userDisplay: document.getElementById('user-display'),
  userAvatars: document.querySelectorAll('.user-avatar'),
  sidebar: document.querySelector('.sidebar'),
};

window.addEventListener('unhandledrejection', (e) => {
  console.error('[App Unhandled Rejection]', e.reason);
});

// ==================== DM IN HOME ====================
const HomeDM = (() => {
  const st = {
    currentUserId: null,
    friends: [],
    activeFriendId: null,
    activeFriendName: '',
    conversationId: null,

    wsConnected: false,
    subscription: null,
    onMessageHandler: null,
    retryCount: 0,
    displayedMsgIds: new Set(),
  };

  // ----- Auth -----
  function getToken() {
    return localStorage.getItem('token') || localStorage.getItem('accessToken') || '';
  }
  function authHeaders(json = true) {
    const h = {};
    const t = getToken();
    if (t) h['Authorization'] = `Bearer ${t}`;
    if (json) h['Content-Type'] = 'application/json';
    return h;
  }

  // ----- API -----
  async function fetchFriends() {
    const res = await fetch('/api/friends', { headers: authHeaders(false), credentials: 'include' });
    if (!res.ok) throw new Error(`Fetch friends failed (${res.status})`);
    return res.json();
  }
  async function getOrCreateConversation(friendUserId) {
    const res = await fetch(`/api/direct-messages/conversation/by-user/${friendUserId}`, {
      headers: authHeaders(false), credentials: 'include'
    });
    if (!res.ok) throw new Error(`Get/Create conversation failed (${res.status})`);
    return res.json();
  }
  async function fetchMessages(conversationId, page = 0, size = 50) {
    const res = await fetch(`/api/direct-messages/conversation/${conversationId}?page=${page}&size=${size}`, {
      headers: authHeaders(false), credentials: 'include'
    });
    if (!res.ok) throw new Error(`Fetch messages failed (${res.status})`);
    const pageData = await res.json();
    return pageData?.content || [];
  }
  async function sendMessage(receiverId, content) {
    const res = await fetch('/api/direct-messages', {
      method: 'POST',
      headers: authHeaders(true),
      credentials: 'include',
      body: JSON.stringify({ receiverId, content })
    });
    if (!res.ok) throw new Error(`Send message failed (${res.status})`);
    return res.json();
  }

  // ----- WS connect (robust) -----
  function connectWS(onMessage) {
    st.onMessageHandler = onMessage;

    if (st.wsConnected && window.state.stompClient?.connected) return;

    const token = getToken();

    const socket = new SockJS('/ws', null, { transports: ['websocket'] });
    socket.onopen = () => console.log('[SockJS] open');
    socket.onerror = (e) => console.error('[SockJS] error', e);
    socket.onclose = (e) => {
      console.warn('[SockJS] close', e);
      scheduleReconnect();
    };

    const client = Stomp.over(socket);
    client.debug = (str) => console.log('[STOMP]', str);
    client.heartbeat.outgoing = 20000;
    client.heartbeat.incoming = 20000;

    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    client.connect(
      headers,
      () => {
        st.wsConnected = true;
        st.retryCount = 0;
        window.state.stompClient = client;
        console.log('[WS] CONNECTED');

        try { st.subscription?.unsubscribe(); } catch {}
        // Subscribe user queue
        st.subscription = client.subscribe('/user/queue/dm', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            console.log('[WS MESSAGE]', msg);
            st.onMessageHandler && st.onMessageHandler(msg);
          } catch (e) { console.error('WS parse error', e); }
        });
        console.log('[WS] SUBSCRIBED /user/queue/dm');

        // Hook close để tự reconnect
        try {
          if (client.ws) {
            const prevOnClose = client.ws.onclose;
            client.ws.onclose = (ev) => {
              try { prevOnClose && prevOnClose(ev); } catch {}
              console.warn('[STOMP] socket closed, scheduling reconnect');
              scheduleReconnect();
            };
          }
        } catch {}
      },
      (err) => {
        console.error('WS connect error', err);
        scheduleReconnect();
      }
    );
  }

  function scheduleReconnect() {
    st.wsConnected = false;
    const delay = Math.min(30000, 2000 * Math.pow(2, st.retryCount++));
    console.warn(`WS reconnect in ${delay}ms (attempt ${st.retryCount})`);
    setTimeout(() => connectWS(st.onMessageHandler), delay);
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      if (!st.wsConnected || !window.state.stompClient?.connected) {
        console.log('[WS] visibilitychange -> reconnect');
        connectWS(st.onMessageHandler);
      }
    }
  });

  // ----- UI -----
  function els() {
    return {
      dmList: document.getElementById('dm-direct-list'),
      dmCenterList: document.getElementById('dm-center-list'),
      dmCenterCount: document.getElementById('dm-center-count'),
      dmDashboard: document.getElementById('dm-dashboard-view'),
      dmConversation: document.getElementById('dm-conversation-view'),
      dmName: document.getElementById('dm-active-friend-name'),
      dmMessages: document.getElementById('dm-messages'),
      dmInput: document.getElementById('dm-input'),
      dmSendBtn: document.getElementById('dm-send-btn'),
    };
  }

  function showDashboard() {
    const { dmDashboard, dmConversation } = els();
    if (dmDashboard) dmDashboard.style.display = 'flex';
    if (dmConversation) dmConversation.style.display = 'none';
  }
  function showConversation() {
    const { dmDashboard, dmConversation } = els();
    if (dmDashboard) dmDashboard.style.display = 'none';
    if (dmConversation) dmConversation.style.display = 'flex';
  }

  function renderFriendsSidebar(friends) {
    const { dmList } = els();
    if (!dmList) return;
    dmList.innerHTML = friends.map(friendSidebarItem).join('');
    dmList.querySelectorAll('.channel-item.dm-item').forEach(item => {
      item.addEventListener('click', () => {
        dmList.querySelectorAll('.channel-item.dm-item').forEach(e => e.classList.remove('active'));
        item.classList.add('active');
        const friendId = Number(item.getAttribute('data-friend-id'));
        const name = item.getAttribute('data-friend-name') || '';
        onSelectFriend(friendId, name);
      });
    });
  }
  function friendSidebarItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    const initials = (name || 'U').split(' ').map(s => s[0]).join('').slice(0,2).toUpperCase();
    return `
      <div class="channel-item dm-item" data-friend-id="${f.friendUserId}" data-friend-name="${name}">
        <div style="display:flex; align-items:center; gap:10px;">
          <div style="width:24px; height:24px; border-radius:50%; background:#3f4147; color:#cfd1d5; display:flex; align-items:center; justify-content:center; font-size:12px;">${initials}</div>
          <span>${name}</span>
        </div>
      </div>`;
  }

  function renderFriendsCenter(friends) {
    const { dmCenterList, dmCenterCount } = els();
    if (!dmCenterList) return;
    if (dmCenterCount) dmCenterCount.textContent = String(friends.length);
    dmCenterList.innerHTML = friends.map(friendCenterItem).join('');
    dmCenterList.querySelectorAll('.dm-center-item').forEach(item => {
      item.addEventListener('click', () => {
        dmCenterList.querySelectorAll('.dm-center-item').forEach(e => e.classList.remove('active'));
        item.classList.add('active');
        const friendId = Number(item.getAttribute('data-friend-id'));
        const name = item.getAttribute('data-friend-name') || '';
        onSelectFriend(friendId, name);
      });
    });
  }
  function friendCenterItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    return `
      <div class="dm-center-item dm-header-item" data-friend-id="${f.friendUserId}" data-friend-name="${name}" style="display:flex; align-items:center; gap:12px; border-radius:8px;">
        <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
        <div style="display:flex; flex-direction:column;">
          <span style="color:#fff;">${name}</span>
          <small style="color:#b5bac1;">Online</small>
        </div>
      </div>`;
  }

  function setActiveFriendName(name) {
    const { dmName, dmInput } = els();
    if (dmName) dmName.textContent = name || 'Tin nhắn trực tiếp';
    if (dmInput) dmInput.placeholder = `Nhắn tin cho @${name || ''}`;
  }

  function renderMessages(list) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    st.displayedMsgIds.clear();
    dmMessages.innerHTML = list.slice().reverse().map(m => {
      if (m.id != null) st.displayedMsgIds.add(String(m.id));
      return msgBubble(m);
    }).join('');
    dmMessages.scrollTop = dmMessages.scrollHeight;
  }
  function appendMessage(m) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    const mid = m.id != null ? String(m.id) : null;
    if (mid && st.displayedMsgIds.has(mid)) return; // chống trùng
    if (mid) st.displayedMsgIds.add(mid);
    dmMessages.insertAdjacentHTML('beforeend', msgBubble(m));
    dmMessages.scrollTop = dmMessages.scrollHeight;
  }
  function msgBubble(m) {
    const mine = String(m.senderId) === String(st.currentUserId);
    const time = new Date(m.createdAt).toLocaleString();
    const safe = (m.content || '').replace(/[&<>"']/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[s]));
    return `
      <div class="message" data-msg-id="${m.id ?? ''}" style="display:flex; ${mine ? 'justify-content:flex-end;' : ''}">
        <div style="max-width:70%; background:${mine ? '#4752c4' : '#2b2d31'}; color:#fff; padding:8px 12px; border-radius:8px;">
          <div style="font-size:12px;color:#c7c7c7; margin-bottom:4px;">${time}</div>
          <div>${safe}</div>
        </div>
      </div>`;
  }

  // ----- Handlers -----
  async function onSelectFriend(friendId, friendName) {
    st.activeFriendId = friendId;
    st.activeFriendName = friendName || ('User ' + friendId);
    setActiveFriendName(st.activeFriendName);

    showConversation();

    const conv = await getOrCreateConversation(friendId);
    st.conversationId = String(conv.id);      // chuẩn hóa String
    const msgs = await fetchMessages(st.conversationId, 0, 50);
    renderMessages(msgs);
  }

  async function onSend() {
    const { dmInput } = els();
    if (!dmInput || !st.activeFriendId) return;
    const content = dmInput.value.trim();
    if (!content) return;
    try {
      const sent = await sendMessage(st.activeFriendId, content);

      // Luôn append lạc quan để sender thấy ngay; WS về sẽ được de-dup theo id
      appendMessage(sent);

      dmInput.value = '';
      if (!st.conversationId && sent.conversationId != null) {
        st.conversationId = String(sent.conversationId);
      }
    } catch (e) {
      console.error('[Send DM Error]', e);
      if (String(e).includes('401') || String(e).includes('403')) {
        alert('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
      }
    }
  }

  function onIncomingMessage(msg) {
      console.log('%c[SOCKET] TIN NHẮN ĐẾN!', 'color: red; font-size: 16px; font-weight: bold;');
      console.log('Dữ liệu tin nhắn:', msg);
      console.log('ID Hội thoại hiện tại (State):', st.conversationId, typeof st.conversationId);
      console.log('ID Hội thoại tin nhắn (Msg):', msg.conversationId, typeof msg.conversationId);
      const currentId = String(st.conversationId || '');
      const msgId = String(msg.conversationId || '');
      console.log(`So sánh: "${currentId}" giống "${msgId}" không? -> ${currentId === msgId}`);
      if (currentId === msgId) {
          console.log('ID KHỚP -> ĐANG CẬP NHẬT GIAO DIỆN...');
          appendMessage(msg);
          // Scroll xuống dưới cùng sau khi thêm tin
          const { dmMessages } = els();
          if (dmMessages) {
              // Dùng setTimeout để đảm bảo HTML render xong mới scroll
              setTimeout(() => {
                  dmMessages.scrollTop = dmMessages.scrollHeight;
              }, 50);
          }
      } else {
          console.warn('⚠ID KHÔNG KHỚP -> KHÔNG HIỂN THỊ. (Bạn đang chat với người khác?)');
      }
    }

  // ----- Init -----
  async function init() {
    try {
      st.currentUserId = window.state.currentUser?.id || null;

      const { dmSendBtn, dmInput } = els();
      dmSendBtn?.addEventListener('click', onSend);
      dmInput?.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });

      st.friends = await fetchFriends();
      renderFriendsSidebar(st.friends);
      renderFriendsCenter(st.friends);
      showDashboard();

      connectWS(onIncomingMessage);
    } catch (err) {
      console.error('[HomeDM.init Error]', err);
    }
  }

  return { init };
})();

// ==================== APP BOOTSTRAP ====================
document.addEventListener('DOMContentLoaded', async function () {
  async function init() {
    try {
      if (elements.app) elements.app.style.display = 'flex';
      await loadMe();
      await loadServers();
      await HomeDM.init();
      console.log('App Initialized & UI Synced (Server + DM)');
    } catch (err) {
      console.error('[App Init Error]', err);
    }
  }

  await init();
});