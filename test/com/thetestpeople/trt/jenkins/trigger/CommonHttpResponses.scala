package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.utils.http.HttpResponse


object CommonHttpResponses {

  def badCredentialsResponse() = HttpResponse(
    status = 401,
    statusText = "Bad credentials",
    body = """<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
<title>Error 401 Bad credentials</title>
</head>
<body><h2>HTTP ERROR 401</h2>
<p>Problem accessing /crumbIssuer/api/json. Reason:
<pre>    Bad credentials</pre></p><hr /><i><small>Powered by Jetty://</small></i><br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                

</body>
</html>""")

  def forbiddenResponse() = HttpResponse(
    status = 403,
    statusText = "Forbidden",
    body = """<html><head><meta http-equiv='refresh' content='1;url=/login?from=%2FcrumbIssuer%2Fapi%2Fjson'/><script>window.location.replace('/login?from=%2FcrumbIssuer%2Fapi%2Fjson');</script></head><body style='background-color:white; color:white;'>


Authentication required
<!--
You are authenticated as: anonymous
Groups that you are in:
  
Permission you need to have (but didn't): hudson.model.Hudson.Read
 ... which is implied by: hudson.security.Permission.GenericRead
 ... which is implied by: hudson.model.Hudson.Administer
-->

</body></html>  """)

}