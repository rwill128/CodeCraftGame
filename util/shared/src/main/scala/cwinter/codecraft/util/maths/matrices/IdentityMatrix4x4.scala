package cwinter.codecraft.util.maths.matrices

private[codecraft] object IdentityMatrix4x4 extends Matrix4x4(
  Array[Float](
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1
  )
)