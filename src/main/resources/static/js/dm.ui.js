const DMUI = (() => {
  let onSelectFriendRef = null;

  function els() {
    return {
      dmList: document.getElementById('dm-direct-list'),
      btnFriends: document.getElementById('btn-friends'),
      dmCenterList: document.getElementById('dm-center-list'),
      dmCenterCount: document.getElementById('dm-center-count'),
      dmDashboard: document.getElementById('dm-dashboard-view'),
      dmConversation: document.getElementById('dm-conversation-view'),
      dmName: document.getElementById('dm-active-friend-name'),
      dmMessages: document.getElementById('dm-messages'),
      dmInput: document.getElementById('dm-input'),
      dmSendBtn: document.getElementById('dm-send-btn'),
      dmTabAll: document.getElementById('dmTabAll'),
      dmTabPending: document.getElementById('dmTabPending'),
      dmTabAddFriend: document.getElementById('dmTabAddFriend'),
      dmAllView: document.getElementById('dm-all-view'),
      dmPendingView: document.getElementById('dm-pending-view'),
      dmAddFriendView: document.getElementById('dm-addfriend-view'),
      dmPendingInList: document.getElementById('dm-pending-in-list'),
      dmPendingOutList: document.getElementById('dm-pending-out-list'),
      dmAddFriendInput: document.getElementById('dm-addfriend-username'),
      dmAddFriendSend: document.getElementById('dm-addfriend-send'),
      dmAddFriendResult: document.getElementById('dm-addfriend-result'),
      dmSearch: document.getElementById('dm-center-search'),
      dmSearchContainer: document.getElementById('dm-search-container'),
    };
  }

  function showDashboard() {
    const { dmDashboard, dmConversation } = els();
    if (dmDashboard) dmDashboard.style.display = 'flex';
    if (dmConversation) dmConversation.style.display = 'none';
  }

  function showConversation() {
    const { dmDashboard, dmConversation } = els();
    if (dmDashboard) dmDashboard.style.display = 'none';
    if (dmConversation) dmConversation.style.display = 'flex';
  }

  function setActiveFriendName(name) {
    const { dmName, dmInput } = els();
    if (dmName) dmName.textContent = name || 'Tin nhắn trực tiếp';
    if (dmInput) dmInput.placeholder = `Nhắn tin cho @${name || ''}`;
  }

  function resetActiveItems() {
    const { dmList, btnFriends } = els();
    dmList?.querySelectorAll('.channel-item.dm-item').forEach(e => e.classList.remove('active'));
    btnFriends?.classList.remove('active');
  }

  function highlightFriendsButton() {
    resetActiveItems();
    const { btnFriends } = els();
    btnFriends?.classList.add('active');
  }

  function renderFriendsSidebar(friends, onSelectFriendCallback) {
    const { dmList } = els();
    if (!dmList) return;
    dmList.innerHTML = friends.map(friendSidebarItem).join('');
    dmList.querySelectorAll('.channel-item.dm-item').forEach(item => {
      item.addEventListener('click', () => {
        resetActiveItems();
        updateSidebarItem(Number(item.getAttribute('data-friend-id')), false);
        item.classList.add('active');
        const friendId = Number(item.getAttribute('data-friend-id'));
        const name = item.getAttribute('data-friend-name') || '';
        onSelectFriendCallback && onSelectFriendCallback(friendId, name);
      });
    });
  }

  function friendSidebarItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    const initials = (name || 'U').split(' ').map(s => s[0]).join('').slice(0, 2).toUpperCase();
    return `
      <div class="channel-item dm-item" id="dm-item-${f.friendUserId}" data-friend-id="${f.friendUserId}" data-friend-name="${name}">
        <div style="display:flex; align-items:center; gap:12px; flex:1; overflow:hidden;">
          <div style="width:32px; height:32px; border-radius:50%; background:#3f4147; color:#cfd1d5; display:flex; align-items:center; justify-content:center; font-size:14px; flex-shrink:0;">
            ${initials}
          </div>
          <div class="dm-name-container" style="display:flex; flex-direction:column; overflow:hidden;">
            <span class="dm-name-text" style="color:#96989d; font-weight:500; white-space:nowrap; text-overflow:ellipsis; overflow:hidden; font-size:15px;">
                ${name}
            </span>
          </div>
        </div>
      </div>`;
  }

  function updateSidebarItem(friendId, hasUnread) {
    const item = document.getElementById(`dm-item-${friendId}`);
    if (!item) return;
    const nameText = item.querySelector('.dm-name-text');
    if (nameText) {
      nameText.style.color = hasUnread ? '#ffffff' : '#96989d';
      nameText.style.fontWeight = hasUnread ? '700' : '500';
    }
  }

  function renderFriendsCenter(friends, onSelectFriendCallback) {
    const { dmCenterList, dmCenterCount } = els();
    if (!dmCenterList) return;
    if (dmCenterCount) dmCenterCount.textContent = String(friends.length);
    dmCenterList.innerHTML = friends.map(friendCenterItem).join('');
    dmCenterList.querySelectorAll('.dm-center-item').forEach(item => {
      item.addEventListener('click', () => {
        const friendId = Number(item.getAttribute('data-friend-id'));
        const sidebarItem = document.getElementById(`dm-item-${friendId}`);
        if (sidebarItem) sidebarItem.click();
      });
    });
  }

  function friendCenterItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    return `
      <div class="dm-center-item dm-header-item" data-friend-id="${f.friendUserId}" data-friend-name="${name}" style="display:flex; align-items:center; gap:12px; border-radius:8px;">
        <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
        <div style="display:flex; flex-direction:column;">
          <span style="color:#fff;">${name}</span>
          <small style="color:#b5bac1;">Online</small>
        </div>
      </div>`;
  }

  function isNearBottom() {
    const { dmMessages } = els();
    if (!dmMessages) return false;
    const threshold = 100;
    return (dmMessages.scrollTop + dmMessages.clientHeight) >= (dmMessages.scrollHeight - threshold);
  }

  // --- HTML CHO PHẦN INTRO (CHẶN & SPAM) ---
  function getDMIntroHtml(friend) {
    if (!friend) return '';
    const displayName = friend.displayName || friend.friendUsername || ('User ' + friend.friendUserId);
    const username = friend.friendUsername || ('user' + friend.friendUserId);
    const friendId = friend.friendUserId;
    const avatarUrl = friend.avatarUrl ? friend.avatarUrl : `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&size=128`;

    return `
      <div class="dm-intro-wrapper">
        <div class="dm-intro-avatar-large"><img src="${avatarUrl}"></div>
        <h1 class="dm-intro-displayname">${displayName}</h1>
        <h3 class="dm-intro-username">${username}</h3>
        <div class="dm-intro-text">
            Đây là phần mở đầu lịch sử tin nhắn trực tiếp của bạn với <strong>${displayName}</strong>.<br>Hai bạn có 1 máy chủ chung.
        </div>
        <div class="dm-intro-actions">
            <button class="btn-dm-action" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
            <button class="btn-dm-action" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}">Spam</button>
        </div>
      </div>`;
  }

  // --- RENDER MESSAGES VÀ GẮN SỰ KIỆN CLICK ---
  function renderMessages(list, currentUserId, displayedMsgIdsSet, friendInfo) {
    const { dmMessages } = els();
    if (!dmMessages) return;

    displayedMsgIdsSet?.clear();

    const messagesHtml = list.slice().reverse().map(m => {
      if (m.id != null && displayedMsgIdsSet) displayedMsgIdsSet.add(String(m.id));
      return msgBubble(m, currentUserId);
    }).join('');

    const introHtml = getDMIntroHtml(friendInfo);
    dmMessages.innerHTML = introHtml + messagesHtml;
    scrollToBottom(true);

    // --- XỬ LÝ CLICK NÚT ---
    const btnBlock = document.getElementById('btn-dm-block');
    const btnSpam = document.getElementById('btn-dm-spam');

    if (btnBlock) {
      btnBlock.onclick = async () => {
        const uid = btnBlock.getAttribute('data-id');
        const name = btnBlock.getAttribute('data-name');
        console.log('Bấm nút Chặn:', uid, name); // Debug
        if (confirm(`Bạn có chắc muốn chặn ${name}? Họ sẽ không thể nhắn tin cho bạn nữa.`)) {
          try {
            await DMApi.blockUser(uid);
            alert('Đã chặn thành công.');
            window.location.reload();
          } catch (e) {
            alert('Lỗi khi chặn: ' + e.message);
          }
        }
      };
    }

    if (btnSpam) {
      btnSpam.onclick = async () => {
        const uid = btnSpam.getAttribute('data-id');
        const name = btnSpam.getAttribute('data-name');
        console.log('Bấm nút Spam:', uid, name); // Debug
        if (confirm(`Báo cáo ${name} là Spam và chặn người này?`)) {
          try {
            await DMApi.reportUser(uid, 'SPAM');
            await DMApi.blockUser(uid);
            alert('Đã báo cáo và chặn thành công.');
            window.location.reload();
          } catch (e) {
            alert('Lỗi khi xử lý spam: ' + e.message);
          }
        }
      };
    }
  }

  function appendMessage(m, currentUserId, displayedMsgIdsSet) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    const mid = m.id != null ? String(m.id) : null;
    if (mid && displayedMsgIdsSet?.has(mid)) return;
    if (mid && displayedMsgIdsSet) displayedMsgIdsSet.add(mid);
    const wasNearBottom = isNearBottom();
    const isMine = String(m.senderId) === String(currentUserId);
    dmMessages.insertAdjacentHTML('beforeend', msgBubble(m, currentUserId));
    if (isMine || wasNearBottom) scrollToBottom(true);
  }

  function scrollToBottom(force = false) {
    const { dmMessages } = els();
    if (dmMessages && force) {
      setTimeout(() => { dmMessages.scrollTop = dmMessages.scrollHeight; }, 50);
    }
  }

  function msgBubble(m, currentUserId) {
    const mine = String(m.senderId) === String(currentUserId);
    const time = m.createdAt ? new Date(m.createdAt).toLocaleString() : 'Đang gửi...';
    const safe = (m.content || '').replace(/[&<>"']/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[s]));
    const isError = m.status === 'error';
    const errorStyle = isError ? 'border: 1px solid red;' : '';
    const cursor = isError ? 'cursor: pointer;' : '';
    const retryAttr = isError ? `onclick="DM.retryMessage('${m.tempId}')" title="Nhấn để gửi lại"` : '';

    return `
      <div class="message" id="msg-${m.id || m.tempId}" data-msg-id="${m.id || ''}" style="display:flex; ${mine ? 'justify-content:flex-end;' : ''}; opacity: ${m.status==='pending'?0.5:1};">
        <div style="max-width:70%; background:${mine ? '#4752c4' : '#2b2d31'}; color:#fff; padding:8px 12px; border-radius:8px; ${errorStyle} ${cursor}" ${retryAttr}>
          <div style="font-size:12px;color:#c7c7c7; margin-bottom:4px;">
            ${time} ${isError ? '<span style="color:red; margin-left:5px;">(Lỗi - Gửi lại)</span>' : ''}
          </div>
          <div>${safe}</div>
        </div>
      </div>`;
  }

  function replaceTempMessage(tempId, realMessage, currentUserId) {
    const tempEl = document.getElementById(`msg-${tempId}`);
    if (tempEl) tempEl.outerHTML = msgBubble(realMessage, currentUserId);
  }

  function markMessageError(tempId) {
    const tempEl = document.getElementById(`msg-${tempId}`);
    if (tempEl) {
      const bubble = tempEl.querySelector('div[style*="background"]');
      if (bubble) {
        bubble.style.border = '1px solid red';
        bubble.style.cursor = 'pointer';
        bubble.setAttribute('onclick', `DM.retryMessage('${tempId}')`);
        const timeDiv = bubble.querySelector('div:first-child');
        if (timeDiv && !timeDiv.innerHTML.includes('Lỗi')) timeDiv.innerHTML += '<span style="color:red; margin-left:5px;">(Lỗi - Gửi lại)</span>';
      }
      tempEl.style.opacity = '1';
    }
  }

  function setActiveTab(tab) {
    const { dmTabAll, dmTabPending, dmTabAddFriend, dmAllView, dmPendingView, dmAddFriendView, dmSearchContainer } = els();
    dmTabAll?.classList.toggle('active', tab === 'all');
    dmTabPending?.classList.toggle('active', tab === 'pending');
    dmTabAddFriend?.classList.toggle('active', tab === 'add');
    if (dmAllView) dmAllView.style.display = tab === 'all' ? 'block' : 'none';
    if (dmPendingView) dmPendingView.style.display = tab === 'pending' ? 'block' : 'none';
    if (dmAddFriendView) dmAddFriendView.style.display = tab === 'add' ? 'block' : 'none';
    if (dmSearchContainer) dmSearchContainer.style.display = tab === 'add' ? 'none' : 'flex';
  }

  function pendingInboundItem(r) {
    const name = r.senderUsername || ('User ' + r.senderId);
    return `
      <div class="dm-pending-item" style="display:flex; align-items:center; justify-content:space-between; padding:8px 12px; border-radius:8px; background:#2b2d31; color:#fff; margin-bottom:8px;">
        <div style="display:flex; align-items:center; gap:12px;">
          <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
          <div style="display:flex; flex-direction:column;">
            <span>${name}</span>
            <small style="color:#b5bac1;">Đã gửi lúc ${new Date(r.createdAt).toLocaleString()}</small>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="add-friend-btn" data-action="accept" data-request-id="${r.id}">Chấp nhận</button>
          <button class="tab-item" data-action="decline" data-request-id="${r.id}">Từ chối</button>
        </div>
      </div>`;
  }

  function pendingOutboundItem(r) {
    const name = r.recipientUsername || ('User ' + r.recipientId);
    return `
      <div class="dm-pending-item" style="display:flex; align-items:center; justify-content:space-between; padding:8px 12px; border-radius:8px; background:#2b2d31; color:#fff; margin-bottom:8px;">
        <div style="display:flex; align-items:center; gap:12px;">
          <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
          <div style="display:flex; flex-direction:column;">
            <span>${name}</span>
            <small style="color:#b5bac1;">Đã gửi lúc ${new Date(r.createdAt).toLocaleString()}</small>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="tab-item" data-action="cancel" data-request-id="${r.id}">Huỷ</button>
        </div>
      </div>`;
  }

  function renderPending(inbound, outbound) {
    const { dmPendingInList, dmPendingOutList } = els();
    if (dmPendingInList) dmPendingInList.innerHTML = inbound.map(pendingInboundItem).join('');
    if (dmPendingOutList) dmPendingOutList.innerHTML = outbound.map(pendingOutboundItem).join('');
  }

  async function reloadFriends() {
    try {
      const friends = await DMApi.fetchFriends();
      renderFriendsSidebar(friends, onSelectFriendRef);
      renderFriendsCenter(friends, onSelectFriendRef);
    } catch (e) {
      console.error('Reload friends failed', e);
    }
  }

  function initFriendsDashboard({ friends, onSelectFriend }) {
    onSelectFriendRef = onSelectFriend;
    setActiveTab('all');
    renderFriendsCenter(friends, onSelectFriend);
    const { dmSearch } = els();
    dmSearch?.addEventListener('input', (e) => {
      const q = e.target.value || '';
      if (document.getElementById('dm-all-view')?.style.display !== 'none') {
        const list = q ? friends.filter(f => (f.displayName || f.friendUsername || '').toLowerCase().includes(q.toLowerCase())) : friends;
        renderFriendsCenter(list, onSelectFriend);
      }
    });
    const { dmAddFriendInput, dmAddFriendSend, dmAddFriendResult } = els();
    dmAddFriendSend?.addEventListener('click', async () => {
      const username = dmAddFriendInput?.value?.trim();
      if (!username) return;
      dmAddFriendSend.disabled = true;
      dmAddFriendResult.textContent = 'Đang gửi yêu cầu...';
      dmAddFriendResult.style.color = '#b9bbbe';
      try {
        const created = await DMApi.sendFriendRequestByUsername(username);
        dmAddFriendResult.textContent = `Đã gửi lời mời tới ${created.recipientUsername}`;
        dmAddFriendResult.style.color = '#23a559';
        setActiveTab('pending');
        const inbound = await DMApi.listInboundRequests();
        const outbound = await DMApi.listOutboundRequests();
        renderPending(inbound, outbound);
      } catch (e) {
        const msg = e.message.replace('Error: ', '');
        if (msg.includes('Không tìm thấy')) {
            dmAddFriendResult.textContent = 'Không tìm thấy người dùng này.';
        } else {
            dmAddFriendResult.textContent = msg;
        }
        dmAddFriendResult.style.color = '#ed4245';
      } finally {
        dmAddFriendSend.disabled = false;
      }
    });
    const { dmTabAll, dmTabPending, dmTabAddFriend } = els();
    dmTabAll?.addEventListener('click', () => { setActiveTab('all'); reloadFriends(); });
    dmTabPending?.addEventListener('click', async () => {
      setActiveTab('pending');
      const inbound = await DMApi.listInboundRequests();
      const outbound = await DMApi.listOutboundRequests();
      renderPending(inbound, outbound);
      document.querySelectorAll('#dm-pending-in-list [data-action="accept"]').forEach(btn => {
        btn.addEventListener('click', async () => { try { await DMApi.acceptRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); await reloadFriends(); } catch (e) { alert('Không thể chấp nhận: ' + e); } });
      });
      document.querySelectorAll('#dm-pending-in-list [data-action="decline"]').forEach(btn => {
        btn.addEventListener('click', async () => { try { await DMApi.declineRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); await reloadFriends(); } catch (e) { alert('Không thể từ chối: ' + e); } });
      });
      document.querySelectorAll('#dm-pending-out-list [data-action="cancel"]').forEach(btn => {
        btn.addEventListener('click', async () => { try { await DMApi.cancelRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); await reloadFriends(); } catch (e) { alert('Không thể huỷ: ' + e); } });
      });
    });
    dmTabAddFriend?.addEventListener('click', () => { setActiveTab('add'); });
  }

  return {
    els, showDashboard, showConversation, setActiveFriendName, renderFriendsSidebar, updateSidebarItem,
    renderFriendsCenter, renderMessages, appendMessage, replaceTempMessage, markMessageError,
    scrollToBottom, resetActiveItems, highlightFriendsButton, initFriendsDashboard, setActiveTab,
    renderPending, reloadFriends
  };
})();