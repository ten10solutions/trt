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
      show: false
  selection:
    mode: "xy"
  legend:
    show: false

window.createHistoryChart = (chartId, passes, warnings, fails) ->  
  series = [] # We omit empty series, otherwise JFlot displays no data
  if passes.length > 0
    series.push
      label: "Passes"
      data: passes
      color: "#609000"
  if warnings.length > 0
    series.push
      label: "Warnings"
      data: warnings
      color: "#FFBF00"
  if fails.length > 0
    series.push
      label: "Failures"
      data: fails
      color: "#b94a48"
  $.plot $("#" + chartId), series, chartOptions
  $("#" + chartId).bind "plothover", onChartHover(passes, warnings, fails)
  initialiseTooltip()

prettyDate = (epochMillis) ->
  time = new Date(epochMillis).toLocaleTimeString()
  date = new Date(epochMillis).toLocaleDateString()
  "#{time} #{date}" 
  
onChartHover = (passes, warnings, fails) -> (event, pos, item) ->
  if item
    dataItem = undefined
    if item.seriesIndex == 0
      dataItem = passes[item.dataIndex]
    else if item.seriesIndex == 1
      dataItem = warnings[item.dataIndex]
    else if item.seriesIndex == 2
      dataItem = fails[item.dataIndex]
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
