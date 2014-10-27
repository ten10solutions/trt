toggleCloseColor = ->
  opacity = $(this).css("opacity")
  opacity = (if opacity < 0.8 then 1 else 0.6)
  $(this).css opacity: opacity

window.performTestAction = (action) ->
  $("#testActionForm").attr("action", action).submit()

makeEngine = (name, url) -> 
  new Bloodhound(
    name: name
    limit: 12
    remote:
      url: url
      filter: (resp) -> ({val: s} for s in resp)
    datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.val
    queryTokenizer: Bloodhound.tokenizers.whitespace
   )

nameEngine = makeEngine "testNames", "/webApi/tests/names?query=%QUERY"
groupEngine = makeEngine "groups", "/webApi/tests/groups?query=%QUERY"
categoryEngine = makeEngine "categories", "/webApi/categories?query=%QUERY"

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

  nameEngine.initialize()
  groupEngine.initialize()
  categoryEngine.initialize()

  addTypeahead "test-name-field", "testNames", nameEngine
  addTypeahead "group-name-field", "groupNames", groupEngine
  addTypeahead "category-field", "categoryNames", categoryEngine

addTypeahead = (id, name, engine) ->
  $("#" + id).typeahead
    hint: true
    highlight: true
    minLength: 1
  ,
    name: name
    displayKey: "val"
    source: engine.ttAdapter()

