package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.ParkomatService

@Singleton
class Application @Inject()(cc: ControllerComponents, parkomatService: ParkomatService)
                           (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  def index = Action { implicit request =>
    parkomatService.lastStatus.statusText

    Ok(views.html.index(parkomatService.lastStatus))
  }

  def ws(): WebSocket = WebSocket.accept[String, String] { implicit request =>
    ActorFlow.actorRef(out => ParkomatWebSocketActor.props(out, parkomatService))
  }

}
