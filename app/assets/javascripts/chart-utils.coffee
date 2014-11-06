# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = (left, top) -> """
  <button type='button' style='left: #{left}px; top: #{top}px;' class='zoom-out-button btn btn-default' title='Zoom out'><i class='fa fa-zoom fa-search-minus'></i></button>
"""

addZoomOutButton = (plotOffset, config, originalBounds) ->
  console.log(plotOffset)
  left = plotOffset.left + 10
  top = plotOffset.top + 10
  $(zoomOutButtonTemplate(left, top)).appendTo($("#" + config.chartId)).click (event) ->
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
  plotOffset = config.plot.getPlotOffset()
  addZoomOutButton plotOffset, config, originalBounds

window.addZoomSupport = (config) ->
  originalBounds = getPlotBounds config.plot 
  $("#" + config.chartId).bind "plotselected", onChartSelected(config, originalBounds)

window.formatDate = (date) ->
  time = new Date(date).toLocaleTimeString()
  date = new Date(date).toLocaleDateString()
  "#{time} #{date}" 

window.formatDateWithoutTime = (date) ->
  new Date(date).toLocaleDateString()
