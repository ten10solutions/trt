createSeries = (counts) -> 
  [
    {
      label: "Pass"
      data: counts.pass
      color: "#609000"
    }
    {
      label: "Fail"
      data: counts.fail
      color: "#b94a48"
    }
  ]

sliceLabel = (label, series) ->
  count = Math.round(series.data[0][1])
  "<div class='slice-label'>#{label}<br/>#{count}</div>"

chartOptions =
  series: 
    pie: 
      show: true
      radius: 1
      label: 
        show: true
        radius: 3/4
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

pieClick = (urls) -> (event, pos, obj) ->
  if  (!obj)
    return
  else if (obj.series.label == "Pass")
    window.location = urls.pass
  else if (obj.series.label == "Fail")
    window.location = urls.fail;

window.createPieChart = (counts, urls) ->
  slices = createSeries(counts)
  $.plot('#pie-chart', slices, chartOptions)
  $("#pie-chart").bind("plotclick", pieClick(urls))
