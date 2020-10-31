package littleakka

object Actor {
  type Receive = PartialFunction[Any, Unit]

  /** Default placeholder (null) used for "!" to indicate that there is no sender of the message,
    * that will be translated to the receiving system's deadLetters.
    */
  final val noSender: ActorRef = null
}

trait Actor {
  type Receive = Actor.Receive

  def receive: Receive

  implicit val context: ActorContext = ActorCell.context.get

  implicit final val self: ActorRef = context.self

  final def sender(): ActorRef = context.sender()
}
