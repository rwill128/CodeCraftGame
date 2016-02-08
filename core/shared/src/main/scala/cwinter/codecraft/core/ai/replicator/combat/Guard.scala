package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.api.Drone


class Guard(
  val friend: Drone,
  required: Int
) extends Mission {
  val minRequired = 1
  val priority = 10
  var maxRequired = required
  var timeout = 0
  resetTimeout()


  def missionInstructions = Circle(friend.position, 500)
  def hasExpired = maxRequired == 0 || friend.isDead
  override def update(): Unit = {
    timeout -= 1
    if (timeout == 0) {
      maxRequired -= 1
      reduceAssignedToMax()
      resetTimeout()
    }
  }

  private def resetTimeout(): Unit = timeout = 450

  def refresh(required: Int): Unit = {
    if (required > maxRequired) maxRequired = required
    else if (required + 1 >= maxRequired) resetTimeout()
  }
}

