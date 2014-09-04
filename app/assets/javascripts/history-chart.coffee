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
  $.plot $("#" + chartId), series, chartOptions
  $("#" + chartId).bind "plothover", onChartHover(seriesData)
  initialiseTooltip()

prettyDate = (epochMillis) ->
  time = new Date(epochMillis).toLocaleTimeString()
  date = new Date(epochMillis).toLocaleDateString()
  "#{time} #{date}" 
  
onChartHover = (seriesData) -> (event, pos, item) ->
  if item
    dataItem = seriesData[item.seriesIndex][item.dataIndex]
    epochMillis = dataItem[0]
    count = dataItem[1]

    tooltipText = "#{count} #{item.series.label} on #{prettyDate(epochMillis)}"
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body"
