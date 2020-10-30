package littleakka

import java.util.concurrent.ForkJoinPool

import scala.concurrent.duration._

// This dispatcher is backed by a thread pool,
// pick an actor and assign it a dormant thread from it’s pool
// TODO: understand why fork-join over thread-pool?
class Dispatcher(val executorService: ForkJoinPool) {
  final val throughputDeadlineTime: Duration = 10.millis

  final val isThroughputDeadlineTimeDefined = throughputDeadlineTime.toMillis > 0

  final val throughput: Int = 10

  /**
    * Queue the message and schedule the mailbox for execution
    */
  def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    val mailbox = receiver.mailbox
    mailbox.enqueue(invocation)
    registerForExecution(mailbox)
  }

  /**
    * Suggest to register the provided mailbox for execution
    */
  def registerForExecution(mailbox: Mailbox): Unit = {
    if (mailbox.canBeScheduled) {
      if (mailbox.setAsScheduled()) {
        executorService.execute(mailbox)
      }
    }
  }
}
