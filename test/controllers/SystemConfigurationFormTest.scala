package controllers

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.Configuration
import org.scalatest._
import org.scalatest.FlatSpec
import com.thetestpeople.trt.model.SystemConfiguration

@SlowTest
@RunWith(classOf[JUnitRunner])
class SystemConfigurationFormTest extends FlatSpec with ShouldMatchers {

  "System configuration" should "not allow negative values" in {
    val config = SystemConfiguration(failureCountThreshold = -1)
    val Seq(error) = SystemConfigurationForm.form.fillAndValidate(config).errors
    error.key should equal ("brokenCountThreshold")
    error.message should include ("negative")
  }

}