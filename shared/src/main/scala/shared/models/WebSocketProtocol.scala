package shared.models

sealed trait WebSocketMessage
case object ParkomatConnect extends WebSocketMessage
case class ParkingStatus(lastUpdate: Long, freeNormalSpots: Int, freeLiftSpots: Int) extends WebSocketMessage