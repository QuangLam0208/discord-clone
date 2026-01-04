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

      // Tabs Dashboard
      dmTabAll: document.getElementById('dmTabAll'),
      dmTabPending: document.getElementById('dmTabPending'),
      dmTabAddFriend: document.getElementById('dmTabAddFriend'),

      // Views Dashboard
      dmAllView: document.getElementById('dm-all-view'),
      dmPendingView: document.getElementById('dm-pending-view'),
      dmAddFriendView: document.getElementById('dm-addfriend-view'),

      // Lists in Dashboard
      dmPendingInList: document.getElementById('dm-pending-in-list'),
      dmPendingOutList: document.getElementById('dm-pending-out-list'),

      // Add Friend Input
      dmAddFriendInput: document.getElementById('dm-addfriend-username'),
      dmAddFriendSend: document.getElementById('dm-addfriend-send'),
      dmAddFriendResult: document.getElementById('dm-addfriend-result'),

      // Sidebar Search
      dmSidebarSearch: document.getElementById('dm-sidebar-search'),
      dmSearchResults: document.getElementById('dm-search-results'),

      // Center Search
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

  // --- RENDER SIDEBAR ---
  function renderFriendsSidebar(conversations, onSelectConversationCallback) {
    const { dmList } = els();
    if (!dmList) return;

    dmList.innerHTML = conversations.map(c => friendSidebarItem(c)).join('');

    dmList.querySelectorAll('.channel-item.dm-item').forEach(item => {
      item.addEventListener('click', () => {
        resetActiveItems();
        // UI: Mark as read
        const nameText = item.querySelector('.dm-name-text');
        if (nameText) {
            nameText.style.color = '#96989d';
            nameText.style.fontWeight = '500';
        }
        item.classList.add('active');

        const friendId = Number(item.getAttribute('data-friend-id'));
        const name = item.getAttribute('data-friend-name') || '';

        if (onSelectConversationCallback) {
            onSelectConversationCallback(friendId, name);
        }
      });
    });
  }

  function friendSidebarItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    const initials = (name || 'U').split(' ').map(s => s[0]).join('').slice(0, 2).toUpperCase();
    const avatar = f.avatarUrl
        ? `<img src="${f.avatarUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`
        : initials;

    const isUnread = f.unreadCount > 0;
    const color = isUnread ? '#ffffff' : '#96989d';
    const weight = isUnread ? '700' : '500';

    return `
      <div class="channel-item dm-item" id="dm-item-${f.friendUserId}" data-friend-id="${f.friendUserId}" data-friend-name="${name}">
        <div style="display:flex; align-items:center; gap:12px; flex:1; overflow:hidden;">
          <div style="width:32px; height:32px; border-radius:50%; background:#3f4147; color:#cfd1d5; display:flex; align-items:center; justify-content:center; font-size:14px; flex-shrink:0;">
            ${avatar}
          </div>
          <div class="dm-name-container" style="display:flex; flex-direction:column; overflow:hidden;">
            <span class="dm-name-text" style="color:${color}; font-weight:${weight}; white-space:nowrap; text-overflow:ellipsis; overflow:hidden; font-size:15px;">
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
        const name = item.getAttribute('data-friend-name');
        onSelectFriendCallback(friendId, name);
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

  // --- HTML CHO PHẦN INTRO (FIX: Avatar -> Tên hiển thị -> Tên hệ thống) ---
  function getDMIntroHtml(friend, relationshipStatus) {
      if (!friend) return '';
      const displayName = friend.displayName || friend.friendUsername || ('User ' + friend.friendUserId);
      const username = friend.friendUsername || ('user' + friend.friendUserId);
      const friendId = friend.friendUserId;
      const avatarUrl = friend.avatarUrl ? friend.avatarUrl : `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=random&size=128`;

      let actionsHtml = '';
      const btnStyle = 'padding: 6px 16px; border-radius: 3px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; margin-right: 8px; color: #fff; transition: background-color 0.2s;';
      const btnGray = 'background-color: #4f545c;';
      const btnRed = 'background-color: #ed4245;';
      const btnGreen = 'background-color: #248046;';
      const btnBlue = 'background-color: #5865F2;';

      if (relationshipStatus === 'FRIEND') {
          actionsHtml = `
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-unfriend" data-id="${friendId}" data-name="${displayName}">Xóa bạn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
          `;
      } else if (relationshipStatus === 'BLOCKED') {
          actionsHtml = `
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-unblock" data-id="${friendId}" data-name="${displayName}">Bỏ chặn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnRed}" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}" data-blocked="true">Báo cáo Spam</button>
          `;
      } else if (relationshipStatus === 'SENT_REQUEST') {
          // Mình gửi đi
          actionsHtml = `
              <button class="btn-dm-action" style="${btnStyle} ${btnBlue} opacity: 0.6; cursor: not-allowed;" disabled>Đã gửi yêu cầu kết bạn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
          `;
      } else if (relationshipStatus === 'RECEIVED_REQUEST') {
          // --- LOGIC MỚI: Người ta gửi đến mình (4 NÚT) ---
          actionsHtml = `
              <button class="btn-dm-action" style="${btnStyle} ${btnBlue}" id="btn-dm-accept" data-id="${friendId}" data-name="${displayName}">Chấp nhận yêu cầu</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-decline" data-id="${friendId}" data-name="${displayName}">Bỏ qua</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnRed}" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}" data-blocked="false">Báo cáo Spam</button>
          `;
      } else {
          // Người lạ
          actionsHtml = `
              <button class="btn-dm-action" style="${btnStyle} ${btnBlue}" id="btn-dm-addfriend" data-id="${friendId}" data-name="${displayName}">Thêm bạn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
              <button class="btn-dm-action" style="${btnStyle} ${btnRed}" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}" data-blocked="false">Báo cáo Spam</button>
          `;
      }

      // ... (Phần return HTML intro-wrapper)
      return `
        <div class="dm-intro-wrapper" style="margin: 16px; display: flex; flex-direction: column; gap: 8px;">
          <div class="dm-intro-avatar-large" style="width: 80px; height: 80px; margin-bottom: 12px;">
              <img src="${avatarUrl}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">
          </div>
          <h1 class="dm-intro-displayname" style="font-size: 28px; font-weight: 700; color: #fff; margin: 0;">${displayName}</h1>
          <h3 class="dm-intro-username" style="font-size: 18px; font-weight: 500; color: #b9bbbe; margin: 0;">${username}</h3>
          <div class="dm-intro-text" style="color: #b9bbbe; font-size: 14px; margin-top: 8px;">
              Đây là phần mở đầu lịch sử tin nhắn trực tiếp của bạn với <strong>${displayName}</strong>.
          </div>
          <div class="dm-intro-actions" style="margin-top: 16px;">
              ${actionsHtml}
          </div>
        </div>
        <div style="height: 1px; background-color: #3f4147; margin: 0 16px 16px 16px;"></div>`;
  }

  function renderMessages(list, currentUserId, displayedMsgIdsSet, friendInfo, relationshipStatus) {
    const { dmMessages } = els();
    if (!dmMessages) return;

    displayedMsgIdsSet?.clear();

    let messagesToRender = list;
    let blockNoticeHtml = '';

    if (relationshipStatus === 'BLOCKED') {
        messagesToRender = [];
        blockNoticeHtml = `
            <div style="display: flex; justify-content: center; margin: 20px 0;">
                <span style="background-color: #36393f; padding: 8px 12px; border-radius: 4px; color: #dcddde; font-size: 14px; border: 1px solid #202225;">
                    Tin nhắn đã bị ẩn vì bạn đang chặn người dùng này.
                </span>
            </div>
        `;
    }

    const messagesHtml = messagesToRender.slice().reverse().map(m => {
      if (m.id != null && displayedMsgIdsSet) displayedMsgIdsSet.add(String(m.id));
      return msgBubble(m, currentUserId);
    }).join('');

    const introHtml = getDMIntroHtml(friendInfo, relationshipStatus);
    dmMessages.innerHTML = introHtml + blockNoticeHtml + messagesHtml;

    scrollToBottom(true);

    // Bind Buttons
    const btnBlock = document.getElementById('btn-dm-block');
    const btnUnblock = document.getElementById('btn-dm-unblock');
    const btnSpam = document.getElementById('btn-dm-spam');
    const btnAddFriend = document.getElementById('btn-dm-addfriend');
    const btnUnfriend = document.getElementById('btn-dm-unfriend');
    const btnAccept = document.getElementById('btn-dm-accept');
    const btnDecline = document.getElementById('btn-dm-decline');

    if (btnBlock) btnBlock.onclick = () => window.DM && window.DM.handleBlock(btnBlock.getAttribute('data-id'));
    if (btnUnblock) btnUnblock.onclick = () => window.DM && window.DM.handleUnblock(btnUnblock.getAttribute('data-id'));
    if (btnSpam) btnSpam.onclick = () => window.DM && window.DM.handleSpam(btnSpam.getAttribute('data-id'), btnSpam.getAttribute('data-blocked') === 'true');
    if (btnAddFriend) btnAddFriend.onclick = () => window.DM && window.DM.handleAddFriend(btnAddFriend.getAttribute('data-id'));
    if (btnUnfriend) btnUnfriend.onclick = () => window.DM && window.DM.handleUnfriend(btnUnfriend.getAttribute('data-id'));
    if (btnAccept) btnAccept.onclick = () => window.DM && window.DM.handleAcceptRequest(btnAccept.getAttribute('data-id'));
    if (btnDecline) btnDecline.onclick = () => window.DM && window.DM.handleDeclineRequest(btnDecline.getAttribute('data-id'));
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
      <div class="message" id="msg-${m.id || m.tempId}" data-msg-id="${m.id || ''}" style="display:flex; ${mine ? 'justify-content:flex-end;' : ''}; opacity: ${m.status==='pending'?0.5:1}; margin-top: 10px;">
        <div style="max-width:70%; background:${mine ? '#5865f2' : '#2b2d31'}; color:#fff; padding:8px 12px; border-radius:8px; ${errorStyle} ${cursor}" ${retryAttr}>
          <div style="font-size:12px;color:#e0e0e0; margin-bottom:4px; font-weight: 500;">
             ${mine ? 'Bạn' : ''} <span style="font-weight: 400; opacity: 0.7; font-size: 11px; margin-left: 4px;">${time}</span>
             ${isError ? '<span style="color:red; margin-left:5px;">(Lỗi - Gửi lại)</span>' : ''}
          </div>
          <div style="white-space: pre-wrap; word-break: break-word; line-height: 1.4;">${safe}</div>
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

  function renderPending(inbound, outbound) {
    const { dmPendingInList, dmPendingOutList } = els();
    if (dmPendingInList) {
        dmPendingInList.innerHTML = inbound.map(r => {
            const name = r.senderUsername || ('User ' + r.senderId);
            return `
              <div class="dm-pending-item" style="display:flex; align-items:center; justify-content:space-between; padding:10px; border-radius:8px; background:#2b2d31; color:#fff; margin-bottom:8px; border: 1px solid #3f4147;">
                <div style="display:flex; align-items:center; gap:12px;">
                  <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
                  <div style="display:flex; flex-direction:column;">
                    <span style="font-weight: 600;">${name}</span>
                    <small style="color:#b5bac1;">Yêu cầu kết bạn đến</small>
                  </div>
                </div>
                <div style="display:flex; gap:8px;">
                  <button class="add-friend-btn" style="background-color: #248046;" data-action="accept" data-request-id="${r.id}">Chấp nhận</button>
                  <button class="add-friend-btn" style="background-color: #ed4245;" data-action="decline" data-request-id="${r.id}">Từ chối</button>
                </div>
              </div>`;
        }).join('');
    }
    if (dmPendingOutList) {
        dmPendingOutList.innerHTML = outbound.map(r => {
            const name = r.recipientUsername || ('User ' + r.recipientId);
            return `
              <div class="dm-pending-item" style="display:flex; align-items:center; justify-content:space-between; padding:10px; border-radius:8px; background:#2b2d31; color:#fff; margin-bottom:8px; border: 1px solid #3f4147;">
                <div style="display:flex; align-items:center; gap:12px;">
                  <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;"></div>
                  <div style="display:flex; flex-direction:column;">
                    <span style="font-weight: 600;">${name}</span>
                    <small style="color:#b5bac1;">Yêu cầu kết bạn đi</small>
                  </div>
                </div>
                <div style="display:flex; gap:8px;">
                  <button class="add-friend-btn" style="background-color: #4f545c;" data-action="cancel" data-request-id="${r.id}">Huỷ</button>
                </div>
              </div>`;
        }).join('');
    }
  }

  async function reloadFriends() {
    try {
      const friends = await DMApi.fetchFriends();
      renderFriendsCenter(friends, onSelectFriendRef);
    } catch (e) { console.error('Reload friends failed', e); }
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
        dmAddFriendResult.textContent = msg.includes('Không tìm thấy') ? 'Không tìm thấy người dùng này.' : msg;
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
      setTimeout(() => {
          document.querySelectorAll('#dm-pending-in-list [data-action="accept"]').forEach(btn => {
            btn.addEventListener('click', async () => { try { await DMApi.acceptRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); await reloadFriends(); } catch (e) { alert('Lỗi: ' + e); } });
          });
          document.querySelectorAll('#dm-pending-in-list [data-action="decline"]').forEach(btn => {
            btn.addEventListener('click', async () => { try { await DMApi.declineRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); } catch (e) { alert('Lỗi: ' + e); } });
          });
          document.querySelectorAll('#dm-pending-out-list [data-action="cancel"]').forEach(btn => {
            btn.addEventListener('click', async () => { try { await DMApi.cancelRequest(btn.getAttribute('data-request-id')); const i = await DMApi.listInboundRequests(); const o = await DMApi.listOutboundRequests(); renderPending(i, o); } catch (e) { alert('Lỗi: ' + e); } });
          });
      }, 100);
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