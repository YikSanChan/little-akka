package viktorklang

object Actor {

  import java.util.concurrent.{ConcurrentLinkedQueue, Executor}
  import java.util.concurrent.atomic.AtomicInteger

  type Behavior = Any => Effect

  sealed trait Effect extends (Behavior => Behavior)

  case object Stay extends Effect {
    def apply(old: Behavior): Behavior = old
  }

  case class Become(like: Behavior) extends Effect {
    def apply(old: Behavior): Behavior = like
  }

  // Stay Dead plz
  final val Die = Become(msg => {
    println("Dropping msg [" + msg + "] due to severe case of death.")
    Stay
  })

  // The notion of an Address to where you can post messages to
  // TODO: maybe ActorRef?
  trait Address {
    def !(msg: Any): Unit
  }

  private abstract class AtomicRunnableAddress extends Address with Runnable {
    val on = new AtomicInteger(0)
  }

  // Seeded by the self-reference that yields the initial behavior
  def apply(initial: Address => Behavior)(implicit e: Executor): Address =
  // Memory visibility of "behavior" is guarded by "on" using volatile piggybacking
    new AtomicRunnableAddress {

      // Our awesome little mailbox, free of blocking and evil
      private final val mbox = new ConcurrentLinkedQueue[Any]

      // Rebindable top of the mailbox, bootstrapped to identity
      private var behavior: Behavior = {
        case self: Address => Become(initial(self))
      }

      // As an optimization, we peek at our threads local copy of our behavior to see if we should bail out early
      final override def !(msg: Any): Unit = behavior match {

        // Efficiently bail out if we're _known_ to be dead
        case dead @ Die.`like` =>
          dead(msg)

        // Enqueue the message onto the mailbox and try to schedule for execution
        case _ =>
          mbox.offer(msg)
          async()
      }

      // Switch ourselves off, and then see if we should be rescheduled for execution
      final override def run(): Unit = try {
        if (on.get == 1) {
          behavior = behavior(mbox.poll())(behavior)
        }
      } finally {
        on.set(0);
        async()
      }

      // Schedule to run on the Executor and back out on failure
      private def async(): Unit = {
        // If there's something to process, and we're not already scheduled
        if (!mbox.isEmpty && on.compareAndSet(0, 1)) {
          try e.execute(this) catch {
            case t => on.set(0); throw t
          }
        }
      }
    } match {
      case a: Address =>
        a ! a
        a
    } // Make the actor self aware by seeding its address to the initial behavior
}
