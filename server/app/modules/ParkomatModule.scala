package modules

import com.google.inject.AbstractModule
import services.{InfluxDbService, ParkomatService}

class ParkomatModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[InfluxDbService]).asEagerSingleton()
    bind(classOf[ParkomatService]).asEagerSingleton()
  }
}
