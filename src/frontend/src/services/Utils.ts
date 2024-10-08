import {Toast} from "bootstrap";

class Utils {
    static toHumanReadable(timestamp: number): string {
        const localDate = new Date(timestamp);
        return localDate.toLocaleString();
    }

    static createRequestInit(body: any, method: string): RequestInit {
        return {
            method: method.toLocaleUpperCase(),
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        };
    }

    static  showToast(title: string, message: string, color: string = 'light', icon: string = 'bi-info-circle', delay: number = 2500) {
        let toastContainer = document.getElementById('toast-container');

        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '1050';
            document.body.appendChild(toastContainer);
        }

        const toastElement = document.createElement('div');
        toastElement.className = `toast text-bg-${color} border-0 mb-2`;
        toastElement.setAttribute('role', 'alert');
        toastElement.setAttribute('aria-live', 'assertive');
        toastElement.setAttribute('aria-atomic', 'true');

        toastElement.innerHTML = `
        <div class="toast-header">
          <i class="${icon} me-2"></i>
          <strong class="me-auto">${title}</strong>
          <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
          ${message}
        </div>
      `;

        toastContainer.appendChild(toastElement);

        const toast = new Toast(toastElement, { delay: delay });
        toast.show();
        toastElement.addEventListener('hidden.bs.toast', () => {
            if (toastElement.parentNode) {
                toastElement.parentNode.removeChild(toastElement);
            }
        });
    }
}

export { Utils };
