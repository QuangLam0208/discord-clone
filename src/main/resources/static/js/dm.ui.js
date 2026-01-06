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
      dmFileInput: document.getElementById('dm-file-input'),

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
      dmAttachmentPreview: document.getElementById('dm-attachment-preview'),
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

  function removeMessageElement(messageId) {
    const el = document.getElementById(`msg-${messageId}`);
    if (el) {
      el.style.transition = 'opacity 0.3s ease, height 0.3s ease';
      el.style.opacity = '0';
      // Đợi hiệu ứng mờ dần rồi xóa hẳn
      setTimeout(() => el.remove(), 300);
    }
  }

  function renderAttachmentPreview(files) {
    const { dmAttachmentPreview } = els();
    if (!dmAttachmentPreview) return;

    if (files.length === 0) {
      dmAttachmentPreview.style.display = 'none';
      dmAttachmentPreview.innerHTML = '';
      return;
    }

    dmAttachmentPreview.style.display = 'flex';
    dmAttachmentPreview.innerHTML = '';

    files.forEach((file, index) => {
      const item = document.createElement('div');
      item.className = 'attachment-preview-item';
      item.style.cssText = 'position: relative; width: 100px; height: 100px; border-radius: 8px; overflow: hidden; background: #202225; flex-shrink: 0; border: 1px solid #202225;';

      let innerContent = '';
      if (file.type.startsWith('image/')) {
        const url = URL.createObjectURL(file);
        innerContent = `<img src="${url}" style="width: 100%; height: 100%; object-fit: cover;">`;
      } else {
        innerContent = `
                <div style="width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 5px;">
                    <i class="fa-solid fa-file" style="font-size: 24px; color: #b9bbbe; margin-bottom: 5px;"></i>
                    <span style="font-size: 10px; color: #b9bbbe; text-align: center; word-break: break-all; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">${file.name}</span>
                </div>
            `;
      }

      item.innerHTML = `
            ${innerContent}
            <div onclick="window.DM.removeAttachment(${index})" style="position: absolute; top: 2px; right: 2px; background: #ed4245; color: white; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; font-size: 12px; cursor: pointer; box-shadow: 0 2px 4px rgba(0,0,0,0.5);">
                <i class="fa-solid fa-xmark"></i>
            </div>
        `;
      dmAttachmentPreview.appendChild(item);
    });
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
    const initials = (name || 'U').split(' ').map(s => s[0]).join('').slice(0, 2).toUpperCase();
    const avatar = f.avatarUrl
      ? `<img src="${f.avatarUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`
      : initials;

    return `
      <div class="dm-center-item dm-header-item" data-friend-id="${f.friendUserId}" data-friend-name="${name}" style="display:flex; align-items:center; gap:12px; border-radius:8px;">
        <div class="user-avatar" style="width:32px;height:32px;border-radius:50%;background:#5865F2;display:flex;align-items:center;justify-content:center;color:#fff;overflow:hidden;">
            ${avatar}
        </div>
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
      actionsHtml = `
            <button class="btn-dm-action" style="${btnStyle} ${btnBlue} opacity: 0.6; cursor: not-allowed;" disabled>Đã gửi yêu cầu kết bạn</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
        `;
    } else if (relationshipStatus === 'RECEIVED_REQUEST') {
      actionsHtml = `
            <button class="btn-dm-action" style="${btnStyle} ${btnBlue}" id="btn-dm-accept" data-id="${friendId}" data-name="${displayName}">Chấp nhận yêu cầu</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-decline" data-id="${friendId}" data-name="${displayName}">Bỏ qua</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnRed}" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}" data-blocked="false">Báo cáo Spam</button>
        `;
    } else {
      actionsHtml = `
            <button class="btn-dm-action" style="${btnStyle} ${btnBlue}" id="btn-dm-addfriend" data-id="${friendId}" data-name="${displayName}">Thêm bạn</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnGray}" id="btn-dm-block" data-id="${friendId}" data-name="${displayName}">Chặn</button>
            <button class="btn-dm-action" style="${btnStyle} ${btnRed}" id="btn-dm-spam" data-id="${friendId}" data-name="${displayName}" data-blocked="false">Báo cáo Spam</button>
        `;
    }

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

  // --- Logic Thời Gian---
  function formatDiscordTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const now = new Date();
    const oneDay = 24 * 60 * 60 * 1000;

    const isToday = now.getDate() === date.getDate() && now.getMonth() === date.getMonth() && now.getFullYear() === date.getFullYear();
    const isYesterday = new Date(now - oneDay).getDate() === date.getDate() && now.getMonth() === date.getMonth() && now.getFullYear() === date.getFullYear();

    const timeStr = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    if (isToday) return `${timeStr}`;
    if (isYesterday) return `Hôm qua lúc ${timeStr}`;
    return `${('0' + date.getDate()).slice(-2)}/${('0' + (date.getMonth() + 1)).slice(-2)}/${date.getFullYear()} ${timeStr}`;
  }

  // --- HÀM VẼ TIN NHẮN---
  function msgBubble(m, currentUserId, avatarMap = {}, friendName = 'Người dùng') {
    const mine = String(m.senderId) === String(currentUserId);
    const timeDisplay = formatDiscordTime(m.createdAt);
    const isError = m.status === 'error';
    const errorStyle = isError ? 'opacity: 0.5;' : '';
    const msgId = m.id || m.tempId;

    // Xác định tên người gửi
    const senderName = mine ? 'Bạn' : friendName;

    // Avatar
    const userAvatarUrl = avatarMap[m.senderId];
    const avatarImg = userAvatarUrl
      ? `<img src="${userAvatarUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`
      : `<div style="width:100%;height:100%;border-radius:50%;background:#5865F2;display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;">U</div>`;
    const avatarHtml = `<div class="user-avatar" style="width: 40px; height: 40px; margin-right: 16px; flex-shrink: 0; cursor: pointer;">${avatarImg}</div>`;

    // Reply Bar
    let replyHtml = '';
    if (m.replyToMessage) {
      replyHtml = `
            <div class="reply-bar" style="display: flex; align-items: center; margin-bottom: 4px; margin-left: 56px; font-size: 0.875rem; color: #b9bbbe; position: relative;">
                <div style="width: 34px; border-top: 2px solid #4f545c; border-left: 2px solid #4f545c; border-top-left-radius: 6px; height: 8px; margin-right: 8px; margin-top: 8px; position: absolute; left: -42px; top: 6px;"></div>
                <span style="opacity: 0.8; font-size: 13px; cursor: pointer;" onclick="window.DM.scrollToMessage('${m.replyToId || m.replyToMessage.id}')">
                    <strong style="color: #fff;">User</strong> đã trả lời: ${m.replyToMessage.content ? m.replyToMessage.content.substring(0, 50) + (m.replyToMessage.content.length > 50 ? '...' : '') : '[Tệp đính kèm]'}
                </span>
            </div>`;
    }

    // Attachments
    let attachmentsHtml = '';
    if (m.attachments && m.attachments.length > 0) {
      m.attachments.forEach(url => {
        if (url.match(/\.(jpeg|jpg|gif|png|webp)$/i)) {
          attachmentsHtml += `<div style="margin-top:5px;"><img src="${url}" style="max-width: 100%; max-height: 350px; border-radius: 4px; cursor: pointer;" onclick="window.open('${url}')"></div>`;
        } else {
          attachmentsHtml += `<div style="margin-top:5px; background: #2f3136; padding: 10px; border-radius: 4px; border: 1px solid #202225; display: inline-flex; align-items: center; gap: 8px;">
                        <i class="fa-solid fa-file" style="color: #b9bbbe; font-size: 24px;"></i>
                        <a href="${url}" target="_blank" style="color: #00b0f4; text-decoration: none; font-size: 14px;">Tải xuống tệp tin</a>
                    </div>`;
        }
      });
    }

    // Content
    let contentHtml = '';
    const rawContent = (m.content || '');
    const safeRawContent = rawContent.replace(/"/g, '&quot;');

    if (m.deleted) {
      contentHtml = `<em style="color: #9ca3af; font-size: 14px;">(Tin nhắn đã bị xóa)</em>`;
    } else {
      const safeContent = rawContent.replace(/[&<>"']/g, s => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]));
      const editedHtml = m.edited ? '<span style="font-size: 0.625rem; color: #72767d; margin-left: 4px;">(đã chỉnh sửa)</span>' : '';
      contentHtml = `<div id="msg-content-${msgId}" data-raw="${safeRawContent}" class="msg-content-text" style="color: #dcddde; font-size: 1rem; line-height: 1.375rem; white-space: pre-wrap;">${safeContent} ${editedHtml}</div>`;
    }

    // --- MENU POPUP ---
    const escapedRawContent = rawContent.replace(/`/g, '\\`').replace(/"/g, '&quot;');

    const menuHtml = `
          <div id="msg-menu-${msgId}" class="msg-menu-popover"
               style="display:none;
                      position: absolute;
                      right: 0;
                      bottom: 100%;
                      margin-bottom: 0px;
                      padding-bottom: 5px;
                      background-color: transparent;
                      width: 180px;
                      z-index: 99999;">

              <div style="background-color: #111214; border-radius: 4px; padding: 4px; border: 1px solid #202225; box-shadow: 0 8px 16px rgba(0,0,0,0.24);">
                  <div class="menu-item" style="padding: 8px; color: #b9bbbe; cursor: pointer; border-radius: 2px; font-size: 14px; display:flex; justify-content:space-between;"
                       onmouseover="this.style.backgroundColor='#5865F2';this.style.color='#fff'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#b9bbbe'"
                       onclick="event.stopPropagation(); DM.prepareReply('${msgId}', '${m.senderId}', \`${escapedRawContent}\`); DM.toggleMenu('${msgId}', false);">
                      <span>Trả lời</span> <i class="fa-solid fa-reply"></i>
                  </div>

                  <div class="menu-item" style="padding: 8px; color: #b9bbbe; cursor: pointer; border-radius: 2px; font-size: 14px; display:flex; justify-content:space-between;"
                       onmouseover="this.style.backgroundColor='#5865F2';this.style.color='#fff'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#b9bbbe'"
                       onclick="event.stopPropagation(); navigator.clipboard.writeText(\`${escapedRawContent}\`); DM.toggleMenu('${msgId}', false);">
                      <span>Sao chép văn bản</span> <i class="fa-solid fa-copy"></i>
                  </div>

                  ${mine && !m.deleted ? `
                  <div class="menu-item" style="padding: 8px; color: #b9bbbe; cursor: pointer; border-radius: 2px; font-size: 14px; display:flex; justify-content:space-between;"
                       onmouseover="this.style.backgroundColor='#5865F2';this.style.color='#fff'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#b9bbbe'"
                       onclick="event.stopPropagation(); DM.enterEditMode('${msgId}'); DM.toggleMenu('${msgId}', false);">
                      <span>Chỉnh sửa</span> <i class="fa-solid fa-pen"></i>
                  </div>
                  <div class="menu-item" style="padding: 8px; color: #ed4245; cursor: pointer; border-radius: 2px; font-size: 14px; display:flex; justify-content:space-between;"
                       onmouseover="this.style.backgroundColor='#ed4245';this.style.color='#fff'" onmouseout="this.style.backgroundColor='transparent';this.style.color='#ed4245'"
                       onclick="event.stopPropagation(); DM.deleteMessageConfirm('${msgId}'); DM.toggleMenu('${msgId}', false);">
                      <span>Xóa tin nhắn</span> <i class="fa-solid fa-trash"></i>
                  </div>
                  ` : ''}
              </div>
          </div>
        `;

    // Toolbar
    const toolbarHtml = `
            <div class="message-actions-toolbar" style="display: none; position: absolute; right: 16px; top: -16px; background-color: #36393f; border: 1px solid #26272d; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.2); z-index: 100; padding: 0 2px; align-items:center;">
                <div class="action-btn" style="width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #b9bbbe;"
                     onclick="DM.prepareReply('${msgId}', '${m.senderId}', \`${escapedRawContent}\`)" title="Trả lời">
                    <i class="fa-solid fa-reply"></i>
                </div>
                ${mine && !m.deleted ? `
                <div class="action-btn" style="width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #b9bbbe;"
                     onclick="DM.enterEditMode('${msgId}')" title="Chỉnh sửa">
                    <i class="fa-solid fa-pen"></i>
                </div>` : ''}

                <div class="action-btn btn-more-options" style="width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #b9bbbe; position: relative;"
                     onclick="event.stopPropagation(); DM.toggleMenu('${msgId}')" title="Thêm">
                    <i class="fa-solid fa-ellipsis"></i>
                    ${menuHtml}
                </div>
            </div>
        `;

    return `
          <div class="message-group" style="margin-top: 17px; position: relative;">
            ${replyHtml}

            <div class="message-wrapper" id="msg-${msgId}"
                 style="display: flex; position: relative; padding: 2px 16px; min-height: 2.75rem;"

                 onmouseenter="if(this.classList.contains('is-editing')) return;
                               this.querySelector('.message-actions-toolbar').style.display = 'flex';
                               this.style.backgroundColor = 'rgba(4, 4, 5, 0.07)';"

                 onmouseleave="if(this.classList.contains('is-editing')) return;
                               if(!this.querySelector('.msg-menu-popover') || this.querySelector('.msg-menu-popover').style.display === 'none') {
                                   this.querySelector('.message-actions-toolbar').style.display = 'none';
                                   this.style.backgroundColor = 'transparent';
                               }">

                ${avatarHtml}

                <div style="flex: 1; min-width: 0; ${errorStyle}">
                    <div style="display: flex; align-items: baseline; margin-bottom: 2px;">
                        <span style="font-weight: 500; color: #fff; margin-right: 8px; cursor: pointer; font-size: 1rem;">${senderName}</span>
                        <span style="font-size: 0.75rem; color: #72767d; margin-left: 0.25rem;">${timeDisplay}</span>
                    </div>
                    <div>${contentHtml}${attachmentsHtml}</div>
                    ${isError ? '<div style="color: #ed4245; font-size: 12px; margin-top: 2px; cursor: pointer;" onclick="DM.retryMessage(\'' + m.tempId + '\')">Gửi lỗi. Nhấn để thử lại.</div>' : ''}
                </div>

                ${!m.deleted && !isError ? toolbarHtml : ''}
            </div>
          </div>
        `;
  }

  function renderMessages(list, currentUserId, displayedMsgIdsSet, friendInfo, relationshipStatus, avatarMap, friendName) {
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
      return msgBubble(m, currentUserId, avatarMap, friendName);
    }).join('');

    const introHtml = getDMIntroHtml(friendInfo, relationshipStatus);
    dmMessages.innerHTML = introHtml + blockNoticeHtml + messagesHtml;

    scrollToBottom(true);

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

  function appendMessage(m, currentUserId, displayedMsgIdsSet, avatarMap, friendName) {
    const { dmMessages } = els();
    if (!dmMessages) return;
    const mid = m.id != null ? String(m.id) : null;
    if (mid && displayedMsgIdsSet?.has(mid)) return;
    if (mid && displayedMsgIdsSet) displayedMsgIdsSet.add(mid);
    const wasNearBottom = isNearBottom();
    const isMine = String(m.senderId) === String(currentUserId);
    dmMessages.insertAdjacentHTML('beforeend', msgBubble(m, currentUserId, avatarMap, friendName));
    if (isMine || wasNearBottom) scrollToBottom(true);
  }

  function scrollToBottom(force = false) {
    const { dmMessages } = els();
    if (dmMessages && force) {
      setTimeout(() => { dmMessages.scrollTop = dmMessages.scrollHeight; }, 50);
    }
  }

  function replaceTempMessage(tempId, realMessage, currentUserId, avatarMap, friendName) {
    const elId = `msg-${tempId}`;
    const tempEl = document.getElementById(elId);

    if (tempEl) {
      const group = tempEl.closest('.message-group');

      const newHtml = msgBubble(realMessage, currentUserId, avatarMap, friendName);

      if (group) {
        group.outerHTML = newHtml;
      } else {
        tempEl.outerHTML = newHtml;
      }

      console.log(`[UI] Replaced message ${tempId} successfully.`);
    } else {
      console.warn(`[UI] Cannot find message element with ID: ${elId} to replace.`);
    }
  }

  function markMessageError(tempId) {
    const tempEl = document.getElementById(`msg-${tempId}`);
    if (tempEl) {
      tempEl.style.opacity = '0.5';
      const content = tempEl.querySelector('.msg-content-text');
      if (content) content.style.color = '#ed4245';
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
    renderPending, reloadFriends, removeMessageElement, renderAttachmentPreview
  };
})();