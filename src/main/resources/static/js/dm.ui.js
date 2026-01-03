// dm.ui.js
const DMUI = (() => {
  const els = {
    friends: () => document.getElementById('dm-friends-list'),
    messages: () => document.getElementById('dm-messages'),
    activeName: () => document.getElementById('dm-active-friend-name'),
    input: () => document.getElementById('dm-input'),
  };

  function friendItemTemplate(friend) {
    const name = friend.displayName || friend.friendUsername || ('User ' + friend.friendUserId);
    const avatar = friend.avatarUrl || '';
    return `
      <div class="dm-header-item" data-friend-id="${friend.friendUserId}" style="display:flex; align-items:center; gap:10px; cursor:pointer; padding:8px 12px; border-radius:4px;">
        <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;overflow:hidden;">
          ${avatar ? `<img src="${avatar}" alt="${name}" style="width:100%;height:100%;object-fit:cover;" />` : ''}
        </div>
        <div style="display:flex;flex-direction:column;">
          <span style="color:#fff;">${name}</span>
          <small style="color:#b5bac1;">@${friend.friendUsername || ''}</small>
        </div>
      </div>`;
  }

  function renderFriends(friends, onClick) {
    const container = els.friends();
    container.innerHTML = friends.map(friendItemTemplate).join('');
    container.querySelectorAll('.dm-header-item').forEach(item => {
      item.addEventListener('click', () => {
        const friendId = Number(item.getAttribute('data-friend-id'));
        container.querySelectorAll('.dm-header-item').forEach(e => e.classList.remove('active'));
        item.classList.add('active');
        onClick && onClick(friendId);
      });
    });
  }

  function setActiveFriendName(name) {
    els.activeName().textContent = name || '';
  }

  function messageBubbleTemplate(m, currentUserId) {
    const mine = m.senderId === currentUserId;
    const time = new Date(m.createdAt).toLocaleString();
    return `
      <div class="message" style="display:flex; ${mine ? 'justify-content:flex-end;' : ''}">
        <div style="max-width:70%; background:${mine ? '#4752c4' : '#2b2d31'}; color:#fff; padding:8px 12px; border-radius:8px;">
          <div style="font-size:12px;color:#c7c7c7; margin-bottom:4px;">${time}</div>
          <div>${escapeHtml(m.content || '')}</div>
        </div>
      </div>`;
  }

  function renderMessages(list, currentUserId) {
    const container = els.messages();
    container.innerHTML = list.slice().reverse().map(m => messageBubbleTemplate(m, currentUserId)).join('');
    container.scrollTop = container.scrollHeight;
  }

  function appendMessage(m, currentUserId) {
    const container = els.messages();
    container.insertAdjacentHTML('beforeend', messageBubbleTemplate(m, currentUserId));
    container.scrollTop = container.scrollHeight;
  }

  function clearMessages() {
    els.messages().innerHTML = '';
  }

  function getInput() { return els.input().value.trim(); }
  function clearInput() { els.input().value = ''; }

  function escapeHtml(str) {
    return str.replace(/[&<>"']/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[s]));
  }

  return { renderFriends, setActiveFriendName, renderMessages, appendMessage, clearMessages, getInput, clearInput };
})();