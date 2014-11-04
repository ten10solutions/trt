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
    maybeNull = (c) -> if c == 0 then null else c
    i = 0
    healthy = []
    warnings = []
    broken = []
    while i < counts.length
      c = counts[i]
      d = new Date(c.when)
      base = 0
      if includeHealthy
        n = c.counts.healthy
        p = if n == 0 then [d, null] else [d, base + n, base]
        healthy.push p
        base += n
      if includeWarnings
        n = c.counts.warning
        p = if n == 0 then [d, null] else [d, base + n, base]
        warnings.push p
        base += n
      if includeBroken
        n = c.counts.broken
        p = if n == 0 then [d, null] else [d, base + n, base]
        broken.push p
        base += n
      i++
    createHistoryChart(chartId, healthy, warnings, broken, counts)

fetchCounts = (dataUrl, callback) ->
  $.get(dataUrl).complete (d) -> callback(d.responseJSON)

$(document).ready ->
  $(".chart-checkbox").change () ->
    chartIndex = parseInt($(this).data("chart-index"))
    renderChart(chartIndex)        
