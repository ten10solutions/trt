# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

chartOptions = 
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
    labelWidth: 70
  yaxis:
    minTickSize: 1
    tickDecimals: 0
    min: 0
  series:
    bars:
      show: true
      barWidth: 24 * 60 * 60 * 1000
      align: "right"
      fill: true
      lineWidth: 1
      fillColor:
        colors: [
          { opacity: 0.9 }
          { opacity: 0.3 } 
          { opacity: 0.9 }
        ]
    points:
      show: false
  selection:
    mode: "xy"
    color: "#bbe"
  legend:
    show: false

tooltipTemplate = Handlebars.compile """
<table class='tooltip-table'>
  <tr>
    <td class='tooltip-header' colspan='2'>{{when}}</td>
  </tr>
  {{#if healthy}}
    <tr>
      <td>Healthy</td>
      <td><span class='badge badge-success'>{{healthy}}</span></td>
    </tr>
  {{/if}}
  {{#if warning}}
    <tr>
      <td>Warning</td>
      <td><span class='badge badge-warning'>{{warning}}</span></td>
    </tr>
  {{/if}}
  {{#if broken}}
    <tr>
      <td>Broken</td>
      <td><span class='badge badge-error'>{{broken}}</span></td>
    </tr>
  {{/if}}
  <tr>
    <td>Total</td>
    <td><span class='badge badge-inverse'>{{total}}</span></td>
  </tr>
</table>
"""

getLocale = ->
  (if navigator.languages then navigator.languages[0] else (navigator.language or navigator.userLanguage))

prettyPrint = (date) ->
  moment.locale(getLocale())
  moment(date).format('LL')
      
getTooltipText = (dayCounts) ->
  cs = dayCounts.counts
  displayDate = prettyPrint(moment(dayCounts.when).subtract(1, 'days'))
  tooltipTemplate
    when: displayDate
    healthy: cs.healthy
    warning: cs.warning
    broken: cs.broken
    total: cs.healthy + cs.warning + cs.broken

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

createHistoryChart = (chartId, seriesData, counts, timelineBounds) ->
  series = []
  addSeries = (label, data, color) ->
    series.push
      label: label
      data: data
      color: color
  addSeries "Healthy",  seriesData.healthy,  "#609000" if seriesData.healthy
  addSeries "Warnings", seriesData.warnings, "#ffbf00" if seriesData.warnings
  addSeries "Broken",   seriesData.broken,   "#b94a48" if seriesData.broken

  chartOpts = chartOptions
  if timelineBounds?
    chartOpts = merge chartOpts,
      xaxis:
        min: timelineBounds.start
        max: timelineBounds.finish

  plot = $.plot $("#" + chartId), series, chartOpts

  addZoomSupport
    plot: plot
    chartId: chartId
    series: series
    chartOptions: chartOpts
    minX: 10 * 60 * 1000
    minY: 10
  $("#" + chartId).unbind "plothover"
  $("#" + chartId).bind "plothover", onChartHover(series, counts)

  initialiseTooltip()

getSeriesData = (counts, includeHealthy, includeWarnings, includeBroken) ->
  healthy = []
  warnings = []
  broken = []
  for c in counts
    date = new Date(c.when)
    base = 0
    makePoint = (n) -> if n == 0 then [date, null] else [date, base + n, base]
    if includeHealthy
      n = c.counts.healthy
      healthy.push makePoint(n)
      base += n
    if includeWarnings
      n = c.counts.warning
      warnings.push makePoint(n)
      base += n
    if includeBroken
      n = c.counts.broken
      broken.push makePoint(n)
      base += n
  {
    healthy: healthy
    warnings: warnings
    broken: broken
  }

window.createHistoryChart = (chartId, counts, timelineBounds, includeHealthy, includeWarnings, includeBroken) ->
  seriesData = getSeriesData counts, includeHealthy, includeWarnings, includeBroken
  createHistoryChart chartId, seriesData, counts, timelineBounds
