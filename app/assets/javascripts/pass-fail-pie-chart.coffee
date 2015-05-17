sliceLabel = (label, series) ->
  count = Math.round(series.data[0][1])
  "<div class='pie-slice-label'>#{label}<br/>#{count}</div>"

chartOptions = (showLabels) ->
  series:
    pie:
      show: true
      radius: 1
      label:
        show: showLabels
        radius: 3 / 4
        formatter: sliceLabel
        background:
          opacity: 0.5
          color: "#000"
      highlight:
        opacity: 0.2
  grid:
    hoverable: true
    clickable: true
  legend:
    show: false

unescapeAmpersands = (url) -> url.replace(/&amp;/g, "&")

window.createPieChart = (chartId, counts, urls, showLabels = true) ->
  slices = [
    {
      label: "Healthy"
      data: counts.pass
      color: "#609000"
    }
    {
      label: "Warning"
      data: counts.warn
      color: "#FFBF00"
    }
    {
      label: "Broken"
      data: counts.fail
      color: "#b94a48"
    }
    {
      label: "Ignored"
      data: counts.ignored
      color: "#909090"
    }
  ]
  $.plot ("#" + chartId), slices, chartOptions(showLabels)

  $("#" + chartId).bind "plotclick", (event, pos, obj) ->
    return unless obj
    if obj.series.label is "Healthy"
      window.location = unescapeAmpersands urls.pass
    else if obj.series.label is "Warning"
      window.location = unescapeAmpersands urls.warn
    else if obj.series.label is "Broken"
      window.location = unescapeAmpersands urls.fail
