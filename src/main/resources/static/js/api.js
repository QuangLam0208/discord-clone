// Simple API wrapper: tự động kèm JWT cookie (credentials: 'include')
// và header Authorization: Bearer {accessToken} nếu có.
// Xử lý 401: chuyển về /login. Trả dữ liệu JSON (nếu có) cho các phương thức tiện ích.

const Api = (function () {
    function getToken() {
        return localStorage.getItem('accessToken');
    }

    function buildHeaders(customHeaders, hasBody) {
        const headers = { ...(customHeaders || {}) };
        const token = getToken();
        if (token && !headers['Authorization']) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        if (hasBody && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }
        return headers;
    }

    async function safeJson(response) {
        try {
            return await response.json();
        } catch {
            return null;
        }
    }

    async function request(endpoint, options = {}) {
        const hasBody = options.body !== undefined && options.body !== null;
        const headers = buildHeaders(options.headers, hasBody);

        const config = {
            method: options.method || 'GET',
            headers,
            body: options.body,
            credentials: 'include', // QUAN TRỌNG: gửi cookie jwt httpOnly
        };

        try {
            const response = await fetch(endpoint, config);

            // Hết phiên hoặc token không hợp lệ
            if (response.status === 401) {
                console.warn('Hết phiên đăng nhập hoặc token không hợp lệ');
                localStorage.removeItem('accessToken');
                localStorage.removeItem('user');
                window.location.href = '/login';
                return null;
            }

            return response;
        } catch (error) {
            console.error('Network/API error: ', error);
            throw error;
        }
    }

    async function getJSON(endpoint) {
        const r = await request(endpoint, { method: 'GET' });
        if (!r) return null;
        if (!r.ok) return null;
        return await safeJson(r);
    }

    async function postJSON(endpoint, body) {
        const r = await request(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
        if (!r) return null;
        const data = await safeJson(r) ?? {};
        if (!r.ok) throw new Error(data.message || data.error || 'Lỗi khi gọi API POST');
        return data;
    }

    async function postMultipart(endpoint, formData) {
        const r = await request(endpoint, { method: 'POST', body: formData });
        if (!r) return null;
        const data = await safeJson(r) ?? {};
        if (!r.ok) throw new Error(data.message || 'Lỗi upload file');
        return data;
    }

    async function deleteObj(endpoint) {
        const r = await request(endpoint, { method: 'DELETE' });
        if (!r) return false;
        if (!r.ok) {
            const data = await safeJson(r) ?? {};
            console.error('Delete failed:', data);
            return false;
        }
        // 200/204 đều coi là thành công
        return true;
    }

    async function putJSON(endpoint, body) {
        const r = await request(endpoint, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
        if (!r) return null;
        const data = await safeJson(r) ?? {};
        if (!r.ok) throw new Error(data.message || 'Lỗi khi gọi API PUT');
        return data;
    }

    async function patchJSON(endpoint, body) {
        const r = await request(endpoint, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
        if (!r) return null;
        const data = await safeJson(r) ?? {};
        if (!r.ok) throw new Error(data.message || 'Lỗi khi gọi API PATCH');
        return data;
    }

    return {
        get: getJSON,
        post: postJSON,
        put: putJSON,
        patch: patchJSON,
        upload: postMultipart,
        delete: deleteObj,
        fetch: request,
        getToken,
    };
})();
window.Api = Api;