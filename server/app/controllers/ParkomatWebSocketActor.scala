package controllers

import akka.actor._
import play.api.Logger
import services.ParkomatService
import shared.models.{ParkingStatus, ParkomatConnect, WebSocketMessage}
import upickle.default._

object ParkomatWebSocketActor {
  def props(out: ActorRef, parkomatService: ParkomatService) = Props(new ParkomatWebSocketActor(out, parkomatService))
}

class ParkomatWebSocketActor(out: ActorRef, parkomatService: ParkomatService) extends Actor {
  parkomatService.registerActorToNotify(self)

  def receive: PartialFunction[Any, Unit] = {
    case msg: WebSocketMessage =>
      out ! write(msg)
    case msg: String =>
      val m = read[WebSocketMessage](msg.toString)

      m match {
        case ParkomatConnect =>
          Logger.debug("Client connected: " + out)
          out ! write(parkomatService.lastStatus)
        case s: ParkingStatus =>
          out ! write(s)
        case unknownMsg => Logger.error("Unknown message: " + unknownMsg)
      }
  }

  override def postStop: Unit = {
    Logger.debug(s"Removed it -> $out")
  }

}
