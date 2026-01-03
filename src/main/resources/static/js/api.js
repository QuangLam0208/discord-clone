const Api = (function () {
    function getToken() {
        return localStorage.getItem('accessToken');
    }
    async function request(endpoint, options = {}) {
        const headers = { ...options.headers };
        const token = getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;
        const config = { ...options, headers };
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
        } catch (error) { console.error('Network/API error: ', error); throw error; }
    }
    async function getJSON(endpoint) { const r = await request(endpoint, { method:'GET' }); if (!r) return null; if (!r.ok) return null; return r.json(); }
    async function postJSON(endpoint, body) {
        const r = await request(endpoint, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });
        if (!r) return null; const data = await r.json().catch(()=>({})); if (!r.ok) throw new Error(data.message||data.error||'Lỗi khi gọi API POST'); return data;
    }
    async function postMultipart(endpoint, formData) {
        const r = await request(endpoint, { method:'POST', body:formData });
        if (!r) return null; const data = await r.json().catch(()=>({})); if (!r.ok) throw new Error(data.message||'Lỗi upload file'); return data;
    }
    async function deleteObj(endpoint) {
        const r = await request(endpoint, { method:'DELETE' }); if (!r) return false;
        if (!r.ok) { const data = await r.json().catch(()=>({})); console.error('Delete failed:', data); return false; } return true;
    }
    async function putJSON(endpoint, body) {
        const r = await request(endpoint, { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });
        if (!r) return null; const data = await r.json().catch(()=>({})); if (!r.ok) throw new Error(data.message||'Lỗi khi gọi API PUT'); return data;
    }
    async function patchJSON(endpoint, body) {
        const r = await request(endpoint, { method:'PATCH', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body) });
        if (!r) return null; const data = await r.json().catch(()=>({})); if (!r.ok) throw new Error(data.message||'Lỗi khi gọi API PATCH'); return data;
    }
    return { get:getJSON, post:postJSON, put:putJSON, patch:patchJSON, upload:postMultipart, delete:deleteObj, fetch:request, getToken };
})();
window.Api = Api;