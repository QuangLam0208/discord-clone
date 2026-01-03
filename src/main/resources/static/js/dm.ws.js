const DMWS = (() => {
  let wsConnected = false;
  let retryCount = 0;
  let subscriptionDM = null;
  let subscriptionFriends = null;
  let onDmMessageHandler = null;
  let onFriendEventHandler = null;
  let stompClient = null;

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

      try { subscriptionDM?.unsubscribe(); } catch {}
      try { subscriptionFriends?.unsubscribe(); } catch {}

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

  function disconnect() {
    try {
      if (subscriptionDM) subscriptionDM.unsubscribe();
      if (subscriptionFriends) subscriptionFriends.unsubscribe();
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

  return { connectWS, disconnect };
})();