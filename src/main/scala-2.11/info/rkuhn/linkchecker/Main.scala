package info.rkuhn
package linkchecker

/**
 * Created by FransAdm on 20150522.
 */
object Main {
  def main(args: Array[String]): Unit = {
    akka.Main.main(Array(classOf[MainActor].getName))
  }

}
