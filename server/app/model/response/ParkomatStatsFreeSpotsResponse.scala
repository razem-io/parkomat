package model.response

import java.time.format.DateTimeFormatter
import java.time._

import shared.models.ParkingSpot

case class ParkomatStatsFreeSpotsResponse(timestamp: Instant, freeLiftPlaces: Seq[String], freeNormalPlaces: Seq[String]) {
  val isEmpty: Boolean = freeLiftPlaces.isEmpty && freeNormalPlaces.isEmpty

  val zone: ZoneId = ZoneId.of("UTC")
}


object ParkomatStatsFreeSpotsResponse {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val parkingSpotReads: Reads[ParkingSpot] = Json.reads[ParkingSpot]

  implicit val parkomatFreeAllResponseReads: Reads[ParkomatStatsFreeSpotsResponse] = (
    (__ \ "lastUpdated").read[String].map(_.split(":", 2).last.trim).map(s => LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME)).map(_.toInstant(ZoneOffset.UTC)) and
    (__ \ "freeLiftPlaces").read[Seq[String]] and
    (__ \ "freeNormalPlaces").read[Seq[String]]
  )(ParkomatStatsFreeSpotsResponse.apply _)

  def empty(): ParkomatStatsFreeSpotsResponse = ParkomatStatsFreeSpotsResponse(Instant.now, Nil, Nil)
}