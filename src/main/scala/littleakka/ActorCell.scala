package littleakka

// TODO: become/unbecome
// TODO: sender()

// Associate actor behavior, the dispatcher and the mailbox
class ActorCell(clazz: Class[_], val dispatcher: Dispatcher) {
  private val _mailbox = new Mailbox(new UnboundedMessageQueue())
  _mailbox.setActor(this)

  private val receive: Actor.Receive =
    clazz.getDeclaredConstructor().newInstance().asInstanceOf[Actor].receive

  private def receiveMessage(messageHandle: Envelope): Unit = {
    receive(messageHandle.message)
  }

  def mailbox: Mailbox = _mailbox

  /**
    * Receive message
    */
  def invoke(messageHandle: Envelope): Unit = {
    receiveMessage(messageHandle)
  }

  /**
    * Send message
    */
  def sendMessage(message: Any): Unit = {
    dispatcher.dispatch(this, Envelope(message))
  }
}
