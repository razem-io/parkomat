package shared.models

import java.time.{Instant, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatterBuilder

sealed trait WebSocketMessage
case object ParkomatConnect extends WebSocketMessage

case class ParkingStatus(lastUpdate: Long, freeNormalSpots: Int, freeLiftSpots: Int, statusText: String, statusDetailText: String, statusUpdateText: String) extends WebSocketMessage {

}

object ParkingStatus {
  private val dateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss").appendLiteral(" / ")
    .appendPattern("dd.MM.yyyy")
    .toFormatter()

  def apply(lastUpdate: Instant, freeNormalSpots: Int, freeLiftSpots: Int): ParkingStatus = {
    val freeSpots: Int = freeLiftSpots + freeNormalSpots

    new ParkingStatus(
      lastUpdate = lastUpdate.toEpochMilli,
      freeNormalSpots = freeNormalSpots,
      freeLiftSpots = freeLiftSpots,
      statusText = freeSpots + (if(freeSpots != 1) " Parkplätze" else " Parkplatz") + " frei",
      statusDetailText = freeNormalSpots + (if(freeSpots != 1) " normale" else " normaler") + " und " + (if(freeLiftSpots == 1) "einer" else freeLiftSpots) + " auf der Hebebühne",
      statusUpdateText = "Letztes Update: " + dateTimeFormatter.format(lastUpdate.atZone(ZoneId.of("Europe/Berlin")))
    )
  }
}