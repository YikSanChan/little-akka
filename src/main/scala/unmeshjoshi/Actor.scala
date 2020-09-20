package unmeshjoshi

object Actor {
  type Receive = PartialFunction[Any, Unit]
}

trait Actor {
  def receive: Actor.Receive
}
