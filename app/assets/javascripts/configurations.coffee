chartData = {}

modalChartIndex = null

window.loadHistoryChart = (dataUrl, chartIndex) ->
  $.get(dataUrl).complete (d) ->
    counts = d.responseJSON
    chartData[chartIndex] = counts 
    if counts.length > 0
      renderChart chartIndex, chartIndex
      $(".chart-widget-#{chartIndex}").show()

renderChart = (chartIndex, chartIdSuffix) ->
  counts = chartData[chartIndex]
  if counts.length > 0
    chartId = "history-chart-#{chartIdSuffix}"
    includeHealthy  = $("#chart-checkbox-healthy-#{chartIdSuffix}").is(":checked")
    includeWarnings = $("#chart-checkbox-warnings-#{chartIdSuffix}").is(":checked")
    includeBroken   = $("#chart-checkbox-broken-#{chartIdSuffix}").is(":checked")
    createHistoryChart chartId, counts, window.timelineBounds, includeHealthy, includeWarnings, includeBroken

$(document).ready ->

  $(".chart-checkbox").change ->
    chartIndex = parseInt($(this).data("chart-index"))
    renderChart chartIndex, chartIndex

  $(".modal-chart-checkbox").change ->
    renderChart modalChartIndex, "modal"

  $(".expand-chart-button").click ->
    modalChartIndex = parseInt $(this).data("chart-index")
    configuration = $(this).data("configuration")
    $("#big-chart-modal").modal()
    $("#modal-configuration-name").text(configuration)

    # We need to make sure the modal dialog is showing before Flot tries to draw the chart
    # otherwise it renders the axes incorrecly.
    $("#big-chart-modal").on 'shown.bs.modal', ->
      renderChart modalChartIndex, "modal"
