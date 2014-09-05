chartOptions =
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
  series:
    bars:
      show: true
    points:
      show: true
  selection:
    mode: "xy"

# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = "<div class='zoom-out-button'>Zoom out</div>"

addZoomOutButton = (series, originalBounds) ->
  $(zoomOutButtonTemplate).appendTo($("#duration-chart")).click (event) ->
    event.preventDefault()
    zoomedOptions = merge chartOptions,
      xaxis:
        min: originalBounds.xFrom
        max: originalBounds.xTo
      yaxis:
        min: originalBounds.yFrom
        max: originalBounds.yTo
    $.plot "#duration-chart", series, zoomedOptions

getPlotBounds = (plot) ->
  xFrom: plot.getAxes().xaxis.from
  xTo: plot.getAxes().xaxis.to
  yFrom: plot.getAxes().yaxis.from
  yTo: plot.getAxes().yaxis.to

clampRanges = (ranges) -> 
  minutes = 60 * 1000
  if ranges.xaxis.to - ranges.xaxis.from < 10 * minutes
    ranges.xaxis.to = ranges.xaxis.from + 10 * minutes
  if ranges.yaxis.to - ranges.yaxis.from < 0.01
    ranges.yaxis.to = ranges.yaxis.from + 0.01  

onChartSelected = (series, originalBounds) -> (event, ranges) ->
  clampRanges ranges
  zoomedOptions = merge chartOptions,
    xaxis:
      min: ranges.xaxis.from
      max: ranges.xaxis.to
    yaxis:
      min: ranges.yaxis.from
      max: ranges.yaxis.to
  $.plot "#duration-chart", series, zoomedOptions
  addZoomOutButton series, originalBounds

onChartHover = (event, pos, item) ->
  if item
    eventDate = item.datapoint[0]
    seconds = item.datapoint[1]
    tooltipText = "#{seconds} seconds #{formatDate(eventDate)}"
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body"

onChartClick = (executionUrls) -> (event, pos, item) ->
  if item
    window.location = executionUrls[item.seriesIndex][item.dataIndex]

createSeries = (fails, passes) -> 
  [
    {
      label: "Duration, failures (seconds)"
      data: fails
      color: "#b94a48"
    }
    {
      label: "Duration, passes (seconds)"
      data: passes
      color: "#609000"
    }
  ]

window.createDurationChart = (executionUrls, fails, passes) ->
  series = createSeries fails, passes

  plot = $.plot $("#duration-chart"), series, chartOptions
  originalBounds = getPlotBounds plot

  #addZoomSupport(plot, "duration-chart", series, chartOptions)
  $("#duration-chart").bind "plotselected", onChartSelected(series, originalBounds)
  $("#duration-chart").bind "plotclick", onChartClick(executionUrls)
  $("#duration-chart").bind "plothover", onChartHover

  initialiseTooltip()
