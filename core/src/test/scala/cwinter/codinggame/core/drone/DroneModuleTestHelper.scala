package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SimulatorEvent
import cwinter.codinggame.util.maths.Vector2

object DroneModuleTestHelper {
  def multipleUpdates(module: DroneModule, count: Int): (Seq[SimulatorEvent], Int, Seq[Vector2]) = {
    val allUpdates = for {
      _ <- 0 until count
    } yield module.update(0)

    allUpdates.foldLeft((Seq.empty[SimulatorEvent], 0, Seq.empty[Vector2])){
      case ((es1, r1, rs1), (es2, r2, rs2)) => (es1 ++ es2, r1 + r2, rs1 ++ rs2)
    }
  }
}
