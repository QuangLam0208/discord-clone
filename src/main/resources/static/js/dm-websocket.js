// WebSocket Setup for Real-time Direct Messages
// Wrap in IIFE to avoid global namespace pollution
(function() {
    let stompClient = null;
    let dmSubscription = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const BASE_RECONNECT_DELAY = 1000; // 1 second
    const pendingDMMessages = [];
    const MAX_PENDING_MESSAGES = 100; // Limit array size to prevent memory leaks

    function connectWebSocket() {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.error('No access token found for WebSocket connection');
            return;
        }

        // Connect to WebSocket endpoint
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        // Add Authorization header
        const headers = {
            'Authorization': 'Bearer ' + token
        };

        // Connect to WebSocket
        stompClient.connect(headers, function(frame) {
            console.log('WebSocket Connected: ' + frame);
            reconnectAttempts = 0; // Reset reconnect counter on successful connection

            // Subscribe to direct messages
            dmSubscription = stompClient.subscribe('/user/queue/dm', function(message) {
                console.log('Received DM via WebSocket:', message.body);
                try {
                    const dmMessage = JSON.parse(message.body);
                    handleIncomingDirectMessage(dmMessage);
                } catch (e) {
                    console.error('Failed to parse DM message:', e);
                }
            });

            console.log('Subscribed to /user/queue/dm for real-time direct messages');
        }, function(error) {
            console.error('WebSocket connection error:', error);
            
            // Implement exponential backoff with retry limits
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++;
                const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1), 30000);
                console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
                setTimeout(connectWebSocket, delay);
            } else {
                console.error('Max reconnection attempts reached. Please refresh the page to reconnect.');
            }
        });
    }

    function disconnectWebSocket() {
        if (dmSubscription) {
            dmSubscription.unsubscribe();
            dmSubscription = null;
        }
        if (stompClient !== null) {
            stompClient.disconnect();
            stompClient = null;
        }
        console.log('WebSocket Disconnected');
    }

    function handleIncomingDirectMessage(dmMessage) {
        console.log('Handling incoming DM:', dmMessage);
        
        // Dispatch a custom event that other parts of the application can listen to
        const event = new CustomEvent('dmMessageReceived', { 
            detail: dmMessage 
        });
        window.dispatchEvent(event);
        
        // Store the message with size limit to prevent memory leaks
        pendingDMMessages.push(dmMessage);
        if (pendingDMMessages.length > MAX_PENDING_MESSAGES) {
            pendingDMMessages.shift(); // Remove oldest message
        }
        
        // Expose pending messages globally (read-only)
        window.pendingDMMessages = [...pendingDMMessages];
        
        // Show a notification to the user
        const senderName = dmMessage.senderName || dmMessage.senderId || 'Unknown User';
        const messagePreview = dmMessage.content ? 
            (dmMessage.content.length > 50 ? dmMessage.content.substring(0, 50) + '...' : dmMessage.content) : 
            'New message';
        
        // Use SweetAlert2 for notification (already loaded in the page)
        Swal.fire({
            title: 'New Message',
            html: `<strong>${senderName}</strong><br/>${messagePreview}`,
            icon: 'info',
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
            toast: true,
            background: '#36393f',
            color: '#fff'
        });
        
        // TODO: Once DM UI is fully implemented, add code here to:
        // 1. Update the conversation list if needed
        // 2. If the conversation is currently open, append the message to the chat
        // 3. Update unread message count
    }

    // Initialize WebSocket connection when page loads
    document.addEventListener('DOMContentLoaded', function() {
        connectWebSocket();
    });

    // Clean up on page unload
    window.addEventListener('beforeunload', function() {
        disconnectWebSocket();
    });

    // Expose the stompClient for use by other parts of the application
    window.getStompClient = function() {
        return stompClient;
    };
    
    // Allow manual reconnection
    window.reconnectWebSocket = function() {
        reconnectAttempts = 0;
        connectWebSocket();
    };
})();
