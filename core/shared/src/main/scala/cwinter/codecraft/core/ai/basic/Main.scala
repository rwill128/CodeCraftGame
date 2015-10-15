package cwinter.codecraft.core.ai.basic

import cwinter.codecraft.core.api.{DroneController, Drone, DroneSpec, MineralCrystal}
import cwinter.codecraft.util.maths.{Rng, Vector2}


private[core] class Mothership extends DroneController {
  var t = 0
  var collectors = 0

  val collectorDroneSpec = new DroneSpec(storageModules = 2)
  val fastCollectorDroneSpec = new DroneSpec(storageModules = 1, engines = 1)
  val attackDroneSpec = new DroneSpec(missileBatteries = 2)

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(new DroneSpec(storageModules = 1), new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildDrone(if (Rng.bernoulli(0.9f)) collectorDroneSpec else fastCollectorDroneSpec, new ScoutingDroneController(this))
        collectors += 1
      } else {
        buildDrone(attackDroneSpec, new AttackDroneController())
      }
    }

    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).lengthSquared)
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

private[core] class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onDeath(): Unit = mothership.collectors -= 1

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    if (nextCrystal.isEmpty && mineralCrystal.size <= availableStorage) {
      moveTo(mineralCrystal.position)
      nextCrystal = Some(mineralCrystal)
    }
  }

  override def onTick(): Unit = {
    if (availableStorage == 0 && !hasReturned) {
      moveTo(mothership)
    } else if ((hasReturned && availableStorage > 0) || Rng.bernoulli(0.005) && nextCrystal == None) {
      hasReturned = false
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrivesAtPosition(): Unit = {
    if (availableStorage == 0) {
      giveMineralsTo(mothership)
      hasReturned = true
    } else {
      if (nextCrystal.map(_.harvested) == Some(true)) {
        nextCrystal = None
      }
      for (
        mineral <- nextCrystal
        if mineral.position ~ position
      ) {
        harvest(mineral)
        nextCrystal = None
      }
    }
  }

  override def onArrivesAtDrone(drone: Drone): Unit = {
    giveMineralsTo(drone)
    hasReturned = true
  }

  override def onDroneEntersVision(drone: Drone): Unit = ()
}

private[core] class AttackDroneController extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = ()

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  override def onTick(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).lengthSquared)
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
      moveInDirection(enemy.position - position)
    } else if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  override def onArrivesAtPosition(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

