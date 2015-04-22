package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import cwinter.graphics.model.Geometry
import cwinter.worldstate.{DroneDescriptor, WorldObjectDescriptor}


private[core] class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: DroneController,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject {

  val dynamics: DroneDynamics = new DroneDynamics(100, radius, initialPos, time)
  val storageCapacity = modules.count(_ == StorageModule)
  val nLasers = modules.count(_ == Lasers)
  val factoryCapacity = modules.count(_ == NanobotFactory)

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var storedMinerals = List.empty[MineralCrystal]
  private[this] var storedEnergyGlobes: Int = startingResources

  private[this] var movementCommand: MovementCommand = HoldPosition
  private[this] var droneConstructions = List.empty[(ConstructDrone, Int)]

  def processEvents(): Unit = {
    movementCommand match {
      case MoveToPosition(position) =>
        if (position ~ this.position) {
          movementCommand = HoldPosition
          enqueueEvent(ArrivedAtPosition)
          dynamics.halt()
        }
      case _ => // don't care
    }

    eventQueue foreach {
      case Spawned => controller.onSpawn()
      case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
      case ArrivedAtPosition => controller.onArrival()
      case event => throw new Exception(s"Unhandled event! $event")
    }
    eventQueue.clear()
    controller.onTick()
  }

  def processCommands(): Unit = {
    movementCommand match {
      case MoveInDirection(direction) =>
        dynamics.orientation = direction.normalized
      case MoveToPosition(position) =>
        val dist = position - this.position
        val speed = 100 / 30 // TODO: improve this
        if ((dist dot dist) <= speed * speed) {
          dynamics.limitSpeed(dist.size * 30)
          dynamics.orientation = dist.normalized
          dynamics.limitSpeed(100)
        } else {
          dynamics.orientation = dist.normalized
        }
      case HarvestMineralCrystal(mineral) =>
        harvestResource(mineral)
        movementCommand = HoldPosition
      case HoldPosition =>
        dynamics.halt()
    }

    droneConstructions =
      for ((spec, progress) <- droneConstructions)
        yield (spec, progress + 1)
  }

  def enqueueEvent(event: DroneEvent): Unit = {
    eventQueue.enqueue(event)
  }


  def giveMovementCommand(value: MovementCommand): Unit = movementCommand = value
  def startDroneConstruction(command: ConstructDrone): Unit = {
    droneConstructions ::= ((command, 0))
  }

  def harvestResource(mineralCrystal: MineralCrystal): Unit = {
    // TODO: better error messages, add option to emit warnings and abort instead of throwing
    // TODO: harvesting takes some time to complete
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(this.position ~ mineralCrystal.position)
    storedMinerals ::= mineralCrystal
  }

  override def position: Vector2 = dynamics.pos

  def availableStorage: Int =
    storageCapacity - storedMinerals.map(_.size).sum - math.ceil(storedEnergyGlobes / 7.0).toInt

  def availableFactories: Int =
    factoryCapacity - droneConstructions.map(_._1.size - 2).sum


  override def descriptor: WorldObjectDescriptor = {
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.orientation.toFloat,
      Seq(),
      moduleDescriptors,
      Seq.fill[Byte](size - 1)(2),
      size
    )
  }

  private def moduleDescriptors: Seq[cwinter.worldstate.DroneModule] = {
    var result = List.empty[cwinter.worldstate.DroneModule]
    var index = 0
    for (
      l <- modules
      if l == Lasers
    ) {
      result ::= cwinter.worldstate.Lasers(index)
      index += 1
    }

    for ((ConstructDrone(_, size), _) <- droneConstructions) {
      result ::= cwinter.worldstate.ProcessingModule(index until index + (size - 2), 0)
      index += size - 2
    }
    for (i <- 0 until availableFactories) {
      result ::= cwinter.worldstate.ProcessingModule(Seq(index))
      index += 1
    }

    var storageSum = 0
    // TODO: HarvestedMineral class (no position)
    for (MineralCrystal(size, pos) <- storedMinerals.sortBy(-_.size)) {
      result ::= cwinter.worldstate.StorageModule(index until index + size, -1)
      index += size
      storageSum += size
    }
    var globesRemaining = storedEnergyGlobes
    for (i <- 0 until storageCapacity - storageSum) {
      val globes = math.min(7, globesRemaining)
      result ::= cwinter.worldstate.StorageModule(Seq(index), globes)
      globesRemaining -= globes
      index += 1
    }
    result
  }

  private def radius: Double = {
    val sideLength = 40
    val radiusBody = 0.5f * sideLength / math.sin(math.Pi / size).toFloat
    radiusBody + Geometry.circumradius(4, size)
  }
}


sealed trait Module

case object StorageModule extends Module
case object Lasers extends Module
case object NanobotFactory extends Module


sealed trait DroneEvent

case object Spawned extends DroneEvent
case class MineralEntersSightRadius(mineralCrystal: MineralCrystal) extends DroneEvent
case object ArrivedAtPosition extends DroneEvent

sealed trait DroneCommand

sealed trait MovementCommand extends DroneCommand
case class MoveInDirection(direction: Vector2) extends MovementCommand
case class MoveToPosition(position: Vector2) extends MovementCommand
case class HarvestMineralCrystal(mineralCrystal: MineralCrystal) extends MovementCommand
case object HoldPosition extends MovementCommand

sealed trait ConstructionCommand extends DroneCommand
case class ConstructDrone(modules: Seq[Module], size: Int) extends ConstructionCommand {
  // TODO: assert size ~ modules
}
case object ConstructTinyDrone {
  def apply(module: Module): ConstructDrone = ConstructDrone(Seq(module), 3)
}
case object ConstructSmallDrone {
  def apply(module1: Module, module2: Module): ConstructDrone =
    ConstructDrone(Seq(module1, module2), 4)
}

