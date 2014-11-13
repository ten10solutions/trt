package com.thetestpeople.trt

object Config {

  object Ci {

    object Poller {

      val InitialDelay = "ci.poller.initialDelay"

      val Interval = "ci.poller.interval"

      val Enabled = "ci.poller.enabled"

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

      val InitialDelay = "counts.poller.initialDelay"

      val Interval = "counts.poller.interval"

    }

  }

  object Lucene {

    final val IndexDirectory = "lucene.indexDirectory"

    final val InMemory = "lucene.inMemory"

  }

  object Http {

    final val UseCache = "http.useCache"

    final val Timeout = "http.timeout"

  }

}