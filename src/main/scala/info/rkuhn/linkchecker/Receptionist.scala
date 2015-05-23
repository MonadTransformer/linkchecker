package info.rkuhn
package linkchecker

import akka.actor.{Actor, ActorRef, Address, Deploy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.cluster.{Cluster, ClusterEvent}
import akka.remote.RemoteScope

import scala.concurrent.duration._
import scala.util.Random

object Receptionist {
  case class Get(url: String)
  case class Result(url: String, links: Set[String])
  case class Failed(url: String, reason: String)
  private case class Job(client: ActorRef, url: String)
}

class Receptionist extends Actor {

  import Receptionist._

  val waiting: Receive = {case Get(url) => context.become(runNext(Vector(Job(sender(), url))))}
  var reqNo = 0

  def controllerProps: Props = Props[Controller]

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = waiting

  def running(queue: Vector[Job]): Receive = {
    case Controller.Result(links) =>
      val job = queue.head
      job.client ! Result(job.url, links)
      context.stop(context.unwatch(sender()))
      context.become(runNext(queue.tail))
    case Terminated(_)            =>
      val job = queue.head
      job.client ! Failed(job.url, "controller failed unexpectedly")
      context.become(runNext(queue.tail))
    case Get(url)                 => context.become(enqueueJob(queue, Job(sender(), url)))
  }

  def runNext(queue: Vector[Job]): Receive = {
    reqNo += 1
    if (queue.isEmpty) waiting
    else {
      val controller = context.actorOf(controllerProps, s"c$reqNo")
      context.watch(controller)
      controller ! Controller.Check(queue.head.url, 2)
      running(queue)
    }
  }

  def enqueueJob(queue: Vector[Job], job: Job): Receive = {
    if (queue.size > 3) {
      sender ! Failed(job.url, "queue overflow")
      running(queue)
    } else running(queue :+ job)
  }

}

/*
class ClusterReceptionist extends Actor {

  import ClusterEvent.{MemberRemoved, MemberUp}
  import Receptionist._

  val cluster = Cluster(context.system)
  cluster.subscribe(self, classOf[MemberUp])
  cluster.subscribe(self, classOf[MemberRemoved])
  val randomGen = new Random
  val awaitingMembers: Receive = {
    case Get(url)                                                  => sender ! Failed(url, "no nodes available")
    case current: ClusterEvent.CurrentClusterState                 =>
      val notMe = current.members.toVector map (_.address) filter (_ != cluster.selfAddress)
      if (notMe.nonEmpty) context.become(active(notMe))
    case MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(Vector(member.address)))
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def pick[A](coll: IndexedSeq[A]): A = coll(randomGen.nextInt(coll.size))

  def receive = awaitingMembers

  def active(addresses: Vector[Address]): Receive = {
    case Get(url) if context.children.size < addresses.size        =>
      val client = sender()
      val address = pick(addresses)
      context.actorOf(Props(new Customer(client, url, address)))
    case Get(url)                                                  =>
      sender ! Failed(url, "too many parallel queries")
    case MemberUp(member) if member.address != cluster.selfAddress =>
      context.become(active(addresses :+ member.address))
    case MemberRemoved(member, _)                                  =>
      val next = addresses filterNot (_ == member.address)
      if (next.isEmpty) context.become(awaitingMembers)
      else context.become(active(next))
  }
}

class Customer(client: ActorRef, url: String, node: Address) extends Actor {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
  implicit val s = context.parent
  val props = Props[Controller].withDeploy(Deploy(scope = RemoteScope(node)))
  val controller = context.actorOf(props, "controller")
  context.watch(controller)

  context.setReceiveTimeout(5.seconds)
  controller ! Controller.Check(url, 2)

  def receive = ({
    case ReceiveTimeout           =>
      context.unwatch(controller)
      client ! Receptionist.Failed(url, "controller timed out")
    case Terminated(_)            =>
      client ! Receptionist.Failed(url, "controller died")
    case Controller.Result(links) =>
      context.unwatch(controller)
      client ! Receptionist.Result(url, links)
  }: Receive) andThen (_ => context.stop(self))
}*/
