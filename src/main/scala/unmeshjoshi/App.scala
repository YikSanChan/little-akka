package unmeshjoshi

import java.util.concurrent.TimeUnit

import unmeshjoshi.Actor.Receive

import scala.concurrent.{ExecutionContext, Future}

class SimpleActor extends Actor {
  override def receive: Receive = {
    case msg ⇒
      println(s"Received $msg")
  }
}

object App {

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val system = new ActorSystem()

    val actor = system.actorOf(classOf[SimpleActor])
    for (i <- 1 to 100) {
      Future {
        actor ! s"Hello [$i]"
      }
    }

    system.awaitTermination(1, TimeUnit.SECONDS)
  }
}
