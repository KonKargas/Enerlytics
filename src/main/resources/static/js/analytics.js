(() => {
    'use strict';
    const dataset = document.getElementById('datasetSelect');
    const type = document.getElementById('aggregationSelect');
    const grouping = document.getElementById('groupingSelect');
    const dateFrom = document.getElementById('dateFrom');
    const dateTo = document.getElementById('dateTo');
    const resetDates = document.getElementById('resetDates');
    const status = document.getElementById('analyticsStatus');
    const body = document.getElementById('analyticsTableBody');
    if (!dataset || !type || !grouping) return;

    const colors = ['#c6a036', '#90a4c4', '#5b78a6'];
    const number = new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 });
    const integer = new Intl.NumberFormat('en-US');
    let bar = null;
    let doughnut = null;
    let requestNumber = 0;

    function setStatus(message, error = false) {
        status.textContent = message;
        status.classList.toggle('error', error);
    }

    function setText(id, value) {
        document.getElementById(id).textContent = value;
    }

    function drawCharts(rows, isSum) {
        if (typeof Chart === 'undefined') {
            throw new Error('Chart.js could not be loaded. Check your internet connection.');
        }
        const scale = isSum ? 1000 : 1;
        const names = ['Kitchen', 'Laundry', 'Heating'];
        const fields = ['kitchen', 'laundry', 'heating'];
        const periodLabels = rows.map(row => row.period);
        const datasets = fields.map((field, index) => ({
            label: names[index], data: rows.map(row => row[field] / scale),
            backgroundColor: colors[index], borderRadius: 4, maxBarThickness: 38
        }));
        if (bar) bar.destroy();
        bar = new Chart(document.getElementById('analyticsBarChart'), {
            type: 'bar', data: { labels: periodLabels, datasets },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { labels: { color: '#cdd8e6' } }, tooltip: { callbacks: {
                    label: ctx => `${ctx.dataset.label}: ${number.format(ctx.parsed.y)} ${isSum ? 'kWh' : 'Wh / reading'}`
                } } },
                scales: {
                    x: { ticks: { color: '#9caec4' }, grid: { display: false } },
                    y: { beginAtZero: true, ticks: { color: '#9caec4' }, grid: { color: 'rgba(130,147,169,.12)' } }
                }
            }
        });
        const weighted = fields.map(field => rows.reduce((sum, row) =>
            sum + (isSum ? row[field] : row[field] * row.measurementCount), 0));
        if (doughnut) doughnut.destroy();
        doughnut = new Chart(document.getElementById('analyticsDoughnutChart'), {
            type: 'doughnut',
            data: { labels: names, datasets: [{ data: weighted, backgroundColor: colors, borderWidth: 0, hoverOffset: 5 }] },
            options: { responsive: true, maintainAspectRatio: false, cutout: '72%',
                plugins: { legend: { position: 'bottom', labels: { color: '#cdd8e6', padding: 18 } } } }
        });
    }

    function showResults(rows, isSum) {
        const unit = isSum ? 'kWh' : 'Wh / reading';
        const count = rows.reduce((sum, row) => sum + row.measurementCount, 0);
        const fields = ['kitchen', 'laundry', 'heating'];
        fields.forEach(field => {
            const total = rows.reduce((sum, row) => sum + (isSum ? row[field] : row[field] * row.measurementCount), 0);
            const value = isSum ? total / 1000 : (count ? total / count : 0);
            setText(`${field}Value`, number.format(value));
            setText(`${field}Unit`, isSum ? 'Total kWh' : 'Weighted mean Wh / reading');
            setText(`${field}Heading`, `${field.charAt(0).toUpperCase() + field.slice(1)} (${unit})`);
        });
        setText('countValue', integer.format(count));
        setText('chartUnit', unit);
        setText('resultsSubtitle', `Values grouped by ${grouping.options[grouping.selectedIndex].text.toLowerCase()}.`);
        body.replaceChildren();
        rows.forEach(row => {
            const tr = document.createElement('tr');
            const values = [row.period, integer.format(row.measurementCount),
                ...fields.map(field => number.format(row[field] / (isSum ? 1000 : 1)))];
            values.forEach(value => {
                const td = document.createElement('td');
                td.textContent = value;
                tr.appendChild(td);
            });
            body.appendChild(tr);
        });
    }

    function clearVisuals() {
        if (bar) { bar.destroy(); bar = null; }
        if (doughnut) { doughnut.destroy(); doughnut = null; }
        body.replaceChildren();
        ['kitchenValue', 'laundryValue', 'heatingValue', 'countValue']
            .forEach(id => setText(id, '—'));
    }

    async function load() {
        const id = dataset.value;
        if (!id) { setStatus('No dataset has been imported yet.'); return; }
        const token = ++requestNumber;
        if (dateFrom.value && dateTo.value && dateFrom.value > dateTo.value) {
            clearVisuals();
            setStatus('Date From cannot be after Date To.', true);
            return;
        }
        setStatus('Loading analytics...');
        const params = new URLSearchParams({ datasetId: id, type: type.value, grouping: grouping.value });
        if (dateFrom.value) params.set('dateFrom', dateFrom.value);
        if (dateTo.value) params.set('dateTo', dateTo.value);
        try {
            const response = await fetch(`${window.enerlyticsAnalytics.apiUrl}?${params}`);
            if (!response.ok) throw new Error(`Analytics request failed (HTTP ${response.status})`);
            const rows = await response.json();
            if (token !== requestNumber) return;
            if (!Array.isArray(rows)) throw new Error('Invalid analytics response');
            if (rows.length === 0) {
                clearVisuals();
                setStatus('No measurements for the selected date range.');
                return;
            }
            showResults(rows, type.value === 'SUM');
            drawCharts(rows, type.value === 'SUM');
            setStatus(`${integer.format(rows.length)} periods loaded successfully.`);
            const url = new URL(window.location.href);
            url.searchParams.set('datasetId', id);
            for (const [key, value] of [['dateFrom', dateFrom.value], ['dateTo', dateTo.value]]) {
                if (value) url.searchParams.set(key, value);
                else url.searchParams.delete(key);
            }
            window.history.replaceState(null, '', url);
        } catch (error) {
            if (token !== requestNumber) return;
            setStatus(error.message || 'Unable to load analytics.', true);
            console.error(error);
        }
    }
    // Apply browser defaults before the initial analytics request.
    try {
        const preferences = JSON.parse(localStorage.getItem('enerlytics.analytics.preferences.v1') || '{}');
        if (['SUM', 'AVG'].includes(preferences.type)) type.value = preferences.type;
        if (['MONTH', 'SEASON', 'DAY_OF_WEEK', 'DAY_PERIOD'].includes(preferences.grouping)) grouping.value = preferences.grouping;
    } catch (_) { /* Private browsing or malformed preferences: use defaults. */ }

    const query = new URLSearchParams(window.location.search);
    dateFrom.value = query.get('dateFrom') || '';
    dateTo.value = query.get('dateTo') || '';

    [dataset, type, grouping, dateFrom, dateTo]
        .forEach(control => control.addEventListener('change', load));
    resetDates.addEventListener('click', () => {
        dateFrom.value = '';
        dateTo.value = '';
        load();
    });
    load();
})();
