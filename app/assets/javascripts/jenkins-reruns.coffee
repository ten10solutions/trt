renumber = ->
  $(".paramSection").each (i) ->
    $("input", this).each ->
      $(this).attr "name", $(this).attr("name").replace(/params\[(\d+|x)\]/g, "params[" + i + "]")
      $(this).attr "id", $(this).attr("id").replace(/params_(\d+|x)/g, "params_" + i)

    $("label", this).each ->
      $(this).attr "for", $(this).attr("for").replace(/params_(\d+|x)/g, "params_" + i)

$(document).on "click", "button.delete-param", (e) ->
  $(this).parents(".paramSection").remove()
  renumber()

$(document).on "click", "button#addParam", (e) ->
  template = $(".paramSection-template")
  template.before "<div class=\"panel panel-default paramSection\">" + template.html() + "</div>"
  renumber()
