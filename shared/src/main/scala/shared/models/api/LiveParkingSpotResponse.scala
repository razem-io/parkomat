package shared.models.api

import java.time.{Instant, LocalDateTime, ZonedDateTime}

/**
  * {
  * "parkingId": "5",
  * "sensorId": "28927271",
  * "user": null,
  * "startTime": null,
  * "endTime": null,
  * "lastUpdated": "2019-02-11T11:16:59.399+0000",
  * "occupied": true,
  * "reserved": false,
  * "batteryVoltage": 3700,
  * "rssi": -95,
  * "reservationToken": null
  * }
  */

case class LiveParkingSpotResponse(
                                    parkingId: String,
                                    sensorId: String,
                                    user: Option[String],
                                    startTime: Option[ZonedDateTime],
                                    endTime: Option[ZonedDateTime],
                                    lastUpdated: Option[ZonedDateTime],
                                    occupied: Option[Boolean],
                                    reserved: Option[Boolean],
                                    batteryVoltage: Option[Int],
                                    rssi: Option[Int],
                                    reservationToken: Option[String]
)
