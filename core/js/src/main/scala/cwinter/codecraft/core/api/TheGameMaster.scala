package cwinter.codecraft.core.api

import cwinter.codecraft.core.DroneWorldSimulator
import cwinter.codecraft.graphics.engine.{Debug, Renderer}
import cwinter.codecraft.graphics.model.TheModelCache
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.Rectangle
import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object TheGameMaster extends GameMasterLike {
  var canvas: html.Canvas = null
  var currentSimulator: DroneWorldSimulator = null
  private[this] var intervalID: Option[Int] = None


  def runWithAscii(simulator: DroneWorldSimulator): Unit = {
    currentSimulator = simulator

    println("Starting simulator...")
    dom.setInterval(() => {
      if (render != null) {
        render(simulator.worldState, simulator.map.size)
      }
      println("t = " + simulator.timestep)
      println("object count: " + simulator.worldState.length)
      simulator.run(1)
    }, 30)
    println("Success")
  }

  def run(simulator: DroneWorldSimulator): Unit = {
    currentSimulator = simulator

    require(canvas != null, "Must first set TheGameMaster.canvas variable to the webgl canvas element.")
    require(intervalID.isEmpty, "Can only run one CodeCraft game at a time.")
    val renderer = new Renderer(canvas, simulator, simulator.map.initialDrones.head.position)
    intervalID = Some(dom.setInterval(() => {
      renderer.render()
      simulator.run(1)
    }, 15))
    canvas.setAttribute("interval-id", intervalID.toString)
  }

  def stop(): Unit = {
    dom.clearInterval(intervalID.get)
    TheModelCache.clear()
    Debug.clearDrawAlways()
  }

  private[codecraft] var render: (Seq[WorldObjectDescriptor], Rectangle) => Unit = null
}

