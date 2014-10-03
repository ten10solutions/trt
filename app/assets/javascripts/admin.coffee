window.deleteAll = (action) ->
  bootbox.confirm "Are you sure you want to delete all data in the database (cannot be undone)?", (confirmed) ->
    $("#delete-all-form").attr("action", action).submit() if confirmed


