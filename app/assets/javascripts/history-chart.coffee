chartOptions = 
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
  series:
    stack: true
    lines:
      show: true
      fill: true
      fillColor:
        colors: [
          { opacity: 0.1 }
          { opacity: 0.8 }
        ]
    points:
      show: false
  selection:
    mode: "xy"
  legend:
    show: false

onChartHover = (seriesData) -> (event, pos, item) ->
  if item
    dataItem = seriesData[item.seriesIndex][item.dataIndex]
    date = dataItem[0]
    count = dataItem[1]

    tooltipText = "#{count} #{item.series.label} on #{formatDate(date)}"
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()

formatDate = (date) ->
  time = new Date(date).toLocaleTimeString()
  date = new Date(date).toLocaleDateString()
  "#{time} #{date}" 

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body"

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
onChartSelected = (chartId, series, originalBounds) -> (event, ranges) ->
  clampRanges ranges
  zoomedOptions = merge chartOptions,
    xaxis:
      min: ranges.xaxis.from
      max: ranges.xaxis.to
    yaxis:
      min: ranges.yaxis.from
      max: ranges.yaxis.to
  $.plot "#" + chartId, series, zoomedOptions
  addZoomOutButton chartId, series, originalBounds

# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = "<div class='zoom-out-button'>Zoom out</div>"

addZoomOutButton = (chartId, series, originalBounds) ->
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

window.createHistoryChart = (chartId, passes, warnings, fails) ->  
  series = []
  seriesData = []
  if passes.length > 0  # omit empty series otherwise JFlot displays no data
    series.push
      label: "Passes"
      data: passes
      color: "#609000"
    seriesData.push(passes)
  if warnings.length > 0
    series.push
      label: "Warnings"
      data: warnings
      color: "#FFBF00"
    seriesData.push(warnings)
  if fails.length > 0
    series.push
      label: "Failures"
      data: fails
      color: "#b94a48"
    seriesData.push(fails)
  plot = $.plot $("#" + chartId), series, chartOptions
  originalBounds = getPlotBounds plot 

  $("#" + chartId).bind "plothover", onChartHover(seriesData)
  $("#" + chartId).bind "plotselected", onChartSelected(chartId, series, originalBounds)

  initialiseTooltip()
