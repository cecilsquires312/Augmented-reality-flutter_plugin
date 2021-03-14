package io.carius.lars.ar_flutter_plugin.Serialization

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3


fun deserializeMatrix4(transform: ArrayList<Double>): Triple<Vector3, Vector3, Quaternion> {
    val scale = Vector3()
    val position = Vector3()
    val rotation: Quaternion

    // Get the scale by calculating the length of each 3-dimensional column vector of the transformation matrix
    // See https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati for a mathematical explanation
    scale.x = Vector3(transform[0].toFloat(), transform[1].toFloat(), transform[2].toFloat()).length()
    scale.y = Vector3(transform[4].toFloat(), transform[5].toFloat(), transform[6].toFloat()).length()
    scale.z = Vector3(transform[8].toFloat(), transform[9].toFloat(), transform[10].toFloat()).length()

    // Get the translation by taking the last column of the transformation matrix
    // See https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati for a mathematical explanation
    position.x = transform[12].toFloat()
    position.y = transform[13].toFloat()
    position.z = transform[14].toFloat()

    // Get the rotation matrix from the transformation matrix by normalizing with the scales
    // See https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati for a mathematical explanation
    val rowWiseMatrix = floatArrayOf(transform[0].toFloat() / scale.x, transform[4].toFloat() / scale.y, transform[8].toFloat() / scale.z, transform[1].toFloat() / scale.x, transform[5].toFloat() / scale.y, transform[9].toFloat() / scale.z, transform[2].toFloat() / scale.x, transform[6].toFloat() / scale.y, transform[10].toFloat() / scale.z)

    // Calculate the quaternion from the rotation matrix
    // See https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/ for a mathematical explanation
    val w = Math.sqrt(1.0 + rowWiseMatrix[0] + rowWiseMatrix[4] + rowWiseMatrix[8]) / 2.0
    val x = (rowWiseMatrix[7] - rowWiseMatrix[5]) / (4.0 * w)
    val y = (rowWiseMatrix[2] - rowWiseMatrix[6]) / (4.0 * w)
    val z = (rowWiseMatrix[3] - rowWiseMatrix[1]) / (4.0 * w)
    val inputRotation = Quaternion(x.toFloat(),y.toFloat(),z.toFloat(),w.toFloat())

    // Rotate by an additional 180 degrees around z and y to compensate for the different model coordinate system definition used in Sceneform (in comparison to Scenekit and the definition used for the Flutter API of this plugin)
    val correction_z = Quaternion(0.0f, 0.0f, 1.0f, 180f)
    val correction_y = Quaternion(0.0f, 1.0f, 0.0f, 180f)

    // Calculate resulting rotation quaternion by multiplying input and corrections
    rotation = Quaternion.multiply(Quaternion.multiply(inputRotation, correction_y), correction_z)

    return Triple(scale, position, rotation)
}
