const DMUI = (() => {
  // ----- UI Elements -----
  function els() {
    // [LOGIC MỚI] Tìm nút Bạn bè một cách chính xác nhất
    // 1. Tìm theo ID trước
    let btnFriends = document.getElementById('btn-friends');

    // 2. Nếu không thấy ID, tìm phần tử .channel-item chứa icon 'fa-user-group'
    if (!btnFriends) {
        const icon = document.querySelector('.channel-sidebar .fa-user-group');
        if (icon) {
            btnFriends = icon.closest('.channel-item');
        }
    }

    return {
      dmList: document.getElementById('dm-direct-list'),
      dmCenterList: document.getElementById('dm-center-list'),
      dmCenterCount: document.getElementById('dm-center-count'),
      dmDashboard: document.getElementById('dm-dashboard-view'),
      dmConversation: document.getElementById('dm-conversation-view'),
      dmName: document.getElementById('dm-active-friend-name'),
      dmMessages: document.getElementById('dm-messages'),
      dmInput: document.getElementById('dm-input'),
      dmSendBtn: document.getElementById('dm-send-btn'),
      btnFriends: btnFriends // Trả về nút đã tìm được
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

  // === [QUẢN LÝ TRẠNG THÁI ACTIVE] ===

  // Xóa highlight của tất cả (gọi khi chuyển sang chat với user)
  function resetActiveItems() {
      const { dmList, btnFriends } = els();

      // 1. Xóa active ở danh sách user
      if (dmList) {
          dmList.querySelectorAll('.channel-item.dm-item').forEach(e => e.classList.remove('active'));
      }

      // 2. Xóa active ở nút Bạn bè (QUAN TRỌNG)
      if (btnFriends) {
          btnFriends.classList.remove('active');
      }
  }

  // Highlight nút Friends (gọi khi bấm logo hoặc nút bạn bè)
  function highlightFriendsButton() {
      // Trước tiên phải reset hết các user đang sáng
      resetActiveItems();

      const { btnFriends } = els();
      if (btnFriends) {
          btnFriends.classList.add('active');
      }
  }

  // --- Sidebar & List ---
  function renderFriendsSidebar(friends, onSelectFriendCallback) {
    const { dmList } = els();
    if (!dmList) return;
    dmList.innerHTML = friends.map(friendSidebarItem).join('');

    dmList.querySelectorAll('.channel-item.dm-item').forEach(item => {
      item.addEventListener('click', () => {
        // 1. QUAN TRỌNG: Xóa active của nút "Bạn bè" và các user khác
        resetActiveItems();

        // 2. Clear unread visual
        updateSidebarItem(Number(item.getAttribute('data-friend-id')), false);

        // 3. Active item vừa click
        item.classList.add('active');

        const friendId = Number(item.getAttribute('data-friend-id'));
        const name = item.getAttribute('data-friend-name') || '';
        if (onSelectFriendCallback) onSelectFriendCallback(friendId, name);
      });
    });
  }

  // Giao diện: Avatar + Tên
  function friendSidebarItem(f) {
    const name = f.displayName || f.friendUsername || ('User ' + f.friendUserId);
    const initials = (name || 'U').split(' ').map(s => s[0]).join('').slice(0,2).toUpperCase();

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

  // Đổi màu chữ (không badge)
  function updateSidebarItem(friendId, hasUnread) {
    const item = document.getElementById(`dm-item-${friendId}`);
    if (!item) return;
    const nameText = item.querySelector('.dm-name-text');
    if (!nameText) return;

    if (hasUnread) {
        nameText.style.color = '#ffffff';
        nameText.style.fontWeight = '700';
    } else {
        nameText.style.color = '#96989d';
        nameText.style.fontWeight = '500';
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
        const name = item.getAttribute('data-friend-name') || '';
        const sidebarItem = document.getElementById(`dm-item-${friendId}`);
        if(sidebarItem) sidebarItem.click();
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

  // --- Messages & Scrolling ---
  function isNearBottom() {
    const { dmMessages } = els();
    if (!dmMessages) return false;
    const threshold = 100;
    return (dmMessages.scrollTop + dmMessages.clientHeight) >= (dmMessages.scrollHeight - threshold);
  }

  function renderMessages(list, currentUserId, displayedMsgIdsSet) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    if (displayedMsgIdsSet) displayedMsgIdsSet.clear();

    dmMessages.innerHTML = list.slice().reverse().map(m => {
      if (m.id != null && displayedMsgIdsSet) displayedMsgIdsSet.add(String(m.id));
      return msgBubble(m, currentUserId);
    }).join('');
    scrollToBottom(true);
  }

  function appendMessage(m, currentUserId, displayedMsgIdsSet) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    const mid = m.id != null ? String(m.id) : null;
    if (mid && displayedMsgIdsSet && displayedMsgIdsSet.has(mid)) return;
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
            bubble.style.border = '1px solid red'; bubble.style.cursor = 'pointer';
            bubble.setAttribute('onclick', `DM.retryMessage('${tempId}')`);
            const timeDiv = bubble.querySelector('div:first-child');
            if(timeDiv && !timeDiv.innerHTML.includes('Lỗi')) timeDiv.innerHTML += '<span style="color:red; margin-left:5px;">(Lỗi - Gửi lại)</span>';
        }
        tempEl.style.opacity = '1';
    }
  }

  return {
    els,
    showDashboard,
    showConversation,
    setActiveFriendName,
    renderFriendsSidebar,
    updateSidebarItem,
    renderFriendsCenter,
    renderMessages,
    appendMessage,
    replaceTempMessage,
    markMessageError,
    scrollToBottom,
    resetActiveItems,
    highlightFriendsButton
  };
})();