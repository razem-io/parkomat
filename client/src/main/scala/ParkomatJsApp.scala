import org.scalajs.dom
import org.scalajs.dom.raw._
import shared.models._
import upickle.default.{read, write}

import scala.scalajs.js
import scala.scalajs.js.Date

object ParkomatJsApp {


  def main(args: Array[String]): Unit = {
    connect()
  }

  def connect(): Unit = {
    val socket = new WebSocket(getWebsocketUri(dom.document))

    socket.onopen = { event: Event =>
      println(s"OnOpen -> $event")
      socket.send(write(ParkomatConnect))
    }

    socket.onerror = { event: ErrorEvent ⇒
      println(s"OnError -> $event")

      event
    }

    socket.onmessage = { event: MessageEvent ⇒
      println(s"OnMessage -> ${event.data.toString}")

      val webSocketMessage = read[WebSocketMessage](event.data.toString)

      matchWebSocketMessages(webSocketMessage)

      event
    }

    socket.onclose = { event: CloseEvent =>
      println(s"OnClose -> $event")
      reconnect()
      event
    }
  }

  def reconnect(): Unit = {
    println("Trying to reconnect!")
    js.timers.setTimeout(1000) {
      connect()
    }
  }

  def matchWebSocketMessages(webSocketMessage: WebSocketMessage): Unit = {
    webSocketMessage match {
      case m:ParkingStatus => refreshParkingSpots(m)
      case m => println(s"Unknown Message: $m")
    }
  }

  def refreshParkingSpots(parkingStatus: ParkingStatus): Unit = {
    val elStatus = dom.document.getElementById("status")
    val elStatusTime = dom.document.getElementById("statustime")

    val date = new Date(parkingStatus.lastUpdate)

    elStatus.innerHTML = parkingStatus.freeSpots + (if(parkingStatus.freeSpots != 1) " Parkplätze" else " Parkplatz") + " frei"
    elStatusTime.innerHTML = "Letztes Update: " + date.toLocaleTimeString() + " / " + date.toLocaleDateString()
  }

  def getWebsocketUri(document: Document): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${dom.document.location.host}/ws"
  }
}
