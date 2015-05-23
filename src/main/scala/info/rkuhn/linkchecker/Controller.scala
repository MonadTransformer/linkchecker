package info.rkuhn
package linkchecker

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}

import scala.concurrent.duration._

object Controller {
  case class Check(url: String, depth: Int)
  case class Result(links: Set[String])

}

class Controller extends Actor with ActorLogging {

  import Controller._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _: Exception => SupervisorStrategy.Restart
  }

  var cache = Set.empty[String]

  context.setReceiveTimeout(10.seconds)

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0) context.watch(context.actorOf(Props(new Getter(url, depth - 1))))
      cache += url
    case Terminated(_)     => if (context.children.isEmpty) context.parent ! Result(cache)
    case ReceiveTimeout    => context.children foreach context.stop
  }

}