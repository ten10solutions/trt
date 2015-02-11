makeEngine = (name, url) -> 
  new Bloodhound(
    name: name
    limit: 12
    remote:
      url: url
      wildcard: "__QUERY__"
      filter: (resp) -> ({val: s} for s in resp)
    datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.val
    queryTokenizer: Bloodhound.tokenizers.whitespace
   )

window.addTypeahead = (id, name, queryUrl) ->
  engine = makeEngine name, queryUrl
  engine.initialize()
  $("#" + id).typeahead
    hint: true
    highlight: true
    minLength: 1
  ,
    name: name
    displayKey: "val"
    source: engine.ttAdapter()
