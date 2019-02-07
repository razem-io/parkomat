package services

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Singleton}
import model.response.ParkomatStatsFreeSpotsResponse
import monix.execution.atomic.Atomic
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import shared.models.ParkingStatus

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

@Singleton
class ParkomatService @Inject()(config: Configuration, actorSystem: ActorSystem, ws: WSClient) {
  import ParkomatService._

  private implicit val dispatcherEC: ExecutionContextExecutor = actorSystem.dispatcher

  private val logger: Logger = Logger(this.getClass)

  private val actorsToNotify: Atomic[Seq[ActorRef]] = Atomic(Seq.empty[ActorRef])

  private val a_lastResult: Atomic[ParkomatStatsFreeSpotsResponse] = Atomic(ParkomatStatsFreeSpotsResponse.empty())

  private val a_lastStatus: Atomic[ParkingStatus] = Atomic(ParkingStatus(0,0))

  val backendUrl: String = config.get[String]("parkomat.backend.url")

  actorSystem.scheduler.schedule(3.seconds, 30.seconds) {
    logger.info("current: " + a_lastResult)
    val result = ws.url(backendUrl).get()
      .map(_.body)
      .map(Json.parse(_).as[ParkomatStatsFreeSpotsResponse])
      .map(r => a_lastResult.transformAndGet(_ => r))
      .map(r => a_lastStatus.transformAndGet(_ => responseToStatus(r)))

    result.failed.foreach(ex => logger.error("", ex))
    result.foreach(notifyActors)
  }

  private def notifyActors(parkingStatus: ParkingStatus): Unit = {
    actorsToNotify.get.foreach(a => a ! parkingStatus)
  }

  def registerActorToNotify(actorRef: ActorRef): Unit = {
    actorsToNotify.getAndTransform(old => old :+ actorRef)
  }

  def lastResult: ParkomatStatsFreeSpotsResponse = a_lastResult.get
  def lastStatus: ParkingStatus = a_lastStatus.get
}

object ParkomatService {
  def responseToStatus(response: ParkomatStatsFreeSpotsResponse): ParkingStatus = {
    ParkingStatus(response.timestamp.toEpochMilli, response.freeNormalPlaces.size + response.freeLiftPlaces.size)
  }
}
