$(document).ready ->

  setUpDetailsToggleButton('job-details', 'show-job-details-button', 'hide-job-details-button')

window.setUpDetailsToggleButton = (detailsId, showButtonId, hideButtonId) ->

  $('#' + showButtonId).click ->
    $('#' + detailsId).fadeToggle(120)
    $('#' + showButtonId).hide()
    $('#' + hideButtonId).show()

  $('#' + hideButtonId).click ->
    $('#' + detailsId).fadeToggle(120)
    $('#' + showButtonId).show()
    $('#' + hideButtonId).hide()
   
