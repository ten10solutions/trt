package com.thetestpeople.trt.model

trait EntityType

case class Id[T <: EntityType](value: Int) {

  override def toString = value.toString

  def asString = value.toString

}

object Id {

  implicit def idOrdering[T <: EntityType]: Ordering[Id[T]] = Ordering.fromLessThan[Id[T]](_.value < _.value)

  /**
   * A dummy ID, e.g. for inserting new rows
   */
  def dummy[T <: EntityType] = Id[T](-1)

  def parse[T <: EntityType](s: String): Option[Id[T]] =
    try
      Some(Id[T](s.toInt))
    catch {
      case e: NumberFormatException ⇒ None
    }

}