toggleCloseColor = ->
  opacity = $(this).css("opacity")
  opacity = (if opacity < 0.8 then 1 else 0.6)
  $(this).css opacity: opacity

window.performTestAction = (action) ->
  $("#testActionForm").attr("action", action).submit()

$(document).ready ->
  setButtonEnableState = ->
    disabled = $("input:checkbox:checked").length is 0
    $("button#rerunSelected").prop "disabled", disabled
    $("button#deleteSelected").prop "disabled", disabled

  $("#selectAll").change ->
    c = @checked
    checkboxes = $(this).closest("form").find(":checkbox")
    checkboxes.prop "checked", c

  setButtonEnableState()

  $(".testCheckbox").click ->
    setButtonEnableState()

  $(document).ready ->
    $(".tag a").mouseover toggleCloseColor
    $(".tag a").mouseout toggleCloseColor

  $("#filter-form").submit (e) ->
    $("#test-name-field").removeAttr "name" if $("#test-name-field").val() is ""
