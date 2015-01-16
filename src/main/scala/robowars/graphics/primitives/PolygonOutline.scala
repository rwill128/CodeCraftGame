package robowars.graphics.primitives

import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._

import scala.reflect.ClassTag


class PolygonOutline[TColor <: Vertex : ClassTag]
(material: Material[VertexXYZ, TColor])
(val nCorners: Int, val innerRadius: Float, val outerRadius: Float)
  extends Primitive2D[TColor](PolygonOutline.computeVertices(nCorners, innerRadius, outerRadius), material) {

  assert(innerRadius < outerRadius)

  def colorOutside(color: TColor): PolygonOutline[TColor] = {
    for (i <- 0 until nCorners) {
      colors(2 * 3 * i + 1) = color
      colors(2 * 3 * i + 2) = color
      colors(2 * 3 * i + 4) = color
    }
    this
  }

  def colorInside(color: TColor): PolygonOutline[TColor] = {
    for (i <- 0 until nCorners) {
      colors(2 * 3 * i + 0) = color
      colors(2 * 3 * i + 3) = color
      colors(2 * 3 * i + 5) = color
    }
    this
  }
}

object PolygonOutline {
  def computeVertices(nCorners: Int, innerRadius: Float, outerRadius: Float): Array[VertexXY] = {
    val angle = (2 * math.Pi / nCorners).toFloat
    val rotation = Matrix2x2.rotation(angle)

    var outerLast = new VertexXY(outerRadius, 0)
    var innerLast = new VertexXY(innerRadius, 0)

    val positions = new Array[VertexXY](2 * 3 * nCorners)


    for (i <- 0 until nCorners) {
      val outer = rotation * outerLast
      val inner = rotation * innerLast

      positions(2 * 3 * i + 0) = innerLast
      positions(2 * 3 * i + 1) = outerLast
      positions(2 * 3 * i + 2) = outer

      positions(2 * 3 * i + 3) = innerLast
      positions(2 * 3 * i + 4) = outer
      positions(2 * 3 * i + 5) = inner

      outerLast = outer
      innerLast = inner
    }

    positions
  }
}
