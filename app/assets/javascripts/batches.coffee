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
          { opacity: 0.2 }
          { opacity: 0.8 }
        ]
    points:
      show: true
  selection:
    mode: "xy"

# (Recursively) merge two objects, returning a new object
merge = (obj1, obj2) -> $.extend true, {}, obj1, obj2

zoomOutButtonTemplate = "<div class='zoom-out-button'>Zoom out</div>"

addZoomOutButton = (series, originalBounds) ->
  $(zoomOutButtonTemplate).appendTo($("#batch-chart")).click (event) ->
    event.preventDefault()
    zoomedOptions = merge chartOptions,
      xaxis:
        min: originalBounds.xFrom
        max: originalBounds.xTo
      yaxis:
        min: originalBounds.yFrom
        max: originalBounds.yTo
    $.plot "#batch-chart", series, zoomedOptions

onChartHover = (passes, fails) -> (event, pos, item) ->
  if item
    dataItem = undefined
    if item.seriesIndex is 0
      dataItem = fails[item.dataIndex]
    else
      dataItem = passes[item.dataIndex]
    epochMillis = dataItem[0]
    count = dataItem[1]

    tooltipText = count + " " + item.series.label + " on " + new Date(epochMillis).toLocaleTimeString() + " " + new Date(epochMillis).toLocaleDateString()
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()


onChartClick = (batchUrls) -> (event, pos, item) ->
  if item
    window.location = batchUrls[item.dataIndex]  if item

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

onChartSelected = (series, originalBounds) -> (event, ranges) ->
  clampRanges ranges
  zoomedOptions = merge chartOptions,
    xaxis:
      min: ranges.xaxis.from
      max: ranges.xaxis.to
    yaxis:
      min: ranges.yaxis.from
      max: ranges.yaxis.to
  $.plot "#batch-chart", series, zoomedOptions
  addZoomOutButton series, originalBounds

createSeries = (fails, passes) -> 
  [
    {
      label: "Failures"
      data: fails
      color: "#b94a48"
    }
    {
      label: "Passes"
      data: passes
      color: "#609000"
    }
  ]

chartData = {}

window.saveChartData = (batchUrls, passes, fails) ->
  chartData =
     batchUrls: batchUrls
     passes: passes
     fails: fails

window.createPassFailChart = ->
  series = createSeries chartData.fails, chartData.passes

  plot = $.plot($("#batch-chart"), series, chartOptions)
  originalBounds = getPlotBounds plot

  $("#batch-chart").bind "plotselected", onChartSelected(series, originalBounds)
  $("#batch-chart").bind "plotclick", onChartClick(chartData.batchUrls)
  $("#batch-chart").bind "plothover", onChartHover(chartData.passes, chartData.fails)

  initialiseTooltip()

window.deleteSelected = (action) ->
  bootbox.confirm "Are you sure you want to delete the selected batch(es)?", (confirmed) ->
    $("#batchActionForm").attr("action", action).submit()  if confirmed

window.showChart = (action) ->
  $("#chart-hidden-message").hide()
  $("#batch-chart").removeClass("not-shown")
  createPassFailChart()

$(document).ready ->
  setButtonEnableState = ->
    disabled = $("input:checkbox:checked").length is 0
    $("button#deleteSelected").prop "disabled", disabled

  $("#selectAll").change ->
    c = @checked
    checkboxes = $(this).closest("form").find(":checkbox")
    checkboxes.prop "checked", c

  $("form#filter-form").submit (e) ->
    $("#job-select").removeAttr "name"  if $("#job-select").val() is ""

  setButtonEnableState()
  $(".batchCheckbox").click ->
    setButtonEnableState()
