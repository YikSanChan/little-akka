package littleakka

import java.util.concurrent.TimeUnit

import scala.concurrent.{ExecutionContext, Future}

/** Demonstrate tell (!) is working
  */
object Tell {

  class SimpleActor extends Actor {
    override def receive: Receive = { case msg ⇒
      println(s"Received $msg")
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val system = new ActorSystem()
    val actor = system.actorOf(classOf[SimpleActor], "simple")

    for (i <- 1 to 100) {
      Future {
        Thread.sleep(10)
        actor ! s"Hello [$i]"
      }
    }

    system.awaitTermination(1, TimeUnit.SECONDS)
  }
}
