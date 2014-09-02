package com.thetestpeople.trt

object Config {

  object Jenkins {

    object Poller {

      val InitialDelay = "jenkins.poller.initialDelay"

      val Interval = "jenkins.poller.interval"

      val Enabled = "jenkins.poller.enabled"

    }

  }

  object Db {

    object Default {

      val Driver = "db.default.driver"

      val Url = "db.default.url"

      val User = "db.default.user"

      val Password = "db.default.password"

    }

  }

  object CountsCalculator {

    object Poller {

      val InitialDelay = "jenkins.poller.initialDelay"

      val Interval = "jenkins.poller.interval"

    }

  }

}