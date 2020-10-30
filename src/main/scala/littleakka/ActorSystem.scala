package littleakka

import java.util.concurrent.{ForkJoinPool, TimeUnit}

import scala.reflect.ClassTag

// TODO: Props

class ActorSystem(
    val dispatcher: Dispatcher = new Dispatcher(
      new ForkJoinPool(Runtime.getRuntime.availableProcessors)
    )
) {
  def awaitTermination(value: Int, unit: TimeUnit): Boolean = {
    dispatcher.executorService.awaitTermination(value, unit)
  }

  def actorOf[T <: Actor: ClassTag](clazz: Class[T]): ActorRef = {
    new LocalActorRef(clazz, dispatcher)
  }
}
