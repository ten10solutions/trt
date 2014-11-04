chartOptions = 
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
  yaxis:
    minTickSize: 1
    tickDecimals: 0
    min: 0
  series:
    #stack: true
    bars:
      show: true
      #align: "center"
      barWidth: 24*60*60*1000
      fill: true
      lineWidth: 1
      fillColor:
        colors: [
          { opacity: 0.8 }
          { opacity: 0.1 }
        ]
    points:
      show: false
  selection:
    mode: "xy"
  legend:
    show: false

getTooltipText = (dayCounts) ->
  tooltipText = "<table style='border: 0;'><tr><td style='text-align: center;' colspan='2'>#{new Date(dayCounts.when).toLocaleDateString()}</td></tr>"

  total = 0

  n = dayCounts.counts.healthy
  if n > 0
    tooltipText +="<tr><td>Healthy</td><td><span class='badge badge-success'>#{n}</span></td></tr>"
  total += n

  n = dayCounts.counts.warning
  if n > 0
    tooltipText +="<tr><td>Warning</td><td><span class='badge badge-warning'>#{n}</span></td></tr>"
  total += n

  n = dayCounts.counts.broken
  if n > 0
    tooltipText +="<tr><td>Broken</td><td><span class='badge badge-error'>#{n}</span></td></tr>"
  total += n

  tooltipText +="<tr><td>Total</td><td><span class='badge badge-inverse'>#{total}</span></td></tr>"

  tooltipText += "</table>"

onChartHover = (series, counts) -> (event, pos, item) ->
  if item
    tooltipText = getTooltipText counts[item.dataIndex]
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body" unless $('#chart-tooltip').length

window.createHistoryChart = (chartId, healthy, warnings, broken, counts) ->  
  series = []
  if healthy.length > 0  # omit empty series otherwise JFlot displays no data
    series.push
      label: "Healthy"
      data: healthy
      color: "#609000"
  if warnings.length > 0
    series.push
      label: "Warnings"
      data: warnings
      color: "#FFBF00"
  if broken.length > 0
    series.push
      label: "Broken"
      data: broken
      color: "#b94a48"

  plot = $.plot $("#" + chartId), series, chartOptions

  addZoomSupport
    plot: plot
    chartId: chartId
    series: series
    chartOptions: chartOptions
    minX: 10 * 60 * 1000
    minY: 10
  $("#" + chartId).unbind "plothover"
  $("#" + chartId).bind "plothover", onChartHover(series, counts)

  initialiseTooltip()
