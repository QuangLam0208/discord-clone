const API_USER = '/api/users';

document.addEventListener('DOMContentLoaded', () => {
    // Load current user info
    fetch('/api/users/me')
        .then(res => {
            if (!res.ok) throw new Error('Unauthorized');
            return res.json();
        })
        .then(user => {
            const display = document.getElementById('currentEmailDisplay');
            if (display) display.textContent = user.email;
        })
        .catch(() => {
            window.location.href = '/login?redirect=/change-email';
        });

    const requestForm = document.getElementById('requestForm');
    const verifyOldForm = document.getElementById('verifyOldForm');
    const verifyNewForm = document.getElementById('verifyNewForm');

    // Step 1: Request Change
    if (requestForm) {
        requestForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const newEmail = document.getElementById('newEmail').value;

            setLoading(true, requestForm);
            try {
                const res = await fetch(`${API_USER}/change-email/request?newEmail=${encodeURIComponent(newEmail)}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });

                const data = await res.json();

                if (res.ok) {
                    showAlert('success', data.message);
                    document.getElementById('newEmailDisplay').textContent = newEmail;
                    showStep(2);
                } else {
                    showAlert('error', data.message || 'Có lỗi xảy ra');
                }
            } catch (err) {
                showAlert('error', 'Lỗi kết nối server');
            } finally {
                setLoading(false, requestForm);
            }
        });
    }

    // Step 2: Verify Old Email
    if (verifyOldForm) {
        verifyOldForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const otpCode = document.getElementById('otpOld').value;

            setLoading(true, verifyOldForm);
            try {
                const res = await fetch(`${API_USER}/change-email/verify-old?otpCode=${encodeURIComponent(otpCode)}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });

                const data = await res.json();

                if (res.ok) {
                    showAlert('success', data.message);
                    showStep(3);
                } else {
                    showAlert('error', data.message || 'Mã OTP không đúng');
                }
            } catch (err) {
                showAlert('error', 'Lỗi kết nối server');
            } finally {
                setLoading(false, verifyOldForm);
            }
        });
    }

    // Step 3: Verify New Email
    if (verifyNewForm) {
        verifyNewForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const otpCode = document.getElementById('otpNew').value;

            setLoading(true, verifyNewForm);
            try {
                const res = await fetch(`${API_USER}/change-email/verify-new?otpCode=${encodeURIComponent(otpCode)}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });

                const data = await res.json();

                if (res.ok) {
                    showAlert('success', data.message);
                    document.getElementById('finalEmailDisplay').textContent = data.newEmail;
                    showSuccess();
                } else {
                    showAlert('error', data.message || 'Mã OTP không đúng');
                }
            } catch (err) {
                showAlert('error', 'Lỗi kết nối server');
            } finally {
                setLoading(false, verifyNewForm);
            }
        });
    }
});

function showStep(stepNumber) {
    document.querySelectorAll('.form-step').forEach(el => el.classList.remove('active'));
    document.getElementById(`step${stepNumber}`).classList.add('active');

    // Update indicators
    document.querySelectorAll('.step').forEach((el, index) => {
        if (index + 1 < stepNumber) {
            el.classList.add('completed');
            el.classList.remove('active');
            el.innerHTML = '<i class="fas fa-check"></i>';
        } else if (index + 1 === stepNumber) {
            el.classList.add('active');
            el.classList.remove('completed');
            el.textContent = stepNumber;
        } else {
            el.classList.remove('active', 'completed');
            el.textContent = index + 1;
        }
    });

    // Clear alerts when switching steps
    const alertEl = document.getElementById('alertMessage');
    alertEl.style.display = 'none';
}

function showSuccess() {
    document.querySelectorAll('.form-step').forEach(el => el.classList.remove('active'));
    document.getElementById('stepSuccess').classList.add('active');
    document.querySelector('.steps-container').style.display = 'none';
}

function backToStep(stepNumber) {
    showStep(stepNumber);
}

function showAlert(type, message) {
    const alertEl = document.getElementById('alertMessage');
    alertEl.className = `alert alert-${type === 'success' ? 'success' : 'danger'}`;
    alertEl.textContent = message;
    alertEl.style.display = 'block';
}

function setLoading(isLoading, form) {
    const btn = form.querySelector('button[type="submit"]');
    if (btn) {
        btn.disabled = isLoading;
        const originalText = btn.dataset.originalText || btn.innerHTML;

        if (isLoading) {
            btn.dataset.originalText = originalText;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';
        } else {
            btn.innerHTML = originalText;
        }
    }
}
