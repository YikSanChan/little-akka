package unmeshjoshi

object Actor {
  type Receive = PartialFunction[Any, Unit]
}

trait Actor {
  type Receive = Actor.Receive
  def receive: Receive
}
