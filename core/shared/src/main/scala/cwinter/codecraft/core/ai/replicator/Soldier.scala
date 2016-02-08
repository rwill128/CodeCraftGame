package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat._
import cwinter.codecraft.util.maths.{Vector2, Rng}


class Soldier(ctx: ReplicatorContext) extends ReplicatorBase('Soldier, ctx) {
  private[this] var _mission: Option[Mission] = None


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.battleCoordinator.online(this)
  }

  override def onTick(): Unit = {
    handleWeapons()

    if (enemies.nonEmpty) {
      val closest = closestEnemy
      moveInDirection(closest.position - position)
      if (closest.spec.missileBatteries > 0)
        context.battleCoordinator.requestAssistance(this)
    } else _mission.foreach(executeInstructions)
  }

  def executeInstructions(mission: Mission): Unit = mission.missionInstructions match {
    case Scout =>
      scout()
      if (searchToken.isEmpty && Rng.bernoulli(0.005)) {
        moveTo(0.9 * Rng.vector2(worldSize))
      }
    case Attack(enemy, origin) =>
      moveTo(enemy.lastKnownPosition)
      if (enemy.lastKnownPosition ~ position) origin.notFound()
    case Search(position, radius) =>
      if (!isMoving) {
        moveTo(position + radius * Vector2(2 * math.Pi * context.rng.nextDouble()))
      }
    case AttackMove(position) =>
      moveTo(position)
  }

  def startMission(mission: Mission): Unit = {
    mission.assign(this)
    _mission = Some(mission)
  }

  def abortMission(): Unit = {
    for (m <- _mission)
      m.relieve(this)
    _mission = None
  }

  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.offline(this)
    abortMission()
  }

  def isIdle: Boolean = _mission.isEmpty

  def missionPriority: Int = _mission.map(_.priority).getOrElse(-1)
}

