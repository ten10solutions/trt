setButtonEnableState = ->
  disabled = $('input:checkbox:checked').length == 0
  $('button#rerunSelected').prop('disabled', disabled)

$(document).ready ->
  $('#selectAll').change ->
    checkboxes = $(this).closest('form').find(':checkbox')
    checkboxes.prop('checked', this.checked)

  setButtonEnableState()

  $('.executionCheckbox').click ->
    setButtonEnableState()

window.rerunSelected = (action) ->
  $("#executionActionForm").attr("action", action).submit()

window.deleteBatch = (action) ->
  bootbox.confirm "Are you sure you want to delete this batch?", (confirmed) ->
    $("#delete-batch-form").attr("action", action).submit() if confirmed
