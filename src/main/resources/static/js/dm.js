// dm.js
const DM = (() => {
  let state = {
    currentUserId: null,
    friends: [],
    active: { friendId: null, friendName: '', conversationId: null }
  };

  async function init() {
    state.currentUserId = readUserIdFromToken(DMApi.getToken());

    state.friends = await DMApi.fetchFriends();
    DMUI.renderFriends(state.friends, onSelectFriend);

    DMWS.connect(
      () => console.log('WS connected'),
      onIncomingMessage
    );

    if (state.friends.length > 0) {
      await onSelectFriend(state.friends[0].friendUserId);
    }

    document.getElementById('dm-send-btn')?.addEventListener('click', onSend);
    document.getElementById('dm-input')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') onSend();
    });
  }

  async function onSelectFriend(friendId) {
    const friend = state.friends.find(f => f.friendUserId === friendId);
    state.active.friendId = friendId;
    state.active.friendName = friend?.displayName || friend?.friendUsername || ('User ' + friendId);
    DMUI.setActiveFriendName(state.active.friendName);

    const conv = await DMApi.getOrCreateConversation(friendId);
    state.active.conversationId = conv.id;

    const messages = await DMApi.fetchMessages(state.active.conversationId, 0, 50);
    DMUI.renderMessages(messages, state.currentUserId);
  }

  async function onSend() {
    if (!state.active.friendId) return;
    const content = DMUI.getInput();
    if (!content) return;
    try {
      const sent = await DMApi.sendMessage(state.active.friendId, content);
      DMUI.appendMessage(sent, state.currentUserId);
      DMUI.clearInput();
      if (!state.active.conversationId && sent.conversationId) {
        state.active.conversationId = sent.conversationId;
      }
    } catch (e) {
      console.error(e);
      alert('Gửi tin nhắn lỗi');
    }
  }

  function onIncomingMessage(msg) {
    if (state.active.conversationId && msg.conversationId === state.active.conversationId) {
      DMUI.appendMessage(msg, state.currentUserId);
    }
  }

  function readUserIdFromToken(token) {
    try {
      if (!token) return null;
      const payload = JSON.parse(atob(token.split('.')[1] || ''));
      return payload?.id || payload?.userId || null;
    } catch {
      return null;
    }
  }

  return { init };
})();

document.addEventListener('DOMContentLoaded', () => {
  const root = document.getElementById('dm-root');
  if (root) DM.init();
});