package utils

object UtrValidator {
  def isValidUtr(utr: String): Boolean = {
    val utrPattern: Regex = "^[A-Za-z0-9]{1,10}$".r
    utrPattern.findFirstMatchIn(utr) match {
      case Some(_) => true
      case None    => false
    }
  }
}