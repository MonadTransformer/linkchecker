package info.rkuhn
package linkchecker

import java.util.concurrent.Executor

import scala.concurrent.{Future, Promise}

trait WebClient {
  def get(url: String)(implicit exec: Executor): Future[String]

}

case class BadStatus(status: Int) extends RuntimeException

object AsyncWebClient extends WebClient {

  private val client = new com.ning.http.client.AsyncHttpClient

  def get(url: String)(implicit exec: Executor): Future[String] = {
    val (p, listenableFuture) = (Promise[String](), client.prepareGet(url).execute())

      listenableFuture.addListener(new Runnable {
      def run() = {
        val response = listenableFuture.get
        if (response.getStatusCode < 400)
          p.success(response.getResponseBodyExcerpt(128 * 1024))
        else p.failure(BadStatus(response.getStatusCode))
      }
    }, exec)
    p.future
  }

  def shutdown(): Unit = client.close()

}