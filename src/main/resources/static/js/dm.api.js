// dm.api.js
const DMApi = (() => {
  const base = '/api';
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

  async function fetchFriends() {
    const res = await fetch(`${base}/friends`, {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Fetch friends failed');
    return res.json();
  }

  async function getOrCreateConversation(friendUserId) {
    const res = await fetch(`${base}/direct-messages/conversation/by-user/${friendUserId}`, {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Get/Create conversation failed');
    return res.json();
  }

  async function fetchMessages(conversationId, page = 0, size = 50) {
    const res = await fetch(`${base}/direct-messages/conversation/${conversationId}?page=${page}&size=${size}`, {
      method: 'GET',
      headers: authHeaders(false),
      credentials: 'include'
    });
    if (!res.ok) throw new Error('Fetch messages failed');
    const pageData = await res.json();
    return pageData.content || [];
  }

  async function sendMessage(receiverId, content) {
    const body = { receiverId, content };
    const res = await fetch(`${base}/direct-messages`, {
      method: 'POST',
      headers: authHeaders(true),
      credentials: 'include',
      body: JSON.stringify(body)
    });
    if (!res.ok) throw new Error('Send message failed');
    return res.json();
  }

  return { fetchFriends, getOrCreateConversation, fetchMessages, sendMessage, getToken };
})();