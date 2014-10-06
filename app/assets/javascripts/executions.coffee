chartOptions = 
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
  xaxis:
    mode: "time"
    axisLabel: "Date"
  yaxis:
    axisLabel: "# of executions"
  series:
    bars:
      show: true
      align: "center"
      barWidth: 24*60*60*1000
      fill: true
      lineWidth: 1
      fillColor:
        colors: [
          { opacity: 0.8 }
          { opacity: 0.1 }
        ]
    points:
      show: false
  selection:
    mode: "xy"
  legend:
    show: false
  axisLabels:
    show: true

$(document).ready ->

  $("form#filter-form").submit (e) ->
    $("#configuration-select").removeAttr "name" if $("#configuration-select").val() is ""

  $("#configuration-select").change ->
    $("#filter-form").submit()

window.createExecutionVolumeChart = (data) ->  
  series = [
    label: "Execution counts"
    data: data
    color: "#2A9DFA"
  ]
  plot = $.plot $("#executions-chart"), series, chartOptions
  $("#executions-chart").bind "plothover", onChartHover([data])
  initialiseTooltip()

  addZoomSupport
    plot: plot
    chartId: "executions-chart"
    series: series
    chartOptions: chartOptions
    minX: 10 * 60 * 1000
    minY: 10

onChartHover = (seriesData) -> (event, pos, item) ->
  console.log "onChartHover()"
  if item
    console.log "onChartHover: " + item.seriesIndex + ", " + item.dataIndex
    dataItem = seriesData[item.seriesIndex][item.dataIndex]
    date = dataItem[0]
    count = dataItem[1]

    tooltipText = "#{count} tests executed on #{formatDateWithoutTime(date)}"
    $("#chart-tooltip").html(tooltipText).css(
      top: item.pageY + 5
      left: item.pageX + 5
    ).fadeIn(200)
  else
    $("#chart-tooltip").hide()

initialiseTooltip = ->
  $("<div class='chart-tooltip' id='chart-tooltip'/>").appendTo "body"
