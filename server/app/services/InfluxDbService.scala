package services

import java.time.Instant

import akka.actor.ActorSystem
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.{Database, InfluxDB, Point}
import com.google.inject.{Inject, Singleton}
import monix.execution.atomic.Atomic
import play.api.Configuration
import shared.models.ParkingStatus
import shared.models.api.LiveParkingSpotResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class InfluxDbService @Inject()(config: Configuration, system: ActorSystem) {
  implicit val ec: ExecutionContext = system.dispatcher

  val influxDb: InfluxDB = InfluxDB.connect(config.get[String]("influxdb.host"), config.get[Int]("influxdb.port"))
  val db: Database = influxDb.selectDatabase(config.get[String]("influxdb.database.name"))

  db.exists().onComplete {
    case Success(x) if !x =>
      db.create()
      db.createRetentionPolicy(name = "two_years", duration = "105w", replication = 1, default = true)
    case Failure(e) => println(e)
    case _ =>
  }

  private val a_lastResult: Atomic[Array[LiveParkingSpotResponse]] = Atomic(Array[LiveParkingSpotResponse]())

  def writeParkingSpotUpdate(data: Array[LiveParkingSpotResponse]): Future[Boolean] = {

    val now = Instant.now
    val nowEpoch = now.toEpochMilli

    val lastResult = a_lastResult.get()

    val points = data.flatMap(r => {
      val delay: Option[Long] = lastResult.find(_.parkingId == r.parkingId).flatMap(
        lastR => for {
          last <- lastR.lastUpdated
          curr <- r.lastUpdated
        } yield curr.toInstant.toEpochMilli - last.toInstant.toEpochMilli
      )

      delay.filter(_ != 0).map(d =>
        Point("ParkingSpot", nowEpoch)
        .addTag("parkingId", r.parkingId)
        .addTag("sensorId", r.sensorId)
        .addTag("user", r.user.getOrElse("Unknown"))
        .addField("batteryVoltage", r.batteryVoltage.getOrElse(0))
        .addField("lastUpdated", r.lastUpdated.map(_.toInstant).getOrElse(now.minusSeconds(3600)).toEpochMilli)
        .addField("delay", d)
        .addField("occupied", r.occupied.getOrElse(true))
        .addField("rssi", r.rssi.getOrElse(-200))
      )
    })

    a_lastResult.transform(_ => data)

    db.bulkWrite(points, retentionPolicy = "two_years", precision = Precision.MILLISECONDS)
  }

  def writeParkingStatusUpdate(data: ParkingStatus): Future[Boolean] = {
    val point = Point("ParkingStatus", Instant.now.toEpochMilli)
      .addField("freeLiftSpots", data.freeLiftSpots)
      .addField("freeNormalSpots", data.freeNormalSpots)

    db.write(point, retentionPolicy = "two_years", precision = Precision.MILLISECONDS)
  }
}
