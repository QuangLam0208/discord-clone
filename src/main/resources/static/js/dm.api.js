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

  async function sendMessage(receiverId, content, attachments = [], replyToId = null) {
    const body = { receiverId, content, attachments, replyToId };
    const res = await fetch('/api/direct-messages', {
      method: 'POST',
      headers: authHeaders(true),
      credentials: 'include',
      body: JSON.stringify(body)
    });
    handleResponse(res);
    return res.json();
  }

  async function editMessage(messageId, content) {
      const res = await fetch(`/api/direct-messages/${messageId}`, {
          method: 'PUT',
          headers: authHeaders(true),
          body: JSON.stringify({ content })
      });
      handleResponse(res);
      return res.json();
  }

  async function deleteMessage(messageId) {
      const res = await fetch(`/api/direct-messages/${messageId}`, {
          method: 'DELETE',
          headers: authHeaders(false)
      });
      if (!res.ok) throw new Error("Delete failed");
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
    if (res.status === 404) return null;
    handleResponse(res);
    return res.json();
  }
  async function sendFriendRequestByUsername(username) {
      const u = await userByUsername(username);
      if(!u) throw new Error("Không tìm thấy người dùng");
      return sendFriendRequestById(u.id);
  }

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

  async function unblockUser(targetUserId) {
      const res = await fetch(`/api/friends/unblock/${targetUserId}`, {
        method: 'POST', headers: authHeaders(true), credentials: 'include'
      });
      handleResponse(res);
  }

  async function unfriend(targetUserId) {
      const res = await fetch(`/api/friends/${targetUserId}`, {
        method: 'DELETE', headers: authHeaders(true), credentials: 'include'
      });
      handleResponse(res);
  }

  async function fetchBlockedUsers() {
    const res = await fetch('/api/friends/blocked', { headers: authHeaders(false), credentials: 'include' });
    handleResponse(res);
    return res.json();
  }

  async function reportUser(targetUserId, type = 'SPAM') {
      const res = await fetch('/api/reports', {
        method: 'POST', headers: authHeaders(true), credentials: 'include',
        body: JSON.stringify({ targetUserId: Number(targetUserId), type: type })
      });
      handleResponse(res);
  }

  async function getMe() {
      const res = await fetch('/api/users/me', { headers: authHeaders(false) });
      handleResponse(res);
      return res.json();
  }

  return {
    getToken,
    fetchFriends,
    fetchAllConversations,
    getOrCreateConversation,
    fetchMessages,
    sendMessage,
    listInboundRequests,
    listOutboundRequests,
    userByUsername,
    sendFriendRequestByUsername,
    sendFriendRequestById,
    acceptRequest,
    declineRequest,
    cancelRequest,
    blockUser,
    unblockUser,
    unfriend,
    reportUser,
    fetchBlockedUsers,
    editMessage,
    deleteMessage,
    getMe
  };
})();