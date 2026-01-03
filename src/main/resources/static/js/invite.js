// INVITE MODULE

window.openInviteModal = async function () {
    const modal = document.getElementById('inviteModal');
    if (!modal) return;

    // Reset UI
    document.getElementById('invite-link-input').value = 'Đang tải...';
    document.getElementById('invite-friend-list').innerHTML = '<div style="text-align:center; color:#b9bbbe; padding: 10px;">Đang tải danh sách bạn bè...</div>';

    modal.classList.add('active');

    // 1. Get Invite Link
    try {
        const invite = await Api.post(`/api/invites/server/${state.currentServerId}`);
        console.log("Invite created:", invite);

        if (!invite || !invite.code) {
            console.error("Invite response missing code:", invite);
            document.getElementById('invite-link-input').value = 'Lỗi: Server trả về dữ liệu thiếu mã.';
            return;
        }

        // Assuming link format. If local dev, might just be code. 
        // Let's construct a full URL for UX
        const link = `${window.location.origin}/invite/${invite.code}`;
        document.getElementById('invite-link-input').value = link;
    } catch (e) {
        console.error(e);
        if (e.message && (e.message.includes('403') || e.message.includes('Access Denied'))) {
            document.getElementById('invite-link-input').value = 'Bạn không có quyền tạo lời mời.';
        } else {
            console.error(e);
            document.getElementById('invite-link-input').value = 'Lỗi tạo link.';
        }
    }

    // 2. Load Friends
    loadFriendsForInvite();
};

async function loadFriendsForInvite() {
    const listEl = document.getElementById('invite-friend-list');
    try {
        const friends = await Api.get('/api/friends');
        if (!friends || friends.length === 0) {
            listEl.innerHTML = '<div style="text-align:center; color:#b9bbbe; padding: 10px;">Không tìm thấy bạn bè nào.</div>';
            return;
        }

        // TODO: Filter out friends who are already in the server if logical
        // For now list all

        listEl.innerHTML = '';
        friends.forEach(friend => {
            const item = document.createElement('div');
            item.className = 'invite-friend-item';
            item.style.display = 'flex';
            item.style.alignItems = 'center';
            item.style.justifyContent = 'space-between';
            item.style.padding = '8px 0';
            item.style.borderBottom = '1px solid #2f3136';

            // Avatar & Name
            const info = document.createElement('div');
            info.style.display = 'flex';
            info.style.alignItems = 'center';
            info.style.gap = '10px';

            const avatar = document.createElement('div');
            avatar.style.width = '32px';
            avatar.style.height = '32px';
            avatar.style.borderRadius = '50%';
            avatar.style.backgroundColor = friend.avatarUrl ? 'transparent' : '#5865f2';
            if (friend.avatarUrl) {
                avatar.innerHTML = `<img src="${friend.avatarUrl}" style="width:100%;height:100%;border-radius:50%;">`;
            } else {
                avatar.style.display = 'flex';
                avatar.style.alignItems = 'center';
                avatar.style.justifyContent = 'center';
                avatar.style.color = 'white';
                avatar.style.fontSize = '12px';
                avatar.innerText = friend.friendUsername.substring(0, 2).toUpperCase();
            }

            const name = document.createElement('span');
            name.innerText = friend.displayName || friend.friendUsername;
            name.style.color = '#dcddde';
            name.style.fontWeight = '500';

            info.appendChild(avatar);
            info.appendChild(name);

            // Invite Button
            const btn = document.createElement('button');
            btn.innerText = 'Mời';
            btn.className = 'btn-invite-action'; // We will style this
            btn.style.backgroundColor = 'transparent';
            btn.style.border = '1px solid #23a559';
            btn.style.color = '#23a559';
            btn.style.padding = '4px 12px';
            btn.style.borderRadius = '3px';
            btn.style.cursor = 'pointer';
            btn.style.fontSize = '12px';
            btn.style.fontWeight = '500';
            btn.style.transition = 'all 0.2s';

            btn.onmouseover = () => { btn.style.backgroundColor = '#23a559'; btn.style.color = 'white'; };
            btn.onmouseout = () => { btn.style.backgroundColor = 'transparent'; btn.style.color = '#23a559'; };

            btn.onclick = async () => {
                try {
                    // Try to add member directly
                    await Api.post(`/api/servers/${state.currentServerId}/members/add?userId=${friend.friendUserId}`);
                    btn.innerText = 'Đã mời';
                    btn.disabled = true;
                    btn.style.borderColor = '#b9bbbe';
                    btn.style.color = '#b9bbbe';
                    btn.style.backgroundColor = 'transparent';
                    btn.onmouseover = null;
                } catch (e) {
                    // If failed (no permission), user might manually send link
                    // Or we can mock "Sent Invite"
                    console.warn("Direct add failed, might need permission:", e);
                    alert("Bạn không có quyền thêm thành viên trực tiếp. Hãy gửi link mời cho họ!");
                }
            };

            item.appendChild(info);
            item.appendChild(btn);
            listEl.appendChild(item);
        });

    } catch (e) {
        console.error(e);
        listEl.innerHTML = '<div style="text-align:center; color:#f04747;">Lỗi tải danh sách.</div>';
    }
}

window.copyInviteLink = function () {
    const input = document.getElementById('invite-link-input');
    input.select();
    input.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(input.value);

    const btn = document.getElementById('btn-copy-invite');
    const oldText = btn.innerText;
    btn.innerText = 'Đã chép';
    btn.style.backgroundColor = '#23a559';
    setTimeout(() => {
        btn.innerText = oldText;
        btn.style.backgroundColor = ''; // Revert to class style
    }, 2000);
};

window.filterInviteFriends = function () {
    const query = document.getElementById('invite-search-input').value.toLowerCase();
    const items = document.querySelectorAll('.invite-friend-item');
    items.forEach(item => {
        const name = item.querySelector('span').innerText.toLowerCase();
        if (name.includes(query)) {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
};