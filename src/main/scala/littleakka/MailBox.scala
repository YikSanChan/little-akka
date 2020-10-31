package littleakka

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentLinkedQueue, ForkJoinTask}

import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec

final case class Envelope(message: Any, sender: ActorRef)

trait MessageQueue {

  def enqueue(handle: Envelope): Unit

  def dequeue(): Envelope

  def numberOfMessages: Int

  def hasMessages: Boolean

  def cleanUp(deadLetters: MessageQueue): Unit
}

class UnboundedMessageQueue
    extends ConcurrentLinkedQueue[Envelope]
    with MessageQueue {

  override def enqueue(handle: Envelope): Unit = offer(handle)

  override def dequeue(): Envelope = poll()

  override def numberOfMessages: Int = size

  override def hasMessages: Boolean = !isEmpty

  def cleanUp(deadLetters: MessageQueue): Unit = {
    var envelope = dequeue()
    while (envelope ne null) {
      deadLetters.enqueue(envelope)
      envelope = dequeue()
    }
  }
}

// A Mailbox is an executable Task
class Mailbox(val messageQueue: MessageQueue)
    extends ForkJoinTask[Unit]
    with StrictLogging {

  // Mailbox status is simplified to either scheduled or idle
  private val idle = new AtomicBoolean(true)

  def setAsScheduled(): Boolean = {
    // if set success, then the new value is false, not idle means scheduled
    idle.compareAndSet(true, false)
  }

  def setAsIdle(): Boolean = {
    // if set success, then the new value is false, hence idle
    idle.compareAndSet(false, true)
  }

  def canBeScheduled: Boolean = {
    messageQueue.hasMessages
  }

  var actor: ActorCell = _

  def setActor(cell: ActorCell): Unit = actor = cell

  def dispatcher: Dispatcher = actor.dispatcher

  def enqueue(msg: Envelope): Unit = {
    logger.debug("enqueuing message {}", msg)
    messageQueue.enqueue(msg)
  }

  def dequeue(): Envelope = messageQueue.dequeue()

  // Execute the mailbox when it is scheduled
  // mini batch defined by throughput deadline
  @tailrec private final def processMailbox(
      left: Int = dispatcher.throughput.max(1)
  ): Unit = {
    logger.debug("mailbox size {}", messageQueue.numberOfMessages)
    val next = dequeue()
    if (next ne null) {
      logger.debug("processing message {}", next)
      actor.invoke(next)
      if (left > 1)
        processMailbox(left - 1)
    }
  }

  final def run(): Unit = {
    // In akka, it will process system messages first
    // To simplify, I haven't added system messages yet
    processMailbox()
    setAsIdle()
    dispatcher.registerForExecution(this)
  }

  override def exec(): Boolean = {
    run()
    // this is critical to tell forkjoinpool that the task is not completed.
    false
  }

  override def getRawResult: Unit = ()

  override def setRawResult(value: Unit): Unit = ()
}
