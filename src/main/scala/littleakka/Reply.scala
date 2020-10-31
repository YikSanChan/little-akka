package littleakka

import java.util.concurrent.TimeUnit

/** Demonstrate sender() and self is working
  */
object Reply {

  private final case class StartPing(ponger: ActorRef)
  private final case object Ping
  private final case object Pong

  class Pinger extends Actor {

    override def receive: Receive = {
      case StartPing(ponger) =>
        println(s"[$self] Got StartPing from sender=${sender()}")
        ponger ! Ping
      case Pong =>
        println(s"[$self] Got Pong from sender=${sender()}")
    }
  }

  class Ponger extends Actor {

    override def receive: Receive = { case Ping =>
      println(s"[$self] Got Ping from sender=${sender()}")
      sender() ! Pong
    }
  }

  def main(args: Array[String]): Unit = {
    val system = new ActorSystem()
    val pinger1 = system.actorOf(classOf[Pinger], "pinger1")
    val pinger2 = system.actorOf(classOf[Pinger], "pinger2")
    val ponger = system.actorOf(classOf[Ponger], "ponger")
    pinger1 ! StartPing(ponger)
    pinger2 ! StartPing(ponger)
    system.awaitTermination(1, TimeUnit.SECONDS)
  }
}
