const DMApi = (() => {
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

  // Hàm xử lý lỗi chung
  function handleResponse(res) {
    if (res.status === 401 || res.status === 403) {
      alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }
    if (!res.ok) {
      throw new Error(`API Request failed with status ${res.status}`);
    }
    return res;
  }

  // ----- API: DM Chat (giữ nguyên) -----
  async function fetchFriends() {
    const res = await fetch('/api/friends', { headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }

  async function getOrCreateConversation(friendUserId) {
    const res = await fetch(`/api/direct-messages/conversation/by-user/${friendUserId}`, {
      headers: authHeaders(false), credentials: 'include'
    });
    handleResponse(res);
    return res.json();
  }

  async function fetchMessages(conversationId, page = 0, size = 50) {
    const res = await fetch(`/api/direct-messages/conversation/${conversationId}?page=${page}&size=${size}`, {
      headers: authHeaders(false), credentials: 'include'
    });
    handleResponse(res);
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
    handleResponse(res);
    return res.json();
  }

  // ----- API: Friends & Friend Requests -----
  async function listInboundRequests() {
    const res = await fetch('/api/friends/requests?type=inbound', { headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }
  async function listOutboundRequests() {
    const res = await fetch('/api/friends/requests?type=outbound', { headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }
  async function userByUsername(username) {
    const res = await fetch(`/api/users/username/${encodeURIComponent(username)}`, { headers: authHeaders(false), credentials: 'include' });
    if (res.status === 404) throw new Error('Không tìm thấy người dùng');
    handleResponse(res);
    return res.json();
  }
  async function sendFriendRequestByUsername(username) {
    const u = await userByUsername(username);
    const res = await fetch('/api/friends/requests', {
      method: 'POST',
      headers: authHeaders(true),
      credentials: 'include',
      body: JSON.stringify({ targetUserId: u.id })
    });
    handleResponse(res);
    return res.json();
  }
  async function acceptRequest(id) {
    const res = await fetch(`/api/friends/requests/${id}/accept`, { method: 'POST', headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }
  async function declineRequest(id) {
    const res = await fetch(`/api/friends/requests/${id}/decline`, { method: 'POST', headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }
  async function cancelRequest(id) {
    const res = await fetch(`/api/friends/requests/${id}`, { method: 'DELETE', headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
  }

  return {
    // DM chat
    getToken,
    fetchFriends,
    getOrCreateConversation,
    fetchMessages,
    sendMessage,
    // Friends
    listInboundRequests,
    listOutboundRequests,
    sendFriendRequestByUsername,
    acceptRequest,
    declineRequest,
    cancelRequest
  };
})();