package info.rkuhn.linkchecker

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

object ReceptionistSpec {

  def fakeReceptionist: Props =
    Props(new Receptionist {
      override def controllerProps = Props[FakeController]
    })

  class FakeController extends Actor {

    import context.dispatcher

    def receive = {
      case Controller.Check(url, depth) =>
        context.system.scheduler.scheduleOnce(1.second, sender(), Controller.Result(Set(url)))
    }
  }

}

class ReceptionistSpec extends TestKit(ActorSystem("ReceptionistSpec"))
with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  import Receptionist._
  import ReceptionistSpec._

  override def afterAll(): Unit = {
    system.shutdown()
  }

  "A Receptionist" must {

    "reply with a result" in {
      val receptionist = system.actorOf(fakeReceptionist, "sendResult")
      receptionist ! Get("myURL")
      expectMsg(Result("myURL", Set("myURL")))
    }

    "reject request flood" in {
      val receptionist = system.actorOf(fakeReceptionist, "rejectFlood")
      for (i <- 1 to 5) receptionist ! Get(s"myURL$i")
      expectMsg(Failed("myURL5", "queue overflow"))
      for (i <- 1 to 4) expectMsg(Result(s"myURL$i", Set(s"myURL$i")))
    }

  }

}