package com.thetestpeople.trt.utils

import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import play.api.libs.ws.DefaultWSClientConfig

object WsUtils {

  def newWsClient = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new DefaultWSClientConfig).build())
  
}