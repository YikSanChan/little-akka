package viktorklang

object ActorSpec {

  import Actor._

  implicit val e: java.util.concurrent.Executor = java.util.concurrent.Executors.newCachedThreadPool

  def main(args: Array[String]): Unit = {
    val actor = Actor(self => msg => {
      println("self: " + self + " got msg " + msg)
      Die
    })

    actor ! "foo"
    actor ! "foo"
  }
}
