const DMWS = (() => {
  let wsConnected = false;
  let retryCount = 0;
  let subscriptionDM = null;
  let subscriptionFriends = null;
  let subscriptionCurrentConv = null;
  let onDmMessageHandler = null;
  let onFriendEventHandler = null;
  let stompClient = null;

  // LƯU LẠI HỘI THOẠI HIỆN TẠI ĐỂ RE-SUBSCRIBE SAU RECONNECT
  let currentConversationId = null;
  let currentConvCallback = null;

  // ----- WS connect (robust) -----
  function connectWS(onDmMessage, onFriendEvent) {
    onDmMessageHandler = onDmMessage;
    onFriendEventHandler = onFriendEvent;

    if (wsConnected && stompClient?.connected) return;

    const token = DMApi.getToken();
    if (!token) {
      console.warn('No token found, skipping WS connect');
      return;
    }

    const socket = new SockJS('/ws', null, { transports: ['websocket'] });
    const client = Stomp.over(socket);
    client.debug = (str) => { if (str.includes('ERROR')) console.error('[STOMP]', str); };
    client.heartbeat.outgoing = 20000;
    client.heartbeat.incoming = 20000;

    const headers = { 'Authorization': `Bearer ${token}` };

    client.connect(headers, () => {
      wsConnected = true;
      retryCount = 0;
      stompClient = client;
      window.state.stompClient = client;
      console.log('[WS] CONNECTED');

      try { subscriptionDM?.unsubscribe(); } catch { }
      try { subscriptionFriends?.unsubscribe(); } catch { }
      try { subscriptionCurrentConv?.unsubscribe(); } catch { }

      // Subscribe DM queue
      subscriptionDM = client.subscribe('/user/queue/dm', (frame) => {
        try { const msg = JSON.parse(frame.body); onDmMessageHandler && onDmMessageHandler(msg); }
        catch (e) { console.error('WS parse DM error', e); }
      });
      console.log('[WS] SUBSCRIBED /user/queue/dm');

      // Subscribe Friends events (realtime cho sender/receiver)
      subscriptionFriends = client.subscribe('/user/queue/friends', (frame) => {
        try { const evt = JSON.parse(frame.body); onFriendEventHandler && onFriendEventHandler(evt); }
        catch (e) { console.error('WS parse Friends error', e); }
      });
      console.log('[WS] SUBSCRIBED /user/queue/friends');

      let currentUserId = localStorage.getItem('userId');

      // Fallback: Try decoding token if userId is missing
      if (!currentUserId && token) {
        try {
          const base64Url = token.split('.')[1];
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
          const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          }).join(''));
          const payload = JSON.parse(jsonPayload);
          if (payload.id) {
            currentUserId = String(payload.id);
            localStorage.setItem('userId', currentUserId); // Persist for future
            console.log('[WS] Extracted & Saved userId from token:', currentUserId);
          } else if (payload.sub) {
            console.warn('[WS] Token has sub but no id. Cannot subscribe to ID-based topic.');
          }
        } catch (e) {
          console.error('[WS] Token decode failed:', e);
        }
      }

      if (currentUserId) {
        const notifTopic = `/topic/user/${currentUserId}/notifications`;
        console.log('[WS] Subscribing to Personal Notifications:', notifTopic);

        client.subscribe(notifTopic, (message) => {
          console.log('[WS] Notification RECEIVED:', message.body);
          try {
            const payload = JSON.parse(message.body);

            // Show Popup
            Swal.fire({
              title: payload.title || 'Thông báo',
              text: payload.message || '',
              icon: payload.icon || 'info',
              background: '#313338',
              color: '#fff',
              confirmButtonColor: '#5865F2',
              backdrop: `rgba(0,0,0,0.5)`
            }).then(() => {
              if (payload.penalty === 'BAN' || payload.penalty === 'GLOBAL_BAN') {
                window.location.reload();
              }
            });
          } catch (e) {
            console.error('[WS] Error processing notification:', e);
          }
        });
      } else {
        console.error('[WS] CRITICAL: Unable to determine User ID. Notifications will not be received.');
      }

      // RE-SUBSCRIBE VÀO HỘI THOẠI ĐANG MỞ (NẾU CÓ)
      if (currentConversationId && currentConvCallback) {
        try { subscriptionCurrentConv?.unsubscribe(); } catch { }
        subscriptionCurrentConv = client.subscribe(`/topic/dm/${currentConversationId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body);
            currentConvCallback && currentConvCallback(payload);
          } catch (e) {
            console.error('WS parse conversation event error', e);
          }
        });
        console.log(`[WS] RE-SUBSCRIBED /topic/dm/${currentConversationId}`);
      }
    }, (err) => {
      console.error('WS connect error', err);
      wsConnected = false;
      const errStr = String(err);
      if (errStr.includes('401') || errStr.includes('403') || errStr.toLowerCase().includes('authentication')) {
        alert('Phiên đăng nhập hết hạn (WS). Vui lòng đăng nhập lại.');
        window.location.href = '/login';
        return;
      }
      scheduleReconnect();
    });
  }

  function scheduleReconnect() {
    if (wsConnected) return;
    const delay = Math.min(30000, 2000 * Math.pow(2, retryCount++));
    console.warn(`WS reconnect in ${delay}ms (attempt ${retryCount})`);
    setTimeout(() => connectWS(onDmMessageHandler, onFriendEventHandler), delay);
  }

  function subscribeToConversation(conversationId, callback) {
    if (!stompClient || !stompClient.connected || !conversationId) {
      // Lưu sẵn, sẽ re-subscribe sau khi connect xong
      currentConversationId = conversationId || null;
      currentConvCallback = callback || null;
      return;
    }

    // Hủy đăng ký hội thoại cũ nếu có (để tránh nghe lộn xộn)
    try { subscriptionCurrentConv?.unsubscribe(); } catch { }
    subscriptionCurrentConv = null;

    currentConversationId = conversationId;
    currentConvCallback = callback;

    // Đăng ký topic: /topic/dm/{conversationId}
    subscriptionCurrentConv = stompClient.subscribe(`/topic/dm/${conversationId}`, (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        callback && callback(payload);
      } catch (e) {
        console.error('WS parse conversation event error', e);
      }
    });
    console.log(`[WS] Subscribed to /topic/dm/${conversationId}`);
  }

  function disconnect() {
    try {
      if (subscriptionDM) subscriptionDM.unsubscribe();
      if (subscriptionFriends) subscriptionFriends.unsubscribe();
      if (subscriptionCurrentConv) subscriptionCurrentConv.unsubscribe();
      if (stompClient) stompClient.disconnect(() => console.log('[WS] Disconnected'));
    } catch (e) { console.error(e); }
    wsConnected = false;
    stompClient = null;
    retryCount = 0;
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      if (!wsConnected || !stompClient?.connected) {
        console.log('[WS] visibilitychange -> reconnect');
        connectWS(onDmMessageHandler, onFriendEventHandler);
      }
    }
  });

  return { connectWS, disconnect, subscribeToConversation };
})();