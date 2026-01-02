// UPLOAD MODULE

// Global variables for Upload module
// pendingAttachments is now in window.state (home.js)
let uploadBtn, fileInput, chatInput, previewContainer;

document.addEventListener('DOMContentLoaded', () => {
    chatInput = document.getElementById('chat-input');
    uploadBtn = document.querySelector('.upload-icon-btn');
    fileInput = document.getElementById('hidden-file-upload');
    const inputArea = document.querySelector('.input-area');

    // Create Preview Container (Dynamic)
    previewContainer = document.createElement('div');
    previewContainer.className = 'attachment-preview-area';
    previewContainer.style.display = 'none';
    previewContainer.style.padding = '10px 16px 0';
    previewContainer.style.gap = '10px';
    previewContainer.style.display = 'flex';
    previewContainer.style.flexWrap = 'wrap';

    // Insert preview before input wrapper
    if (inputArea) {
        inputArea.insertBefore(previewContainer, inputArea.firstChild);
    }

    // Trigger file input
    if (uploadBtn && fileInput) {
        uploadBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            fileInput.click();
        });

        fileInput.addEventListener('change', async () => {
            const file = fileInput.files[0];
            if (!file) return;

            // Validate size (max 10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert("File quá lớn (Max 10MB).");
                return;
            }

            const formData = new FormData();
            formData.append('file', file);

            try {
                // Upload immediately but don't send message yet
                const response = await Api.upload('/api/upload', formData);
                if (response && response.url) {
                    console.log("Uploaded pending:", response.url);
                    state.pendingAttachments.push(response.url);
                    renderPreviews();
                    chatInput.focus();
                }
            } catch (e) {
                console.error("Upload error:", e);
                alert("Lỗi upload file: " + e.message);
            } finally {
                fileInput.value = '';
            }
        });
    }

    if (chatInput) {
        chatInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                const content = chatInput.value.trim();
                // Send if content exists OR there are attachments
                if ((content || state.pendingAttachments.length > 0) && state.currentChannelId) {
                    if (window.sendMessage) {
                        window.sendMessage(content, [...state.pendingAttachments]);
                    } else {
                        console.error("sendMessage function not found!");
                        alert("Lỗi: Không thể gửi tin nhắn (hàm sendMessage không tìm thấy).");
                    }
                    chatInput.value = '';

                    // Clear pending
                    state.pendingAttachments = [];
                    renderPreviews();
                }
            }
        });
    }
});

function renderPreviews() {
    previewContainer.innerHTML = '';
    if (state.pendingAttachments.length === 0) {
        previewContainer.style.display = 'none';
        return;
    }
    previewContainer.style.display = 'flex';

    state.pendingAttachments.forEach((url, index) => {
        const wrapper = document.createElement('div');
        wrapper.style.position = 'relative';
        wrapper.style.width = '100px';
        wrapper.style.height = '100px';
        wrapper.style.borderRadius = '4px';
        wrapper.style.overflow = 'hidden';
        wrapper.style.border = '1px solid #202225';

        const img = document.createElement('img');
        img.src = url;
        img.style.width = '100%';
        img.style.height = '100%';
        img.style.objectFit = 'cover';

        const removeBtn = document.createElement('div');
        removeBtn.innerHTML = '<i class="fa-solid fa-xmark"></i>';
        removeBtn.style.position = 'absolute';
        removeBtn.style.top = '2px';
        removeBtn.style.right = '2px';
        removeBtn.style.backgroundColor = '#ed4245';
        removeBtn.style.color = 'white';
        removeBtn.style.borderRadius = '50%';
        removeBtn.style.width = '18px';
        removeBtn.style.height = '18px';
        removeBtn.style.display = 'flex';
        removeBtn.style.justifyContent = 'center';
        removeBtn.style.alignItems = 'center';
        removeBtn.style.fontSize = '10px';
        removeBtn.style.cursor = 'pointer';

        removeBtn.onclick = () => {
            state.pendingAttachments.splice(index, 1);
            renderPreviews();
        };

        wrapper.appendChild(img);
        wrapper.appendChild(removeBtn);
        previewContainer.appendChild(wrapper);
    });
}
