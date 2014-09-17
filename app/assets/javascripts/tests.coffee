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
    $("#group-name-field").removeAttr "name" if $("#group-name-field").val() is ""

  nameEngine.initialize()
  groupEngine.initialize()

  $("#test-name-field").typeahead
    hint: true
    highlight: true
    minLength: 1
  ,
    name: "testNames"
    displayKey: "val"
    source: nameEngine.ttAdapter()

  $("#group-name-field").typeahead
    hint: true
    highlight: true
    minLength: 1
  ,
    name: "groups"
    displayKey: "val"
    source: groupEngine.ttAdapter()

nameEngine = new Bloodhound(
  name: "testNames"
  limit: 12
  remote:
    url: "http://localhost:9000/webApi/tests/names?query=%QUERY"
    filter: (resp) ->
      ( {val: s} for s in resp )
  datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.val
  queryTokenizer: Bloodhound.tokenizers.whitespace
)

groupEngine = new Bloodhound(
  name: "groups"
  limit: 12
  remote:
    url: "http://localhost:9000/webApi/tests/groups?query=%QUERY"
    filter: (resp) ->
      ( {val: s} for s in resp )
  datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.val
  queryTokenizer: Bloodhound.tokenizers.whitespace
)

