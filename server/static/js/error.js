const container = document.querySelector('#divMessage');

const SANTIZE_SETTINGS = {
    allowedTags: ['h3', 'h4', 'div', 'span']
};
const sanitize = html => sanitizeHtml(html, SANTIZE_SETTINGS);

function show(html, type) {
    container.className = `show ${type}`;
    container.innerHTML = html;
    clearOnTimeout();
}

export function showError(error, message) {
    show(`
        <div class="title">${message}</div>
        <div class="message">${error.message}</div>
    `, 'error');
}

export function showInfo(message) {
    show(sanitize(message), 'info');
}

let timeout;
function clearOnTimeout() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => {
        container.className = '';
        container.innerHTML = '';
    }, 5000);
}
