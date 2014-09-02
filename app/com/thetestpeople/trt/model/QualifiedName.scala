package com.thetestpeople.trt.model

object QualifiedName {

  val ordering: Ordering[QualifiedName] = Ordering.by(n ⇒ (n.groupOpt, n.name))

  def apply(name: String, group: String): QualifiedName = QualifiedName(name, Some(group))

}

case class QualifiedName(

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
    groupOpt: Option[String] = None) extends Ordered[QualifiedName] {

  override def toString = {
    val prefix = groupOpt match {
      case Some(group) ⇒ group + "."
      case None        ⇒ ""
    }
    s"$prefix$name"
  }

  override def compare(other: QualifiedName) = QualifiedName.ordering.compare(this, other)

}
