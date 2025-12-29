// auth.js
const API_AUTH = '/api/auth';
const API_USER = '/api/users';

/* =======================
   LOGIN
======================= */
async function login(username, password) {
    try {
        const response = await fetch(`${API_AUTH}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.message || 'Đăng nhập thất bại');
        }

        const data = await response.json();

        // LƯU TOKEN
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('username', data.username);

        // chỉ redirect 1 lần
        window.location.href = '/home.html';

    } catch (error) {
        console.error('Login error:', error);
        showError(error.message);
    }
}

/* =======================
   REGISTER
======================= */
async function register(username, email, password, displayName) {
    try {
        const response = await fetch(`${API_AUTH}/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, email, password, displayName })
        });

        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.message || 'Đăng ký thất bại');
        }

        alert('Đăng ký thành công! Vui lòng đăng nhập.');
        window.location.href = '/login.html';

    } catch (error) {
        console.error('Register error:', error);
        alert(error.message);
    }
}

/* =======================
   GET CURRENT USER (/me)
======================= */
async function getCurrentUser() {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        console.warn('No token, redirect to login');
        window.location.href = '/login.html';
        return;
    }

    const response = await fetch(`${API_USER}/me`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (response.status === 401) {
        console.warn('Token invalid / expired');
        logout();
        return;
    }

    if (!response.ok) {
        console.error('Failed to fetch current user');
        return;
    }

    const user = await response.json();
    console.log('Current user:', user);

    // ví dụ hiển thị username
    const usernameEl = document.getElementById('usernameDisplay');
    if (usernameEl) {
        usernameEl.textContent = user.username || user.displayName;
    }
}

/* =======================
   LOGOUT
======================= */
function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('username');
    window.location.href = '/login.html';
}

/* =======================
   UI HELPERS
======================= */
function showError(message) {
    const errorMessage = document.getElementById('errorMessage');
    if (errorMessage) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    } else {
        alert(message);
    }
}

/* =======================
   FORM EVENTS
======================= */
document.addEventListener('DOMContentLoaded', () => {

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            login(username, password);
        });
    }

    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const displayName = document.getElementById('displayName')?.value || username;
            register(username, email, password, displayName);
        });
    }
});
