package com.thetestpeople.trt.utils

case class UsernameAndPassword(username: String, password: String)

trait HttpOps {

  def post(
    url: String,
    body: Map[String, Seq[String]] = Map(),
    basicAuthOpt: Option[UsernameAndPassword] = None)

}