window.loadHistoryChart = (dataUrl, chartId) ->
  fetchCounts dataUrl, (counts) ->
    if counts.length > 0
      healthy  = ([new Date(c.when), c.counts.healthy] for c in counts)
      warning = ([new Date(c.when), c.counts.warning] for c in counts)
      broken  = ([new Date(c.when), c.counts.broken ] for c in counts)
      createHistoryChart(chartId, healthy, warning, broken)

fetchCounts = (dataUrl, callback) ->
  $.get(dataUrl).complete (d) -> callback(d.responseJSON)
