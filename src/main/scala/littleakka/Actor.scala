package littleakka

object Actor {
  type Receive = PartialFunction[Any, Unit]
}

trait Actor {
  type Receive = Actor.Receive

  def receive: Receive

  // TODO
  // implicit val context: ActorContext = ActorCell.contextStack.get.head

  // TODO
  // implicit final val self: ActorRef = context.self //MUST BE A VAL, TRUST ME

  // TODO
  // final def sender(): ActorRef = context.sender()
}
