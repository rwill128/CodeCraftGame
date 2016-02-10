package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.{ReplicatorCommand, ReplicatorBattleCoordinator}
import cwinter.codecraft.core.ai.shared.{HarvestCoordinatorWithZones, SharedContext}


class ReplicatorContext extends SharedContext[ReplicatorCommand] {
  val battleCoordinator = new ReplicatorBattleCoordinator
  val mothershipCoordinator = new MothershipCoordinator
  val harvestCoordinator = new HarvestCoordinatorWithZones
  var isReplicatorInConstruction: Boolean = false
}

