# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = "<div class='zoom-out-button'>Zoom out</div>"

addZoomOutButton = (chartId, series, originalBounds, chartOptions) ->
  $(zoomOutButtonTemplate).appendTo($("#" + chartId)).click (event) ->
    event.preventDefault()
    zoomedOptions = merge chartOptions,
      xaxis:
        min: originalBounds.xFrom
        max: originalBounds.xTo
      yaxis:
        min: originalBounds.yFrom
        max: originalBounds.yTo
    $.plot "#" + chartId, series, zoomedOptions

getPlotBounds = (plot) ->
  xFrom: plot.getAxes().xaxis.from
  xTo: plot.getAxes().xaxis.to
  yFrom: plot.getAxes().yaxis.from
  yTo: plot.getAxes().yaxis.to

clampRanges = (ranges) -> 
  minutes = 60 * 1000
  if ranges.xaxis.to - ranges.xaxis.from < 10 * minutes
    ranges.xaxis.to = ranges.xaxis.from + 10 * minutes
  if ranges.yaxis.to - ranges.yaxis.from < 10
    ranges.yaxis.to = ranges.yaxis.from + 10

onChartSelected = (chartId, series, originalBounds, chartOptions) -> (event, ranges) ->
  clampRanges ranges
  zoomedOptions = merge chartOptions,
    xaxis:
      min: ranges.xaxis.from
      max: ranges.xaxis.to
    yaxis:
      min: ranges.yaxis.from
      max: ranges.yaxis.to
  $.plot "#" + chartId, series, zoomedOptions
  addZoomOutButton chartId, series, originalBounds, chartOptions

window.addZoomSupport = (plot, chartId, series, chartOptions) ->
  originalBounds = getPlotBounds plot 
  $("#" + chartId).bind "plotselected", onChartSelected(chartId, series, originalBounds, chartOptions)

window.formatDate = (date) ->
  time = new Date(date).toLocaleTimeString()
  date = new Date(date).toLocaleDateString()
  "#{time} #{date}" 
