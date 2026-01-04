const DMApi = (() => {
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

  async function fetchFriends() {
    const res = await fetch('/api/friends', { headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }

  // API MỚI: Lấy danh sách hội thoại
  async function fetchAllConversations() {
    const res = await fetch('/api/direct-messages/conversations', { headers: authHeaders(false), credentials: 'include' });
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
    if (res.status === 404) return null; // Sửa để không throw lỗi khi search không thấy
    handleResponse(res);
    return res.json();
  }
  async function sendFriendRequestByUsername(username) {
      // Logic cũ: tìm user -> gửi request
      const u = await userByUsername(username);
      if(!u) throw new Error("Không tìm thấy người dùng");
      return sendFriendRequestById(u.id);
  }

  // Mới: Gửi request bằng ID (dùng cho nút Thêm bạn)
  async function sendFriendRequestById(id) {
      const res = await fetch('/api/friends/requests', {
        method: 'POST',
        headers: authHeaders(true),
        body: JSON.stringify({ targetUserId: id })
      });
      if (!res.ok) {
          const errData = await res.json();
          throw new Error(errData.message || 'Gửi yêu cầu thất bại');
      }
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

  async function blockUser(targetUserId) {
      const res = await fetch(`/api/friends/block/${targetUserId}`, {
        method: 'POST', headers: authHeaders(true), credentials: 'include'
      });
      handleResponse(res);
  }

  // Mới: Unblock
  async function unblockUser(targetUserId) {
      const res = await fetch(`/api/friends/unblock/${targetUserId}`, {
        method: 'POST', headers: authHeaders(true), credentials: 'include'
      });
      handleResponse(res);
  }

  // Mới: Unfriend
  async function unfriend(targetUserId) {
      const res = await fetch(`/api/friends/${targetUserId}`, {
        method: 'DELETE', headers: authHeaders(true), credentials: 'include'
      });
      handleResponse(res);
  }

  async function reportUser(targetUserId, type = 'SPAM') {
      const res = await fetch('/api/reports', {
        method: 'POST', headers: authHeaders(true), credentials: 'include',
        body: JSON.stringify({ targetUserId: Number(targetUserId), type: type })
      });
      handleResponse(res);
  }

  return {
    getToken,
    fetchFriends,
    fetchAllConversations, // New
    getOrCreateConversation,
    fetchMessages,
    sendMessage,
    listInboundRequests,
    listOutboundRequests,
    userByUsername,
    sendFriendRequestByUsername,
    sendFriendRequestById, // New
    acceptRequest,
    declineRequest,
    cancelRequest,
    blockUser,
    unblockUser, // New
    unfriend, // New
    reportUser
  };
})();