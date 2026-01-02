

const Api = (function () {

    function getToken() {
        return localStorage.getItem('accessToken');
    }

    // Hàm fetch wrapper chung (Thấy comment này của t thì rep t trên zalo hehe!)
    async function request(endpoint, options = {}) {
        // Đảm bảo endpoint bắt đầu đúng chuẩn
        const headers = { ...options.headers };
        const token = getToken();

        // Tự động gắn Bearer Token nếu có
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers: headers
        }

        try {
            const response = await fetch(endpoint, config);

            if (response.status === 401) {
                console.warn('Hết phiên đăng nhập hoặc token không hợp lệ');
                localStorage.removeItem('accessToken');
                localStorage.removeItem('user');
                window.location.href = '/login';
                return null;
            }

            return response;
        }
        catch (error) {
            console.error('Network/API error: ', error);
            throw error;
        }
    };

    // Get JSON
    async function getJSON(endpoint) {
        const response = await request(endpoint, { method: 'GET' });

        if (!response) return null;
        if (!response.ok) {
            console.error(`GET ${endpoint} failed:`, response.status);
            return null;
        }

        return await response.json();
    };

    // Post JSON
    async function postJSON(endpoint, body) {
        const response = await request(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        })

        if (!response) return null;
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || data.error || 'Lỗi khi gọi API POST');
        }
        return data;
    };

    // Post Multipart
    async function postMultipart(endpoint, formData) {
        // Lưu ý: Không set Content-Type header thủ công, để browser tự set boundary
        const response = await request(endpoint, {
            method: 'POST',
            body: formData
        });

        if (!response) return null;

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || 'Lỗi upload file');
        }
        return data;
    }

    // Delete (Cho chức năng xóa Server/Channel)
    async function deleteObj(endpoint) {
        const response = await request(endpoint, { method: 'DELETE' });
        if (!response) return false;

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            console.error('Delete failed:', data);
            return false;
        }
        return true;
    }

    // Put JSON
    async function putJSON(endpoint, body) {
        const response = await request(endpoint, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (!response) return null;
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || 'Lỗi khi gọi API PUT');
        }
        return data;
    }

    // Patch JSON
    async function patchJSON(endpoint, body) {
        const response = await request(endpoint, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!response) return null;
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || 'Lỗi khi gọi API PATCH');
        return data;
    }

    // Public các hàm ra ngoài
    return {
        get: getJSON,
        post: postJSON,
        put: putJSON,
        patch: patchJSON,
        upload: postMultipart,
        delete: deleteObj,
        fetch: request
    }
})();

// Gán vào window để dùng toàn cục
window.Api = Api;