package littleakka

trait ActorRef {

  /** Sends a one-way asynchronous message. E.g. fire-and-forget semantics.
    *
    * If invoked from within an actor then the actor reference is implicitly passed on
    * as the implicit 'sender' argument.
    *
    * If not then no sender is available.
    */
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit
}

class LocalActorRef(clazz: Class[_], val dispatcher: Dispatcher)
    extends ActorRef {

  /** In akka, actorCell.init() is called the constructor,
    * which creates the mailbox, and creates the Actor instance.
    * To simplify, I move the init logic inside ActorCell constructor.
    */
  private val actorCell: ActorCell = new ActorCell(this, clazz, dispatcher)

  override def !(message: Any)(implicit
      sender: ActorRef = Actor.noSender
  ): Unit = actorCell.sendMessage(message, sender)
}
