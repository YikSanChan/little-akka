package littleakka

import java.util.concurrent.TimeUnit

/** Demonstrate sender() and self is working
  */
object Reply {

  private final case class StartPing(ponger: ActorRef)
  private final case class Ping(name: String)
  private final case class Pong(name: String)

  class Pinger extends Actor {

    override def receive: Receive = {
      case StartPing(ponger) =>
        println("Start Ping")
        ponger ! Ping("Pinger")
      case Pong(name) =>
        println(s"Got pong from $name, self=$self, sender=${sender()}")
    }
  }

  class Ponger extends Actor {

    override def receive: Receive = { case Ping(name) =>
      println(s"Got ping from $name")
      sender() ! Pong("Ponger")
    }
  }

  def main(args: Array[String]): Unit = {
    val system = new ActorSystem()
    val pinger1 = system.actorOf(classOf[Pinger])
    val pinger2 = system.actorOf(classOf[Pinger])
    val ponger = system.actorOf(classOf[Ponger])
    pinger1 ! StartPing(ponger)
    pinger2 ! StartPing(ponger)
    system.awaitTermination(1, TimeUnit.SECONDS)
  }
}
