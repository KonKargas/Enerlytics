(() => {
    'use strict';
    const KEY = 'enerlytics.analytics.preferences.v1';
    const defaults = { type: 'SUM', grouping: 'MONTH' };
    const types = new Set(['SUM', 'AVG']);
    const groupings = new Set(['MONTH', 'SEASON', 'DAY_OF_WEEK', 'DAY_PERIOD']);
    const form = document.getElementById('settingsForm');
    if (!form) return;
    const type = document.getElementById('defaultAggregation');
    const grouping = document.getElementById('defaultGrouping');
    const feedback = document.getElementById('settingsFeedback');

    function stored() {
        try {
            const value = JSON.parse(localStorage.getItem(KEY) || '{}');
            return {
                type: types.has(value.type) ? value.type : defaults.type,
                grouping: groupings.has(value.grouping) ? value.grouping : defaults.grouping
            };
        } catch (_) { return { ...defaults }; }
    }

    function show(preferences) {
        type.value = preferences.type;
        grouping.value = preferences.grouping;
    }

    show(stored());
    form.addEventListener('submit', event => {
        event.preventDefault();
        try {
            localStorage.setItem(KEY, JSON.stringify({ type: type.value, grouping: grouping.value }));
            feedback.textContent = 'Preferences saved for this browser.';
        } catch (_) { feedback.textContent = 'Unable to save browser preferences. Please check browser storage settings.'; }
    });
    document.getElementById('resetSettings').addEventListener('click', () => {
        try { localStorage.removeItem(KEY); show(defaults); feedback.textContent = 'Default preferences restored.'; }
        catch (_) { feedback.textContent = 'Unable to reset browser preferences.'; }
    });
})();
