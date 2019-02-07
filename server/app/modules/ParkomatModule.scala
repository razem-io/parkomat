package modules

import com.google.inject.AbstractModule
import services.ParkomatService

class ParkomatModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ParkomatService]).asEagerSingleton()
  }
}
