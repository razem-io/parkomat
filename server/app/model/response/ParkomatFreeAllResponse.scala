package model.response

import java.time.{ZoneId, ZonedDateTime}

import shared.models.ParkingSpot

case class ParkomatFreeAllResponse(timeArray: Seq[Long], parkingSpots: Seq[ParkingSpot]) {
  val isEmpty: Boolean = timeArray.isEmpty && parkingSpots.isEmpty

  val zone: ZoneId = ZoneId.of("UTC")

  def timestamp: ZonedDateTime = ZonedDateTime.of(
    timeArray.head.toInt,
    timeArray(1).toInt,
    timeArray(2).toInt,
    timeArray(3).toInt,
    timeArray(4). toInt,
    timeArray(5).toInt,
    0,
    zone
  )
}


object ParkomatFreeAllResponse {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val parkingSpotReads: Reads[ParkingSpot] = Json.reads[ParkingSpot]

  implicit val parkomatFreeAllResponseReads: Reads[ParkomatFreeAllResponse] = (
    (__ \ "last-updated").read[Seq[Long]] and
    (__ \ "parking-spots").read[Seq[ParkingSpot]]
  )(ParkomatFreeAllResponse.apply _)

  def empty(): ParkomatFreeAllResponse = ParkomatFreeAllResponse(Nil, Nil)
}