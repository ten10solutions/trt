
package viewModel

import com.thetestpeople.trt.model.QualifiedName

trait HasTestName {

  def testName: QualifiedName

  def name: AbbreviableName = AbbreviableName(testName.name)

  def groupOpt: Option[AbbreviableName] = testName.groupOpt.map(AbbreviableName)

}