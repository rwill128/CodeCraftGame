package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.{Vector2, ColorRGBA}


private[codecraft] object Debug {
  private[this] var objects = List.empty[WorldObjectDescriptor]
  private[this] var staticObjects = List.empty[WorldObjectDescriptor]
  private[this] var _textModels = List.empty[TextModel]

  def draw(worldObject: WorldObjectDescriptor): Unit = {
    objects ::= worldObject
  }

  def drawAlways(worldObject: WorldObjectDescriptor): Unit = {
    staticObjects ::= worldObject
  }

  def drawText(text: String, xPos: Double, yPos: Double, color: ColorRGBA): Unit = {
    _textModels ::= TextModel(text, xPos.toFloat, yPos.toFloat, color)
  }


  private[this] var _cameraOverride: Option[() => Vector2] = None
  def setCameraOverride(getPos: => Vector2): Unit = {
    _cameraOverride = Some(() => getPos)
  }

  def cameraOverride: Option[Vector2] = _cameraOverride.map(_())

  private[engine] def debugObjects = {
    objects ++ staticObjects
  }

  private[engine] def textModels = _textModels

  private[codecraft] def clear(): Unit = {
    objects = List.empty[WorldObjectDescriptor]
    _textModels = List.empty[TextModel]
  }

  private[cwinter] def clearDrawAlways(): Unit = {
    objects = List.empty[WorldObjectDescriptor]
  }
}
