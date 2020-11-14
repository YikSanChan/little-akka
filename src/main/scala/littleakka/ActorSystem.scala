package littleakka

import java.util.concurrent.{ForkJoinPool, TimeUnit}

import scala.reflect.ClassTag

class ActorSystem(
    val dispatcher: Dispatcher = new Dispatcher(new ForkJoinPool())
) {

  def awaitTermination(value: Int, unit: TimeUnit): Boolean = {
    dispatcher.executorService.awaitTermination(value, unit)
  }

  def actorOf[T <: Actor: ClassTag](clazz: Class[T], name: String): ActorRef = {
    new LocalActorRef(clazz, name, dispatcher)
  }
}
