// auth.js - service layer cho xác thực và user
const API_AUTH = '/api/auth';
const API_USER = '/api/users';

function getToken() {
  return localStorage.getItem('accessToken');
}

function setAuth(data) {
  // Backend trả về { token, type, user: { username, displayName, ... } }
  const token = data?.token || data?.accessToken;
  const user = data?.user;

  if (token) localStorage.setItem('accessToken', token);
  if (user?.username) localStorage.setItem('username', user.username);
  if (user?.displayName || user?.username) {
    localStorage.setItem('displayName', user.displayName || user.username);
  }
}

function clearAuth() {
  localStorage.removeItem('accessToken');
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
    headers: { 'Authorization': `Bearer ${token}` }
  });

  if (response.status === 401) {
    logout();
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
function logout() {
  clearAuth();
  window.location.href = '/login';
}

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
  showToastSuccess,
  showErrorAlert
};