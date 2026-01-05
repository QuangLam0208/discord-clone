// auth.js - service layer cho xác thực và user
const API_AUTH = '/api/auth';
const API_USER = '/api/users';

function getToken() {
  return localStorage.getItem('accessToken');
}

function setAuth(data) {
  // Backend trả về { token, type, user: { id, username, displayName, ... } }
  const token = data?.token || data?.accessToken;
  const user = data?.user;

  if (token) localStorage.setItem('accessToken', token);
  if (user?.id) localStorage.setItem('userId', String(user.id));
  if (user?.username) localStorage.setItem('username', user.username);
  if (user?.displayName || user?.username) {
    localStorage.setItem('displayName', user.displayName || user.username);
  }
}

function clearAuth() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
  localStorage.removeItem('displayName');
}

function normalizeError(response, data) {
  const serverMessage = data && (data.message || data.error);
  if (serverMessage) return serverMessage;

  if ([400, 401, 403].includes(response?.status)) {
    return 'Sai tài khoản hoặc mật khẩu';
  }
  return 'Đã xảy ra lỗi. Vui lòng thử lại sau.';
}

/* =======================
   LOGIN
======================= */
async function login(username, password) {
  try {
    const response = await fetch(`${API_AUTH}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include', // nhận cookie JWT httpOnly nếu server set
      body: JSON.stringify({ username, password })
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(normalizeError(response, data));
    }

    setAuth(data);
    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
}

/* =======================
   REGISTER
======================= */
async function register(username, email, password, displayName) {
  try {
    const response = await fetch(`${API_AUTH}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, email, password, displayName })
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(normalizeError(response, data));
    }

    // Backend đã login ngay sau register -> setAuth luôn
    setAuth(data);
    return data;
  } catch (error) {
    console.error('Register error:', error);
    throw error;
  }
}

/* =======================
   GET CURRENT USER (/me)
======================= */
async function getCurrentUser() {
  const token = getToken();
  if (!token) {
    window.location.href = '/login';
    return null;
  }

  const response = await fetch(`${API_USER}/me`, {
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` },
    credentials: 'include'
  });

  if (response.status === 401) {
    await logout(); // gọi logout server + clear client
    return null;
  }

  if (!response.ok) {
    console.error('Failed to fetch current user');
    return null;
  }

  const user = await response.json();
  return user;
}

/* =======================
   LOGOUT
======================= */
async function logout() {
  try {
    // Xóa cookie JWT phía server (nếu có) + clear client
    await fetch(`${API_AUTH}/logout`, { method: 'POST', credentials: 'include' });
  } catch (e) {
    // ignore network error, vẫn clear client
  } finally {
    clearAuth();
    window.location.href = '/login';
  }
}

/* =======================
   REALTIME FORCE LOGOUT (WS)
======================= */
// Kết nối WS và subscribe kênh force-logout theo user (dùng JwtChannelInterceptor)
// Yêu cầu: SockJS + Stomp đã được include (ở layout): sockjs.min.js, stomp.min.js
let stompClient = null;
let wsConnected = false;
let wsReconnectTimer = null;

function initForceLogout() {
  // Tránh kết nối nhiều lần
  if (wsConnected) return;
  const token = getToken();
  const userId = localStorage.getItem('userId');
  if (!token || !userId) {
    return; // chưa đăng nhập -> không kết nối
  }

  try {
    const sock = new SockJS('/ws');
    stompClient = Stomp.over(sock);
    stompClient.debug = null; // tắt log để console sạch

    // Truyền JWT qua header 'Authorization' để JwtChannelInterceptor xác thực STOMP CONNECT
    const headers = { Authorization: `Bearer ${token}` };

    stompClient.connect(headers, function onConnect() {
      wsConnected = true;
      // Subscribe user destination: /user/queue/force-logout (server convertAndSendToUser(..., "/queue/force-logout", ...))
      const sub = stompClient.subscribe('/user/queue/force-logout', async function(message) {
        try {
          const payload = JSON.parse(message.body);
          if (payload?.type === 'FORCE_LOGOUT') {
            // Gọi logout để hủy session ngay
            await logout();
          } else {
            // fallback vẫn logout
            await logout();
          }
        } catch (e) {
          await logout();
        }
      });
    }, function onError(err) {
      wsConnected = false;
      // thử reconnect sau 5s
      if (!wsReconnectTimer) {
        wsReconnectTimer = setTimeout(() => {
          wsReconnectTimer = null;
          initForceLogout();
        }, 5000);
      }
    });
  } catch (e) {
    // thử reconnect sau 5s
    if (!wsReconnectTimer) {
      wsReconnectTimer = setTimeout(() => {
        wsReconnectTimer = null;
        initForceLogout();
      }, 5000);
    }
  }
}

// Gọi init khi app khởi tạo (sau khi đã có token)
document.addEventListener('DOMContentLoaded', () => {
  initForceLogout();
});

/* =======================
   UI HELPERS (SweetAlert2)
======================= */
function showToastSuccess(title = 'Thành công') {
  const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 1500,
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.addEventListener('mouseenter', Swal.stopTimer);
      toast.addEventListener('mouseleave', Swal.resumeTimer);
    }
  });
  return Toast.fire({ icon: 'success', title });
}

function showErrorAlert(message, title = 'Có lỗi xảy ra') {
  return Swal.fire({
    icon: 'error',
    title,
    text: message,
    background: '#36393f',
    color: '#fff'
  });
}

// Export theo global (nếu không dùng module bundler)
window.AuthService = {
  login,
  register,
  getCurrentUser,
  logout,
  initForceLogout,
  showToastSuccess,
  showErrorAlert
};