window.chartData = {}

window.loadHistoryChart = (dataUrl, chartIndex) ->
  fetchCounts dataUrl, (counts) ->
    window.chartData[chartIndex] = counts
    renderChart(chartIndex)

renderChart = (chartIndex) ->
  counts = window.chartData[chartIndex]
  if counts.length > 0
    chartId = "history-chart-#{chartIndex}"
    includeHealthy  = $("#chart-checkbox-healthy-#{chartIndex}").is(":checked")
    includeWarnings = $("#chart-checkbox-warnings-#{chartIndex}").is(":checked")
    includeBroken   = $("#chart-checkbox-broken-#{chartIndex}").is(":checked")
    healthy = if includeHealthy  then ([new Date(c.when), c.counts.healthy] for c in counts) else []
    warning = if includeWarnings then ([new Date(c.when), c.counts.warning] for c in counts) else []
    broken  = if includeBroken   then ([new Date(c.when), c.counts.broken ] for c in counts) else []
    createHistoryChart(chartId, healthy, warning, broken)

fetchCounts = (dataUrl, callback) ->
  $.get(dataUrl).complete (d) -> callback(d.responseJSON)

$(document).ready ->
  $(".chart-checkbox").change () ->
    chartIndex = parseInt($(this).data("chart-index"))
    renderChart(chartIndex)        
