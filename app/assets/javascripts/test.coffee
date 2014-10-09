durationFormatter = (val, axis) -> formatDuration(val)

formatDuration = (val) ->
  if val < 1
    (val * 1000).toFixed(0) + " ms"
  else if val < 10
    val.toFixed(1) + " s"
  else
    val.toFixed(0) + " s"

chartOptions =
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
    axisLabel: "Time test was executed"
  yaxis:
    axisLabel: "Test duration"
    tickFormatter: durationFormatter
    #tickDecimals: 0 # Don't do this
    min: 0
  series:
    bars:
      show: true
    points:
      show: true
  selection:
    mode: "xy"
  axisLabels:
    show: true

onChartHover = (event, pos, item) ->
  if item
    eventDate = item.datapoint[0]
    seconds = item.datapoint[1]
    tooltipText = "#{formatDuration(seconds)} &mdash; #{formatDate(eventDate)}"
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

window.createDurationChart = (executionUrls, fails, passes) ->
  series = createSeries fails, passes

  plot = $.plot $("#duration-chart"), series, chartOptions

  addZoomSupport
    plot: plot
    chartId: "duration-chart"
    series: series
    chartOptions: chartOptions
    minX: 10 * 60 * 1000
    minY: 0.01
  $("#duration-chart").bind "plotclick", onChartClick(executionUrls)
  $("#duration-chart").bind "plothover", onChartHover

  initialiseTooltip()

  $("#configuration-select").change ->
    $("#configuration-form").submit()
