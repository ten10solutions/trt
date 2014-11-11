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
    minTickSize: 1
    tickDecimals: 0
    min: 0  
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
    color: "#bbe"
  axisLabels:
    show: true

tooltipTemplate = Handlebars.compile """
<table class='tooltip-table'>
  <tr>
    <td class='tooltip-header' colspan='2'>{{batchName}}</td>
  </tr>
  <tr>
    <td>Date</td>
    <td>{{when}}</td>
  </tr>
  {{#if passed}}
    <tr>
      <td>Passed</td>
      <td style='text-align: center'><span class='badge badge-success'>{{passed}}</span></td>
    </tr>
  {{/if}}
  {{#if failed}}
    <tr>
      <td>Failed</td>
      <td style='text-align: center'><span class='badge badge-error'>{{failed}}</span></td>
    </tr>
  {{/if}}
  <tr>
    <td>Total</td>
    <td style='text-align: center'><span class='badge badge-inverse'>{{total}}</span></td>
  </tr>
</table>
"""

onChartHover = (batchNames, passes, fails) -> (event, pos, item) ->
  if item
    dataItem = undefined
    failCount = fails[item.dataIndex][1]
    passCount = passes[item.dataIndex][1]
    batchName = batchNames[item.dataIndex]
    date = passes[item.dataIndex][0]
    tooltipText = tooltipTemplate
      when: formatDateAndTime(date)
      batchName: batchName
      passed: passCount
      failed: failCount
      total: passCount + failCount
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

window.saveChartData = (batchNames, batchUrls, passes, fails) ->
  chartData =
     batchNames: batchNames
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
  $("#batch-chart").bind "plothover", onChartHover(chartData.batchNames, chartData.passes, chartData.fails)

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

  $("#job-select").change ->
    $("#filter-form").submit()

  $("#configuration-select").change ->
    $("#filter-form").submit()

  setButtonEnableState()
  $(".batchCheckbox").click ->
    setButtonEnableState()
