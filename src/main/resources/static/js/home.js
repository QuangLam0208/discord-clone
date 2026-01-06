window.state = {
  currentServerId: null,
  currentChannelId: null,
  currentUser: null,
  stompClient: null,
  tempCategoryId: null,
  tempCategoryId: null,
  pendingAttachments: [],
  serverPermissions: new Set() // Store current server permissions
};

window.elements = {
  app: document.getElementById('app'),
  userDisplay: document.getElementById('user-display'),
  userAvatars: document.querySelectorAll('.user-avatar'),
  sidebar: document.querySelector('.sidebar'),
};

window.addEventListener('unhandledrejection', (e) => {
  console.error('[App Unhandled Rejection]', e.reason);
});

window.reloadPermissions = async function (serverId) {
  if (!serverId) return;
  try {
    const permsList = await Api.get(`/api/servers/${serverId}/members/me/permissions`);
    state.serverPermissions = new Set(permsList.map(p => p.code));
    console.log("Updated permissions:", state.serverPermissions);

    // Refresh Chat UI (if active)
    if (window.loadMessages && state.currentChannelId) {
      // Reload to update edit/delete icons
      loadMessages(state.currentChannelId);
    }

    // Refresh Server Settings (if open)
    if (document.getElementById('serverSettingsModal') &&
      document.getElementById('serverSettingsModal').classList.contains('active')) {
      if (window.openServerSettings) window.openServerSettings(serverId);
    }
  } catch (e) {
    console.error("Failed to reload permissions", e);
  }
}

// ==================== APP BOOTSTRAP ====================
document.addEventListener('DOMContentLoaded', async function () {
  async function init() {
    try {
      if (elements.app) elements.app.style.display = 'flex';
      await loadMe();
      await loadServers();
      await DM.init();
      console.log('App Initialized & UI Synced (Server + DM)');
    } catch (err) {
      console.error('[App Init Error]', err);
    }
  }

  await init();
});