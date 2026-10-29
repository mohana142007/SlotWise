// SlotWise Frontend Interactions (Vanilla JavaScript)

document.addEventListener('DOMContentLoaded', () => {
    // Theme preference is shared across pages and defaults to the system setting.
    const themeToggle = document.getElementById('theme-toggle');
    if (themeToggle) {
        const updateThemeToggle = () => {
            const isDark = document.documentElement.dataset.theme === 'dark';
            const label = isDark ? 'Light mode' : 'Dark mode';
            themeToggle.setAttribute('aria-pressed', String(isDark));
            themeToggle.setAttribute('aria-label', `Switch to ${label.toLowerCase()}`);
            themeToggle.querySelector('.theme-toggle-label').textContent = label;
        };

        updateThemeToggle();
        themeToggle.addEventListener('click', () => {
            const nextTheme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = nextTheme;
            localStorage.setItem('slotwise-theme', nextTheme);
            updateThemeToggle();
        });
    }

    // 1. Restrict date pickers to today or future dates
    const dateInputs = document.querySelectorAll('input[type="date"].future-date');
    const today = new Date().toISOString().split('T')[0];
    dateInputs.forEach(input => {
        if (!input.getAttribute('min')) {
            input.setAttribute('min', today);
        }
    });

    // 2. Cancellation confirmation prompts
    const cancelForms = document.querySelectorAll('form.confirm-cancel');
    cancelForms.forEach(form => {
        form.addEventListener('submit', (e) => {
            const confirmed = window.confirm(
                'Are you sure you want to cancel this booking? This will immediately free the time slot for other students and faculty.'
            );
            if (!confirmed) {
                e.preventDefault();
            }
        });
    });

    // 3. Status toggle confirmation for Admins
    const toggleForms = document.querySelectorAll('form.confirm-toggle');
    toggleForms.forEach(form => {
        form.addEventListener('submit', (e) => {
            const confirmed = window.confirm(
                'Are you sure you want to change the active status of this resource?'
            );
            if (!confirmed) {
                e.preventDefault();
            }
        });
    });

    // 4. Quick preset times on booking form
    const startTimeInput = document.getElementById('startTime');
    const endTimeInput = document.getElementById('endTime');
    if (startTimeInput && endTimeInput) {
        startTimeInput.addEventListener('change', () => {
            if (startTimeInput.value && !endTimeInput.value) {
                // Default end time to 1 hour after start time
                const [h, m] = startTimeInput.value.split(':').map(Number);
                const endH = String((h + 1) % 24).padStart(2, '0');
                endTimeInput.value = `${endH}:${String(m).padStart(2, '0')}`;
            }
        });
    }
});
