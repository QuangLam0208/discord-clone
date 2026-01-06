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

        this.stompClient.connect({}, () => {
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
            // A role changed. It MIGHT affect me. 
            // Simple approach: Always reload my permissions to be safe.
            // Optimized: Check if I have this role. 
            // But I don't easily know my role IDs without refetching. 
            // So just refetch permissions. It's cheap.
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