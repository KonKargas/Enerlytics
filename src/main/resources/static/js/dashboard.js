let consumptionChart = null;

document.addEventListener("DOMContentLoaded", () => {

    const datasetId = window.enerlyticsConfig?.datasetId;
    const typeSelect = document.getElementById("aggregationType");
    const groupingSelect = document.getElementById("timeGrouping");

    if (!datasetId || !typeSelect || !groupingSelect) {
        return;
    }

    const loadChart = async () => {

        const type = typeSelect.value;
        const grouping = groupingSelect.value;

        const url = `/api/analytics?datasetId=${datasetId}&type=${type}&grouping=${grouping}`;

        try {
            const response = await fetch(url);

            if (!response.ok) {
                throw new Error("Failed to load analytics data");
            }

            const data = await response.json();
            renderChart(data, type, grouping);

        } catch (error) {
            console.error("Analytics loading error:", error);
        }
    };

    typeSelect.addEventListener("change", loadChart);
    groupingSelect.addEventListener("change", loadChart);

    loadChart();
});

function renderChart(data, type, grouping) {

    const canvas = document.getElementById("consumptionChart");
    if (!canvas) {
        return;
    }

    const labels = data.map(item => item.period);
    const kitchen = data.map(item => item.kitchen);
    const laundry = data.map(item => item.laundry);
    const heating = data.map(item => item.heating);

    if (consumptionChart) {
        consumptionChart.destroy();
    }

    consumptionChart = new Chart(canvas, {
        type: "bar",
        data: {
            labels: labels,
            datasets: [
                {
                    label: "Kitchen",
                    data: kitchen,
                    backgroundColor: "rgba(198, 160, 54, 0.85)",
                    borderColor: "rgba(198, 160, 54, 1)",
                    borderWidth: 1
                },
                {
                    label: "Laundry",
                    data: laundry,
                    backgroundColor: "rgba(133, 149, 171, 0.75)",
                    borderColor: "rgba(133, 149, 171, 1)",
                    borderWidth: 1
                },
                {
                    label: "Heating",
                    data: heating,
                    backgroundColor: "rgba(68, 88, 115, 0.85)",
                    borderColor: "rgba(68, 88, 115, 1)",
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: "#eef5fc"
                    }
                },
                title: {
                    display: true,
                    text: `Consumption Analysis — ${type} / ${formatGrouping(grouping)}`,
                    color: "#eef5fc",
                    font: {
                        size: 14,
                        weight: "600"
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: "#b7c5d4"
                    },
                    grid: {
                        color: "rgba(37, 52, 70, 0.45)"
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: "#b7c5d4"
                    },
                    grid: {
                        color: "rgba(37, 52, 70, 0.45)"
                    }
                }
            }
        }
    });
}

function formatGrouping(grouping) {
    switch (grouping) {
        case "MONTH":
            return "Month";
        case "SEASON":
            return "Season";
        case "DAY_OF_WEEK":
            return "Day of Week";
        case "DAY_PERIOD":
            return "Day Period";
        default:
            return grouping;
    }
}