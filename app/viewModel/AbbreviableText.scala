package viewModel

case class AbbreviableText(s: String) {

  def full = s

  def abbreviated = if (s.length < 72) s else s.take(72) + "..."

}