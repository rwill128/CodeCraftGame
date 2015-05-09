package cwinter.codinggame.testai

import cwinter.codinggame.core._
import cwinter.codinggame.core.drone._
import cwinter.codinggame.util.maths.{Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.worldstate.BluePlayer

object Main {
  def main(args: Array[String]): Unit = {
    TheGameMaster.runLevel1(new Mothership)
  }
}


class Mothership extends DroneController {
  var t = 0
  var collectors = 0
  var minerals = Set.empty[MineralCrystal]

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildTinyDrone(StorageModule, new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController(this))
        collectors += 1
      } else {
        if (Rng.bernoulli(0.7)) {
          buildMediumDrone(Lasers, Lasers, Lasers, Engines, new AttackDroneController(this))
        } else {
          buildLargeDrone(ShieldGenerator, Engines, Engines, Lasers, Lasers, Lasers, Lasers, new AttackDroneController(this))
        }
      }
    } else {
      for (mineralCrystal <- storedMinerals) {
        if (availableFactories >= mineralCrystal.size) {
          processMineral(mineralCrystal)
        }
      }
    }

    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.head
      shootWeapons(enemy)
    }
  }

  def findClosestMineral(maxSize: Int, position: Vector2): Option[MineralCrystal] = {
    minerals = minerals.filter(!_.harvested)
    val filtered = minerals.filter(_.size <= maxSize)
    if (filtered.isEmpty) None
    else Some(filtered.minBy(m => (m.position - position).magnitudeSquared))
  }

  def registerMineral(mineralCrystal: MineralCrystal): Unit = {
    minerals += mineralCrystal
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.player != drone.player)


  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrival(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onDeath(): Unit = mothership.collectors -= 1

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    mothership.registerMineral(mineralCrystal)
  }

  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None

    if (nextCrystal == None) nextCrystal = mothership.findClosestMineral(availableStorage, position)

    for (
      c <- nextCrystal
      if !(c.position ~ position)
    ) moveToPosition(c.position)

    if (availableStorage == 0 && !hasReturned) {
      moveToDrone(mothership)
      nextCrystal = None
    } else if ((hasReturned && availableStorage > 0) || Rng.bernoulli(0.005) && nextCrystal == None) {
      hasReturned = false
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrival(): Unit = {
    if (availableStorage == 0) {
      depositMineralCrystals(mothership)
      hasReturned = true
    } else {
      if (nextCrystal.map(_.harvested) == Some(true)) {
        nextCrystal = None
      }
      for (
        mineral <- nextCrystal
        if mineral.position ~ position
      ) {
        harvestMineral(mineral)
        nextCrystal = None
      }
    }
  }

  override def onDroneEntersVision(drone: Drone): Unit = ()
}

class AttackDroneController(val mothership: Mothership) extends DroneController {
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit =
    mothership.registerMineral(mineralCrystal)

  override def onTick(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.head
      shootWeapons(enemy)
      moveInDirection(enemy.position - position)
    }
    if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.player != drone.player)

  override def onArrival(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

