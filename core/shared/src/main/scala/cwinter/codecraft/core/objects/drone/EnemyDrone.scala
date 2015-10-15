package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{Player, ObjectNotVisibleException, Drone, DroneSpec}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.util.maths.Vector2

/**
 * Wrapper around drone class to allow users to query a subset of properties of enemy drones.
 */
private[core] class EnemyDrone(
  private[core] val drone: DroneImpl,
  private val holder: Player // the player to whom the handle is given
) extends Drone {
  private[this] var _lastKnownPosition: Vector2 = drone.position // TODO: implement


  override def position: Vector2 = {
    if (isVisible) {
      drone.position
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the position of an enemy drone that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  override def weaponsCooldown: Int = {
    if (isVisible) {
      drone.weaponsCooldown
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the weapons cooldown of an enemy that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  override def hitpoints: Int = {
    if (isVisible) {
      drone.hitpoints
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the hitpoints of an enemy drone that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  def totalAvailableResources: Int =
   if (isVisible) {
     drone.totalAvailableResources
   } else {
     val exception = new ObjectNotVisibleException(
       "Cannot get the totalAvailableResources of an enemy drone that is not inside the sight radius of any of your drones.")
     Errors.error(exception, drone.position)
   }

  override def isEnemy: Boolean = true

  def isVisible: Boolean = {
    drone.player == holder ||
    drone.objectsInSight.exists {
      case drone: DroneImpl => drone.player == holder
      case _ => false
    }
  }

  override def spec: DroneSpec = drone.spec
  override def playerID: Int = drone.player.id
}
