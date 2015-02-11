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

  $("#filter-form").submit (e) ->
    $("#test-name-field").removeAttr "name" if $("#test-name-field").val() is ""
    $("#group-name-field").removeAttr "name" if $("#group-name-field").val() is ""
    $("#category-field").removeAttr "name" if $("#category-field").val() is ""

  $("#configuration-select").change ->
    $("#configuration-form").submit()

  testQueryTemplate = jsRoutes.controllers.WebApiController.testNames("__QUERY__").url
  addTypeahead "test-name-field", "testNames", testQueryTemplate

  groupQueryTemplate = jsRoutes.controllers.WebApiController.groups("__QUERY__").url
  addTypeahead "group-name-field", "groupNames", groupQueryTemplate

  categoryQueryTemplate = jsRoutes.controllers.WebApiController.categories("__QUERY__").url
  addTypeahead "category-field", "categoryNames", categoryQueryTemplate

  $("#filter-tests-header-bar").click ->
    $('.filter-widget, #expand-filter, #collapse-filter').toggle()
