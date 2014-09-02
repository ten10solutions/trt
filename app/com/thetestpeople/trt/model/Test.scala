package com.thetestpeople.trt.model

/**
 * An atomic test case.
 *
 * For example, in JUnit or TestNG, this would be a test method.
 */
case class Test(
    id: Id[Test] = Id.dummy,
    /**
     * (Unqualified) name of the test.
     *
     * For example, in JUnit or TestNG, this would be the name of the test method.
     */
    name: String,
    /**
     * Name of the group / container of this test.
     *
     * For example, in JUnit or TestNG, this would be the (fully-qualified)
     * name of the class containing the test method.
     */
    groupOpt: Option[String]) extends EntityType {

  def qualifiedName = QualifiedName(name, groupOpt)

}