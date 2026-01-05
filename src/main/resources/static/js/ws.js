// WS: kết nối STOMP và nghe force-logout theo userId
(function(){
    let stompClient = null;
    let connected = false;

    function connectWs() {
        try {
            const sock = new SockJS('/ws');
            stompClient = Stomp.over(sock);
            stompClient.debug = null; // tắt log

            stompClient.connect({}, function() {
                connected = true;
                const userId = localStorage.getItem('userId');
                if (!userId) return;

                const dest = `/topic/force-logout/${userId}`;
                stompClient.subscribe(dest, function(message) {
                    try {
                        const payload = JSON.parse(message.body);
                        if (payload?.type === 'FORCE_LOGOUT') {
                            // Gọi logout API để xoá cookie JWT
                            fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
                                .finally(() => {
                                    localStorage.clear();
                                    window.location.href = '/login';
                                });
                        }
                    } catch (e) {
                        // fallback: vẫn logout
                        fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
                            .finally(() => { localStorage.clear(); window.location.href = '/login'; });
                    }
                });
            }, function(err) {
                connected = false;
                // Thử reconnect sau 5s
                setTimeout(connectWs, 5000);
            });
        } catch (e) {
            setTimeout(connectWs, 5000);
        }
    }

    // Tự kết nối khi vào trang
    document.addEventListener('DOMContentLoaded', connectWs);
})();