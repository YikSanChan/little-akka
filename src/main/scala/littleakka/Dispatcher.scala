package littleakka

import java.util.concurrent.ForkJoinPool

import scala.concurrent.duration._

// This dispatcher is backed by a thread pool,
// pick an actor and assign it a dormant thread from it’s pool
class Dispatcher(val executorService: ForkJoinPool) {

  // Throughput defines the number of messages that are processed in a batch
  // before the thread is returned to the pool. Set to 1 for as fair as possible.
  final val throughput: Int = 10

  /** Queue the message and schedule the mailbox for execution
    */
  def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    val mailbox = receiver.mailbox()
    mailbox.enqueue(invocation)
    registerForExecution(mailbox)
  }

  /** Suggest to register the provided mailbox for execution
    */
  def registerForExecution(mailbox: Mailbox): Unit = {
    if (mailbox.canBeScheduled) {
      if (mailbox.setAsScheduled()) {
        executorService.execute(mailbox)
      }
    }
  }
}
