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

  $(".tag a").mouseover toggleCloseColor
  $(".tag a").mouseout toggleCloseColor

  $("#filter-form").submit (e) ->
    $("#test-name-field").removeAttr "name" if $("#test-name-field").val() is ""
    $("#group-name-field").removeAttr "name" if $("#group-name-field").val() is ""
    $("#category-field").removeAttr "name" if $("#category-field").val() is ""

  $("#configuration-select").change ->
    $("#configuration-form").submit()

  addTypeahead "test-name-field", "testNames", "/webApi/tests/names?query=%QUERY"
  addTypeahead "group-name-field", "groupNames", "/webApi/tests/groups?query=%QUERY"
  addTypeahead "category-field", "categoryNames", "/webApi/categories?query=%QUERY"

  $("#filter-tests-header-bar").click ->
    toggleFilters()

toggleFilters = ->
  $('.filter-widget,#expand-filter,#collapse-filter').toggle()
