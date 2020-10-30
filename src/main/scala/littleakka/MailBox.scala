package littleakka

import java.util.concurrent.{ConcurrentLinkedQueue, ForkJoinTask}

import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec

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

  private var idle = true

  /** Using synchronized to simplify things. In the real Akka actors code,
    * it's highly optimized by using atomic compare and swap instruction
    */
  def setAsScheduled(): Boolean = {
    this.synchronized {
      if (idle) {
        idle = false
        true
      } else {
        false
      }
    }
  }

  def setAsIdle(): Boolean = {
    this.synchronized {
      if (!idle) {
        idle = true
        true
      } else {
        false
      }
    }
  }

  def canBeScheduled: Boolean = {
    messageQueue.hasMessages
  }

  var actor: ActorCell = _

  def setActor(cell: ActorCell): Unit = actor = cell

  def dispatcher: Dispatcher = actor.dispatcher

  val shouldProcessMessage: Boolean = true

  def enqueue(msg: Envelope): Unit =
    messageQueue.enqueue(msg)

  def dequeue(): Envelope = messageQueue.dequeue()

  // Execute the mailbox when it is scheduled
  // mini batch defined by throughput deadline
  @tailrec private final def processMailbox(
      left: Int = dispatcher.throughput.max(1),
      deadlineNs: Long =
        if (dispatcher.isThroughputDeadlineTimeDefined)
          System.nanoTime + dispatcher.throughputDeadlineTime.toNanos
        else 0L
  ): Unit =
    if (shouldProcessMessage) {
      val next = dequeue()
      if (next ne null) {
        logger.debug("processing message {}", next)
        actor.invoke(next)

        if (Thread.interrupted())
          throw new InterruptedException(
            "Interrupted while processing actor messages"
          )

        if (
          (left > 1) && (!dispatcher.isThroughputDeadlineTimeDefined || (System.nanoTime - deadlineNs) < 0)
        )
          processMailbox(left - 1, deadlineNs)
      }
    }

  final def run(): Unit = {
    processMailbox()
  }

  override def exec(): Boolean = {
    try {
      run()
      // this is critical to tell forkjoinpool that the task is not completed.
      false
    } finally {
      setAsIdle()
      dispatcher.registerForExecution(this)
    }
  }

  override def getRawResult: Unit = ()

  override def setRawResult(value: Unit): Unit = ()
}

// TODO: sender ActorRef

final case class Envelope(message: Any)
