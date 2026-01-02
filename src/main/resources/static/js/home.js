// MAIN APP ENTRY POINT (v9 - Robust WS reconnect + DM in Home)

// Global app state shared across modules
window.state = {
  currentServerId: null,
  currentChannelId: null,
  currentUser: null,
  stompClient: null,       // STOMP client instance
  tempCategoryId: null,    // For modals
  pendingAttachments: []   // Shared between upload.js and chat.js
};

window.elements = {
  app: document.getElementById('app'),
  userDisplay: document.getElementById('user-display'),
  userAvatars: document.querySelectorAll('.user-avatar'),
  sidebar: document.querySelector('.sidebar'),
};

// Optional: catch unhandled promise errors for clearer logs
window.addEventListener('unhandledrejection', (e) => {
  console.error('[App Unhandled Rejection]', e.reason);
});

// ==================== DM TRONG HOME ====================
const HomeDM = (() => {
  const st = {
    currentUserId: null,
    friends: [],
    activeFriendId: null,
    activeFriendName: '',
    conversationId: null,

    // WS state
    wsConnected: false,
    subscription: null,
    onMessageHandler: null,
    retryCount: 0,
  };

  // ---- Auth helpers ----
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

  // ---- API calls ----
  async function fetchFriends() {
    const res = await fetch('/api/friends', {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
    });
    if (!res.ok) throw new Error(`Fetch friends failed (${res.status})`);
    return res.json();
  }

  async function getOrCreateConversation(friendUserId) {
    const res = await fetch(`/api/direct-messages/conversation/by-user/${friendUserId}`, {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
    });
    if (!res.ok) throw new Error(`Get/Create conversation failed (${res.status})`);
    return res.json(); // ConversationResponse
  }

  async function fetchMessages(conversationId, page = 0, size = 50) {
    const res = await fetch(`/api/direct-messages/conversation/${conversationId}?page=${page}&size=${size}`, {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
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
    return res.json(); // DirectMessageResponse
  }

  // ---- Robust WebSocket connect (auto-retry, heartbeat, re-subscribe) ----
  function connectWS(onMessage) {
    st.onMessageHandler = onMessage;

    // Avoid duplicate connects
    if (st.wsConnected && window.state.stompClient?.connected) return;

    const token = getToken();
    const socket = new SockJS('/ws');

    // Attach low-level handlers for visibility
    socket.onopen = () => console.log('[SockJS] open');
    socket.onerror = (e) => console.error('[SockJS] error', e);
    socket.onclose = (e) => {
      console.warn('[SockJS] close', e);
      scheduleReconnect();
    };

    const client = Stomp.over(socket);

    // Enable STOMP frame logging to help diagnose
    client.debug = (str) => {
      // comment out next line if too noisy
      console.log('[STOMP]', str);
    };

    client.heartbeat.outgoing = 20000;  // send ping
    client.heartbeat.incoming = 20000;  // expect ping

    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    client.connect(
      headers,
      () => {
        // On Connected
        st.wsConnected = true;
        st.retryCount = 0;
        window.state.stompClient = client;
        console.log('[WS] connected');

        // Re/subscribe
        try { st.subscription?.unsubscribe(); } catch {}
        st.subscription = client.subscribe('/user/queue/dm', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            st.onMessageHandler && st.onMessageHandler(msg);
          } catch (e) {
            console.error('WS parse error', e);
          }
        });

        // Hook close to schedule reconnect (STOMP-level)
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
    const delay = Math.min(30000, 2000 * Math.pow(2, st.retryCount++)); // 2s -> 4s -> 8s ... max 30s
    console.warn(`WS reconnect in ${delay}ms (attempt ${st.retryCount})`);
    setTimeout(() => connectWS(st.onMessageHandler), delay);
  }

  // Reconnect when tab returns to foreground
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      if (!st.wsConnected || !window.state.stompClient?.connected) {
        console.log('[WS] visibilitychange -> reconnect');
        connectWS(st.onMessageHandler);
      }
    }
  });

  // ---- UI helpers ----
  function els() {
    return {
      // Sidebar list (TIN NHẮN TRỰC TIẾP)
      dmList: document.getElementById('dm-direct-list'),

      // Center list (dashboard Friends)
      dmCenterList: document.getElementById('dm-center-list'),
      dmCenterCount: document.getElementById('dm-center-count'),

      // Views
      dmDashboard: document.getElementById('dm-dashboard-view'),
      dmConversation: document.getElementById('dm-conversation-view'),

      // Conversation UI
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

  // Sidebar friends (left)
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

  // Center friends (dashboard)
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
    dmMessages.innerHTML = list.slice().reverse().map(msgBubble).join('');
    dmMessages.scrollTop = dmMessages.scrollHeight;
  }
  function appendMessage(m) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    dmMessages.insertAdjacentHTML('beforeend', msgBubble(m));
    dmMessages.scrollTop = dmMessages.scrollHeight;
  }
  function msgBubble(m) {
    const mine = m.senderId === st.currentUserId;
    const time = new Date(m.createdAt).toLocaleString();
    const safe = (m.content || '').replace(/[&<>"']/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[s]));
    return `
      <div class="message" style="display:flex; ${mine ? 'justify-content:flex-end;' : ''}">
        <div style="max-width:70%; background:${mine ? '#4752c4' : '#2b2d31'}; color:#fff; padding:8px 12px; border-radius:8px;">
          <div style="font-size:12px;color:#c7c7c7; margin-bottom:4px;">${time}</div>
          <div>${safe}</div>
        </div>
      </div>`;
  }

  // ---- Handlers ----
  async function onSelectFriend(friendId, friendName) {
    st.activeFriendId = friendId;
    st.activeFriendName = friendName || ('User ' + friendId);
    setActiveFriendName(st.activeFriendName);

    // Switch to conversation view
    showConversation();

    // Load conversation + messages
    const conv = await getOrCreateConversation(friendId);
    st.conversationId = conv.id;
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
      appendMessage(sent);      // append optimistically
      dmInput.value = '';
      if (!st.conversationId && sent.conversationId) {
        st.conversationId = sent.conversationId;
      }
    } catch (e) {
      console.error('[Send DM Error]', e);
      if (String(e).includes('401') || String(e).includes('403')) {
        alert('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
      }
    }
  }

  function onIncomingMessage(msg) {
    // Only show if it's the active conversation
    if (st.conversationId && msg.conversationId === st.conversationId) {
      appendMessage(msg);
    }
    // TODO: update preview/unread in sidebar if needed
  }

  // ---- Init ----
  async function init() {
    try {
      st.currentUserId = window.state.currentUser?.id || null;

      // Bind send events
      const { dmSendBtn, dmInput } = els();
      dmSendBtn?.addEventListener('click', onSend);
      dmInput?.addEventListener('keydown', (e) => { if (e.key === 'Enter') onSend(); });

      // Load friends to both sidebar and center (dashboard)
      st.friends = await fetchFriends();
      renderFriendsSidebar(st.friends);
      renderFriendsCenter(st.friends);
      showDashboard(); // Default view like dm-ui

      // Robust WS connect (auto-reconnect)
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