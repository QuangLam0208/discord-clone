window.state = {
  currentServerId: null,
  currentChannelId: null,
  currentUser: null,
  stompClient: null,
  tempCategoryId: null,
  pendingAttachments: []
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