package littleakka

/** The actor context - the view of the actor cell from the actor.
  * Exposes contextual information for the actor and the current message.
  */
trait ActorContext {

  /** The ActorRef representing this actor
    */
  def self: ActorRef

  /** Returns the sender 'ActorRef' of the current message.
    */
  def sender(): ActorRef
}

object ActorCell {

  /** In akka code, it is named contextStack and typed ThreadLocal[List[ActorContext],
    * but I don't get why it is a List, maybe it doesn't have to?
    * See https://gitter.im/akka/akka?at=5b570134e06d7e74099bab74
    * Also I don't fully understand why it has to be ThreadLocal.
    * According to ktoso:
    * The ThreadLocal is only ever hit once. It is to allow context usage in constructor body.
    * There is no way to set it otherwise, since that would be AFTER constructor has run
    */
  final val context: ThreadLocal[ActorContext] = new ThreadLocal[ActorContext] {
    override def initialValue: ActorContext = null
  }
}

/** Associate actor behavior, the dispatcher and the mailbox
  */
class ActorCell(val self: ActorRef, clazz: Class[_], val dispatcher: Dispatcher)
    extends ActorContext {

  import ActorCell._

  private val _mailbox = new Mailbox(new UnboundedMessageQueue())
  _mailbox.setActor(this)

  // In akka, it enqueues Create to the mailbox,
  // the message will later be processed in Mailbox#run before any custom message
  // To simplify, simply create the actor here, refer to ActorCell#create
  context.set(this)

  private var currentMessage: Envelope = _

  // In akka, the Class => Actor logic can be found in IndirectActorProducer
  private val actor: Actor =
    clazz.getDeclaredConstructor().newInstance().asInstanceOf[Actor]

  private val receive: Actor.Receive = actor.receive

  private def receiveMessage(messageHandle: Envelope): Unit = {
    receive(messageHandle.message)
  }

  // FIXME: if null or sender is null, sender should be deadLetters
  override def sender(): ActorRef = currentMessage.sender

  def mailbox(): Mailbox = _mailbox

  def invoke(messageHandle: Envelope): Unit = {
    currentMessage = messageHandle
    receiveMessage(messageHandle)
    currentMessage = null
  }

  def sendMessage(message: Any, sender: ActorRef): Unit = {
    dispatcher.dispatch(this, Envelope(message, sender))
  }
}
