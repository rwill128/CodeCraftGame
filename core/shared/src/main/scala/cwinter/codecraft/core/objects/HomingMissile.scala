package cwinter.codecraft.core.objects

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.GameConstants.{MissileLifetime, MissileSpeed}
import cwinter.codecraft.core.api.{GameConstants, Player}
import cwinter.codecraft.core.graphics.{HomingMissileModel, BasicHomingMissileModel}
import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.graphics.engine.{NullPositionDescriptor, ModelDescriptor}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Vector2

private[core] class HomingMissile(
  val player: Player,
  initialPos: Vector2,
  val id: Int,
  time: Double,
  target: DroneImpl
) extends WorldObject {
  val dynamics: MissileDynamics =
    new MissileDynamics(MissileSpeed, target.dynamics, player.id, this, initialPos, time)
  val previousPositions = collection.mutable.Queue(initialPos)
  val positions = 7
  var lifetime = MissileLifetime
  var fading: Boolean = false

  def update(): Seq[SimulatorEvent] = {
    if (fading) fade()
    else updatePosition()
  }

  private def fade() = {
    previousPositions.dequeue()
    if (previousPositions.isEmpty) Seq(HomingMissileFaded(this))
    else Seq.empty[SimulatorEvent]
  }

  private def updatePosition() = {
    dynamics.update()
    lifetime -= 1

    if (isAnimated) recordPosition()

    checkForRemoval
  }

  private def recordPosition() = {
    previousPositions.enqueue(position)
    if (previousPositions.length > positions) previousPositions.dequeue()
    while (previousPositions.length > lifetime + 1) previousPositions.dequeue()
  }

  private def checkForRemoval = {
    if (dynamics.removed) {
      fading = true
      Seq(MissileExplodes(this))
    } else if (lifetime == 0) {
      dynamics.remove()
      Seq(HomingMissileFaded(this))
    } else Seq.empty[SimulatorEvent]
  }

  def isAnimated = target.context.settings.allowMissileAnimation

  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: Seq[ModelDescriptor[_]] = Seq(
    ModelDescriptor(
      NullPositionDescriptor,
      modelDescriptor
    )
  )

  private def modelDescriptor =
    if (isAnimated) fancyModelDescriptor
    else basicModelDescriptor

  private def fancyModelDescriptor =
    HomingMissileModel(
      previousPositions.map{case Vector2(x, y) => (x.toFloat, y.toFloat)},
      math.min(MissileLifetime - lifetime, positions),
      player.color
    )

  private def basicModelDescriptor =
    BasicHomingMissileModel(position.x.toFloat, position.y.toFloat, player.color)

  override private[core] def isDead = lifetime <= 0
}

