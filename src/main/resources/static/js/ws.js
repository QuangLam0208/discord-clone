// WS: WebSocket Manager
window.Ws = {
    stompClient: null,
    connected: false,
    serverSubscription: null,

    connect: function () {
        if (this.connected) return;

        const sock = new SockJS('/ws');
        this.stompClient = Stomp.over(sock);
        this.stompClient.debug = null;

        const token = localStorage.getItem('accessToken');
        const headers = {};
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        this.stompClient.connect(headers, () => {
            this.connected = true;
            console.log("Global WS Connected");
            const userId = localStorage.getItem('userId');

            // Re-subscribe to server if we were on one
            if (state.currentServerId) {
                this.subscribeToServer(state.currentServerId);
            }

            if (userId) {
                this.stompClient.subscribe(`/topic/force-logout/${userId}`, (message) => {
                    this.handleLogout(message);
                });

                // Personal Notifications (Warn, Ban, Mute, etc.)
                const notifTopic = `/topic/user/${userId}/notifications`;
                console.log("Subscribing to notifications:", notifTopic);
                this.stompClient.subscribe(notifTopic, (message) => {
                    console.log("Notification received:", message.body);
                    const payload = JSON.parse(message.body);
                    Swal.fire({
                        title: payload.title,
                        text: payload.message,
                        icon: payload.icon || 'info',
                        background: '#313338',
                        color: '#fff',
                        confirmButtonColor: '#5865F2',
                        backdrop: `
                            rgba(0,0,123,0.4)
                            left top
                            no-repeat
                        `
                    }).then(() => {
                        // For bans, we might want to logout or reload
                        if (payload.penalty === 'BAN' || payload.penalty === 'GLOBAL_BAN') {
                            window.location.reload();
                        }
                    });
                });
            }
        }, () => {
            this.connected = false;
            setTimeout(() => this.connect(), 5000);
        });
    },

    subscribeToServer: function (serverId) {
        if (!this.connected || !this.stompClient) return;

        // Unsubscribe previous
        if (this.serverSubscription) {
            this.serverSubscription.unsubscribe();
            this.serverSubscription = null;
        }

        // Subscribe new
        this.serverSubscription = this.stompClient.subscribe(`/topic/server/${serverId}`, (message) => {
            const payload = JSON.parse(message.body);
            this.handleServerEvent(serverId, payload);
        });
        console.log("Subscribed to server updates:", serverId);
    },

    handleServerEvent: function (serverId, payload) {
        // payload = { type: "ROLE_UPDATE", data: ... }
        if (payload.type === 'MEMBER_UPDATE') {
            const data = payload.data;
            // Check if it's ME
            if (state.currentUser && data.userId == state.currentUser.id) {
                console.log("My roles updated, reloading permissions...");
                if (window.reloadPermissions) window.reloadPermissions(serverId);
            }
        }
        else if (payload.type === 'ROLE_UPDATE' || payload.type === 'ROLE_DELETE') {
            console.log("Role updated, reloading permissions...");
            if (window.reloadPermissions) window.reloadPermissions(serverId);
        }
    },

    handleLogout: function (message) {
        try {
            const payload = JSON.parse(message.body);
            if (payload?.type === 'FORCE_LOGOUT') {
                this.performLogout();
            }
        } catch (e) {
            this.performLogout();
        }
    },

    performLogout: function () {
        fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
            .finally(() => {
                localStorage.clear();
                window.location.href = '/login';
            });
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.Ws.connect();
});