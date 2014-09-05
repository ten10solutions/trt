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

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body"

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

  addZoomSupport
    plot: plot
    chartId: chartId
    series: series
    chartOptions: chartOptions
    minX: 10 * 60 * 1000
    minY: 10
  $("#" + chartId).bind "plothover", onChartHover(seriesData)

  initialiseTooltip()
