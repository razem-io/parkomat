package services

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Singleton}
import model.response.ParkomatFreeAllResponse
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

  private val a_lastResult: Atomic[ParkomatFreeAllResponse] = Atomic(ParkomatFreeAllResponse.empty())

  private val a_lastStatus: Atomic[ParkingStatus] = Atomic(ParkingStatus(0,0))

  val backendUrl: String = config.get[String]("parkomat.backend.url")

  actorSystem.scheduler.schedule(3.seconds, 30.seconds) {
    logger.info("current: " + a_lastResult)
    ws.url(backendUrl).get()
      .map(_.body)
      .map(Json.parse(_).as[ParkomatFreeAllResponse])
      .map(r => a_lastResult.transformAndGet(_ => r))
      .map(r => a_lastStatus.transformAndGet(_ => responseToStatus(r)))
      .foreach(notifyActors)
  }

  private def notifyActors(parkingStatus: ParkingStatus): Unit = {
    actorsToNotify.get.foreach(a => a ! parkingStatus)
  }

  def registerActorToNotify(actorRef: ActorRef): Unit = {
    actorsToNotify.getAndTransform(old => old :+ actorRef)
  }

  def lastResult: ParkomatFreeAllResponse = a_lastResult.get
  def lastStatus: ParkingStatus = a_lastStatus.get
}

object ParkomatService {
  def responseToStatus(response: ParkomatFreeAllResponse): ParkingStatus = {
    ParkingStatus(response.timestamp.toInstant.toEpochMilli, response.parkingSpots.size)
  }
}
