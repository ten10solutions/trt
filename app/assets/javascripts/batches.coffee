chartOptions = 
  grid:
    backgroundColor:
      colors: ["#fff", "#eee"]
    hoverable: true
    clickable: true
  xaxis:
    mode: "time"
    axisLabel: "Date"
  yaxis:
    axisLabel: "# of executions in batch"
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
  axisLabels:
    show: true

onChartHover = (passes, fails) -> (event, pos, item) ->
  if item
    dataItem = undefined
    if item.seriesIndex is 0
      dataItem = fails[item.dataIndex]
    else
      dataItem = passes[item.dataIndex]
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

onChartClick = (batchUrls) -> (event, pos, item) ->
  if item
    window.location = batchUrls[item.dataIndex]

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

  addZoomSupport
    plot: plot
    chartId: "batch-chart"
    series: series
    chartOptions: chartOptions
    minX: 10 * 60 * 1000
    minY: 10

  $("#batch-chart").bind "plotclick", onChartClick(chartData.batchUrls)
  $("#batch-chart").bind "plothover", onChartHover(chartData.passes, chartData.fails)

  initialiseTooltip()

window.deleteSelected = (action) ->
  bootbox.confirm "Are you sure you want to delete the selected batch(es)?", (confirmed) ->
    $("#batch-action-form").attr("action", action).submit()  if confirmed

window.showChart = (action) ->
  $("#chart-hidden-message").hide()
  $("#batch-chart").removeClass("not-shown")
  createPassFailChart()

$(document).ready ->
  setButtonEnableState = ->
    disabled = $("input:checkbox:checked").length is 0
    $("button#delete-selected").prop "disabled", disabled

  $("#select-all").change ->
    c = @checked
    checkboxes = $(this).closest("form").find(":checkbox")
    checkboxes.prop "checked", c

  $("form#filter-form").submit (e) ->
    $("#job-select").removeAttr "name" if $("#job-select").val() is ""
    $("#configuration-select").removeAttr "name" if $("#configuration-select").val() is ""

  setButtonEnableState()
  $(".batchCheckbox").click ->
    setButtonEnableState()
