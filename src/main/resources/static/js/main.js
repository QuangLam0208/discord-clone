const API_URL = '/api/users';

async function checkAuth() {
    const token = localStorage.getItem('accessToken');
    console.log("ACCESS TOKEN =", token);
    if (!token) {
        window.location.href = '/login.html';
        return;
    }

    try {
        const response = await fetch(`${API_URL}/me`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Token invalid');
        }

        const user = await response.json();
        console.log('User authenticated:', user);

        // Update UI with user info
        const usernameDisplay = document.getElementById('usernameDisplay');
        if (usernameDisplay) {
            usernameDisplay.textContent = user.username;
        }

    } catch (error) {
        console.error('Auth check failed:', error);
        logout();
    }
}

function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('tokenType');
    localStorage.removeItem('username');
    window.location.href = '/login.html';
}

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
});
