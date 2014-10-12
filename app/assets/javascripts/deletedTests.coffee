$(document).ready ->
  setButtonEnableState = ->
    disabled = $("input:checkbox:checked").length is 0
    $("button#undeleteSelected").prop "disabled", disabled

  $("#selectAll").change ->
    c = @checked
    checkboxes = $(this).closest("form").find(":checkbox")
    checkboxes.prop "checked", c

  setButtonEnableState()

  $(".testCheckbox").click ->
    setButtonEnableState()
