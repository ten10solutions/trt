# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = "<div class='zoom-out-button btn btn-default'>Zoom out</div>"

addZoomOutButton = (config, originalBounds) ->
  $(zoomOutButtonTemplate).appendTo($("#" + config.chartId)).click (event) ->
    event.preventDefault()
    zoomedOptions = merge config.chartOptions,
      xaxis:
        min: originalBounds.xFrom
        max: originalBounds.xTo
      yaxis:
        min: originalBounds.yFrom
        max: originalBounds.yTo
    $.plot "#" + config.chartId, config.series, zoomedOptions

getPlotBounds = (plot) ->
  xFrom: plot.getAxes().xaxis.from
  xTo: plot.getAxes().xaxis.to
  yFrom: plot.getAxes().yaxis.from
  yTo: plot.getAxes().yaxis.to

clampRanges = (ranges, minX, minY) -> 
  if ranges.xaxis.to - ranges.xaxis.from < minX
    ranges.xaxis.to = ranges.xaxis.from + minX
  if ranges.yaxis.to - ranges.yaxis.from < minY
    ranges.yaxis.to = ranges.yaxis.from + minY

onChartSelected = (config, originalBounds) -> (event, ranges) ->
  clampRanges ranges, config.minX, config.minY
  zoomedOptions = merge config.chartOptions,
    xaxis:
      min: ranges.xaxis.from
      max: ranges.xaxis.to
    yaxis:
      min: ranges.yaxis.from
      max: ranges.yaxis.to
  $.plot "#" + config.chartId, config.series, zoomedOptions
  addZoomOutButton config, originalBounds

window.addZoomSupport = (config) ->
  originalBounds = getPlotBounds config.plot 
  $("#" + config.chartId).bind "plotselected", onChartSelected(config, originalBounds)

window.formatDate = (date) ->
  time = new Date(date).toLocaleTimeString()
  date = new Date(date).toLocaleDateString()
  "#{time} #{date}" 

window.formatDateWithoutTime = (date) ->
  new Date(date).toLocaleDateString()
