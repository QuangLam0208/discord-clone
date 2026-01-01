
// MAIN APP ENTRY POINT

// GLOBAL STATE & ELEMENTS
// Exposed globally so other modules can access
window.state = {
    currentServerId: null,
    currentChannelId: null,
    currentUser: null,
    stompClient: null,
    tempCategoryId: null, // Used for modals
    pendingAttachments: [] // Shared between upload.js and chat.js
};

window.elements = {
    app: document.getElementById('app'),
    userDisplay: document.getElementById('user-display'),
    userAvatars: document.querySelectorAll('.user-avatar'),
    sidebar: document.querySelector('.sidebar'), // Container chứa server list
};

document.addEventListener('DOMContentLoaded', async function () {
    // 2. INIT APPLICATION
    async function init() {
        if (elements.app) elements.app.style.display = 'flex';
        await loadMe(); // from user.js
        await loadServers(); // from server.js
        console.log("App Initialized & UI Synced");
    }

    // START APP
    await init();
});