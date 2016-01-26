package cwinter.codecraft.core.api

import java.io.File

import cwinter.codecraft.core.multiplayer.{JavaXWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.{DroneWorldSimulator, MultiplayerClientConfig, MultiplayerConfig, WorldMap}
import cwinter.codecraft.graphics.application.DrawingCanvas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * Main entry point to start the game.
 */
object TheGameMaster extends GameMasterLike {
  override def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    DrawingCanvas.run(simulator)
    simulator
  }

  def runReplayFromFile(filepath: String): DroneWorldSimulator = {
    val simulator = createReplaySimulator(scala.io.Source.fromFile(filepath).mkString)
    run(simulator)
  }

  def runLastReplay(): DroneWorldSimulator = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplayFromFile(latest.getPath)
  }


  def prepareMultiplayerGame(serverAddress: String): Future[(WorldMap, MultiplayerConfig)] = {
    val websocketConnection = new JavaXWebsocketClient(s"ws://$serverAddress:8080")
    val serverConnection = new WebsocketServerConnection(websocketConnection)
    val sync = serverConnection.receiveInitialWorldState()

    // TODO: receive this information from server
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)

    sync.map(sync => {
      val map = sync.worldMap
      val connection = MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
      (map, connection)
    })
  }
}

