package services

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Singleton}
import monix.execution.atomic.Atomic
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import shared.models.ParkingStatus
import shared.models.api.LiveParkingSpotResponse

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

@Singleton
class ParkomatService @Inject()(config: Configuration, actorSystem: ActorSystem, ws: WSClient) {
  import ParkomatService._

  private implicit val dispatcherEC: ExecutionContextExecutor = actorSystem.dispatcher

  private val logger: Logger = Logger(this.getClass)

  private val actorsToNotify: Atomic[Seq[ActorRef]] = Atomic(Seq.empty[ActorRef])

  private val a_lastResult: Atomic[Array[LiveParkingSpotResponse]] = Atomic(Array[LiveParkingSpotResponse]())

  private val a_lastStatus: Atomic[ParkingStatus] = Atomic(ParkingStatus(0,0,0))

  val backendUrl: String = config.get[String]("parkomat.backend.url")


  // 2019-02-11T11:16:59.399+0000
  private val dateTimeFormatter = new DateTimeFormatterBuilder()
    // here is the same as your code
    .appendPattern("yyyy-MM-dd").appendLiteral('T')
    // time (hour/minute/seconds)
    .appendPattern("HH:mm:ss.SSS")
    // offset
    .appendOffset("+HHmm", "Z")
    // create formatter
    .toFormatter()


  import play.api.libs.functional.syntax._
  import play.api.libs.json._
  implicit val liveParkingSpotResponseReads: Reads[LiveParkingSpotResponse] = (
    (__ \ "parkingId").read[String] and
    (__ \ "sensorId").read[String] and
    (__ \ "user").readNullable[String] and
    (__ \ "startTime").readNullable[String].map(_.map(s => ZonedDateTime.parse(s, dateTimeFormatter))) and
    (__ \ "endTime").readNullable[String].map(_.map(s => ZonedDateTime.parse(s, dateTimeFormatter))) and
    (__ \ "lastUpdated").readNullable[String].map(_.map(s => ZonedDateTime.parse(s, dateTimeFormatter))) and
    (__ \ "occupied").readNullable[Boolean] and
    (__ \ "reserved").readNullable[Boolean] and
    (__ \ "batteryVoltage").readNullable[Int] and
    (__ \ "rssi").readNullable[Int] and
    (__ \ "reservationToken").readNullable[String]
    )(LiveParkingSpotResponse.apply _)

  actorSystem.scheduler.schedule(3.seconds, 30.seconds) {
    logger.info("current: " + a_lastResult)
    val result: Future[Option[ParkingStatus]] = ws.url(backendUrl).get()
      .map(_.body)
      .map(Json.parse(_).as[Array[LiveParkingSpotResponse]])
      .map(r => a_lastResult.transformAndGet(_ => r))
      .map(r => responseToStatus(r).map(s => a_lastStatus.transformAndGet(_ => s)))

    result.failed.foreach(ex => logger.error("", ex))
    result.foreach(_.foreach(notifyActors))
  }

  private def notifyActors(parkingStatus: ParkingStatus): Unit = {
    actorsToNotify.get.foreach(a => a ! parkingStatus)
  }

  def registerActorToNotify(actorRef: ActorRef): Unit = {
    actorsToNotify.getAndTransform(old => old :+ actorRef)
  }

  def lastResult: Array[LiveParkingSpotResponse] = a_lastResult.get
  def lastStatus: ParkingStatus = a_lastStatus.get
}

object ParkomatService {
  val liftSpaces: Seq[String] = (1 to 6).map(_.toString)
  def responseToStatus(responses: Array[LiveParkingSpotResponse]): Option[ParkingStatus] = responses.seq
    .flatMap(_.lastUpdated)
    .sortBy(_.toEpochSecond)
    .lastOption.map(t => {
      ParkingStatus(
        lastUpdate = t.toInstant.toEpochMilli,
        freeNormalSpots = responses.seq.filter(_.occupied.exists(_ == false)).count(r => !liftSpaces.contains(r.parkingId)),
        freeLiftSpots = responses.seq.filter(_.occupied.exists(_ == false)).count(r => liftSpaces.contains(r.parkingId))
      )
    })
}
