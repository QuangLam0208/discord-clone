// dm.ws.js
const DMWS = (() => {
  let stompClient = null;
  let connected = false;

  function connect(onConnected, onMessage) {
    const token = DMApi.getToken();
    const socket = new SockJS('/ws');
    const client = Stomp.over(socket);
    client.debug = null;

    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    client.connect(headers, () => {
      connected = true;
      stompClient = client;
      stompClient.subscribe('/user/queue/dm', (frame) => {
        try {
          const msg = JSON.parse(frame.body);
          onMessage && onMessage(msg);
        } catch (e) { console.error('WS parse error', e); }
      });
      onConnected && onConnected();
    }, (err) => {
      console.error('WS connect error', err);
      connected = false;
      stompClient = null;
    });
  }

  function isConnected() { return connected; }
  return { connect, isConnected };
})();