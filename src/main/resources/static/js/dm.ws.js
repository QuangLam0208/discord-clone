const DMWS = (() => {
  let wsConnected = false;
  let retryCount = 0;
  let subscription = null;
  let onMessageHandler = null;
  let stompClient = null;

  // ----- WS connect (robust) -----
  function connectWS(onMessage) {
    onMessageHandler = onMessage;

    if (wsConnected && stompClient?.connected) return;

    const token = DMApi.getToken();
    if (!token) {
        console.warn('No token found, skipping WS connect');
        return;
    }

    const socket = new SockJS('/ws', null, { transports: ['websocket'] });
    socket.onopen = () => console.log('[SockJS] open');

    const client = Stomp.over(socket);
    client.debug = (str) => {
       // Chỉ log lỗi
       if (str.includes('ERROR')) console.error('[STOMP]', str);
    };

    // Tắt heartbeat log cho đỡ rối, giữ functionality
    client.heartbeat.outgoing = 20000;
    client.heartbeat.incoming = 20000;

    const headers = { 'Authorization': `Bearer ${token}` };

    client.connect(
      headers,
      () => {
        wsConnected = true;
        retryCount = 0;
        stompClient = client;
        window.state.stompClient = client; // Sync với global state nếu cần
        console.log('[WS] CONNECTED');

        try { subscription?.unsubscribe(); } catch {}

        // Subscribe user queue
        subscription = client.subscribe('/user/queue/dm', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            onMessageHandler && onMessageHandler(msg);
          } catch (e) { console.error('WS parse error', e); }
        });
        console.log('[WS] SUBSCRIBED /user/queue/dm');
      },
      (err) => {
        console.error('WS connect error', err);
        wsConnected = false;

        // Detect Auth Error in Frame
        const errStr = String(err);
        if (errStr.includes('401') || errStr.includes('403') || errStr.toLowerCase().includes('authentication')) {
             console.error('[WS] Auth Failed. Stopping retry.');
             alert('Phiên đăng nhập hết hạn (WS). Vui lòng đăng nhập lại.');
             window.location.href = '/login';
             return; // Stop retry
        }

        scheduleReconnect();
      }
    );
  }

  function scheduleReconnect() {
    if (wsConnected) return;
    const delay = Math.min(30000, 2000 * Math.pow(2, retryCount++));
    console.warn(`WS reconnect in ${delay}ms (attempt ${retryCount})`);
    setTimeout(() => connectWS(onMessageHandler), delay);
  }

  function disconnect() {
    try {
        if (subscription) subscription.unsubscribe();
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
        connectWS(onMessageHandler);
      }
    }
  });

  return { connectWS, disconnect };
})();