package com.thetestpeople.trt.utils

import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

object WsUtils {
  
  def newWsClient = new NingWSClient(new NingAsyncHttpClientConfigBuilder().build())
  
}