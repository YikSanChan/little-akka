package unmeshjoshi

trait Actor {
  def receive: PartialFunction[Any, Unit]
}