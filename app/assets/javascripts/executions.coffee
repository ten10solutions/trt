$(document).ready ->

  $("form#filter-form").submit (e) ->
    $("#configuration-select").removeAttr "name" if $("#configuration-select").val() is ""
