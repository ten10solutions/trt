chartData = {}

window.loadHistoryChart = (dataUrl, chartIndex) ->
  fetchCounts dataUrl, (counts) ->
    chartData[chartIndex] = counts
    renderChart(chartIndex)

renderChart = (chartIndex) ->
  counts = chartData[chartIndex]
  if counts.length > 0
    chartId = "history-chart-#{chartIndex}"
    includeHealthy  = $("#chart-checkbox-healthy-#{chartIndex}").is(":checked")
    includeWarnings = $("#chart-checkbox-warnings-#{chartIndex}").is(":checked")
    includeBroken   = $("#chart-checkbox-broken-#{chartIndex}").is(":checked")
    createHistoryChart chartId, counts, includeHealthy, includeWarnings, includeBroken

fetchCounts = (dataUrl, callback) ->
  $.get(dataUrl).complete (d) -> callback(d.responseJSON)

$(document).ready ->
  $(".chart-checkbox").change ->
    chartIndex = parseInt($(this).data("chart-index"))
    renderChart(chartIndex)        
