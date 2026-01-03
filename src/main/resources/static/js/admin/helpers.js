// ====== Helpers ======
(function () {
    function closeModal(id){ const m=document.getElementById(id); if(m) m.classList.remove('active'); }
    function openModal(id){ const m=document.getElementById(id); if(m) m.classList.add('active'); }
    function toastOk(t='Thành công'){ if(window.Swal){ return Swal.fire({icon:'success',title:t,timer:1200,showConfirmButton:false,background:'#313338',color:'#fff'});} }
    function toastErr(t='Có lỗi xảy ra'){ if(window.Swal){ return Swal.fire({icon:'error',title:'Thất bại',text:t,background:'#313338',color:'#fff'});} }
    function escapeHtml(s){ return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
    function escapeJsAttr(s){ return String(s||'').replace(/\\/g, '\\\\').replace(/'/g, "\\'"); }

    // Đóng modal khi click overlay / ESC + đóng status-menu
    window.addEventListener('click', e=>{
        if(e.target.classList && e.target.classList.contains('modal-overlay')) e.target.classList.remove('active');
        if (!e.target.closest('.status-menu') && !e.target.closest('.btn-mini.warn')) {
            document.querySelectorAll('.status-menu.open').forEach(el => {
                el.classList.remove('open', 'drop-up');
                el.style.position = '';
                el.style.top = '';
                el.style.left = '';
                el.style.right = '';
                el.style.minWidth = '';
            });
        }
    });
    window.addEventListener('keydown', e=>{
        if(e.key==='Escape'){
            document.querySelectorAll('.modal-overlay.active').forEach(el=>el.classList.remove('active'));
            document.querySelectorAll('.status-menu.open').forEach(el => {
                el.classList.remove('open', 'drop-up');
                el.style.position = '';
                el.style.top = '';
                el.style.left = '';
                el.style.right = '';
                el.style.minWidth = '';
            });
        }
    });

    // Expose
    window.closeModal = closeModal;
    window.openModal = openModal;
    window.toastOk = toastOk;
    window.toastErr = toastErr;
    window.escapeHtml = escapeHtml;
    window.escapeJsAttr = escapeJsAttr;
})();