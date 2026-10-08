package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.example.nawigacja_wewnatrz_budynku.R
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point3D
import com.example.nawigacja_wewnatrz_budynku.dataClasses.PointF

class MapScaler(
    private val imageView: ImageView,
    private val grid: Array<IntArray>,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val x: Int,
    private val y: Int,
    private val path : List<Point3D>
) {

    val matrix = Matrix()
    val camera = Camera()

    fun CenterMap()
    {
        var userPosition = Point(x * mapWidth + (mapWidth / 2), y * mapHeight + (mapHeight / 2))
        var screenCenter : Point
        if(path[0].floor != path[1].floor)
        {
            screenCenter = Point(imageView.width / 2, imageView.height / 2)
        }
        else
        {
            screenCenter = Point(imageView.width / 2, (imageView.height / 8) * 7)
        }
        (imageView.drawable as? BitmapDrawable)?.paint?.isFilterBitmap = false
        imageView.invalidate()
        val scaleX = imageView.width.toFloat() / grid[0].size
        val scaleY = imageView.height.toFloat() / grid.size
        Log.d("WIDTH", grid[0].size.toString())
        val scale = minOf(scaleX, scaleY) * 0.01f * (grid[0].size / 7f)
        Log.d("ScaleDebug", "mapWidth=$mapWidth, mapHeight=$mapHeight, scaleX=$scaleX, scaleY=$scaleY, scale=$scale")
        var delta = PointF(screenCenter.x - userPosition.x * scale, screenCenter.y - userPosition.y * scale)
        matrix.postScale(scale, scale)
        matrix.postTranslate(delta.x, delta.y)
        imageView.imageMatrix = matrix
    }

    fun RotateMap():Float
    {
        val dx = path[1].x - path[0].x
        val dy = path[1].y - path[0].y

        val angleRad = kotlin.math.atan2(-dy.toDouble(), dx.toDouble())
        var angleDeg = Math.toDegrees(angleRad).toFloat() - 90

        val pivotX = (x * mapWidth + (mapWidth / 2)).toFloat()
        val pivotY = (y * mapHeight + (mapHeight / 2)).toFloat()

        Log.d("RotateMap", "dx=$dx, dy=$dy, angleDeg=$angleDeg, pivotX=$pivotX, pivotY=$pivotY")

        matrix.postRotate(angleDeg, pivotX, pivotY)

        imageView.imageMatrix = matrix
        return angleDeg
    }

    fun CenterUserAtBottom() {
        matrix.postScale(1.5f, 1.5f, imageView.width / 2f, imageView.height / 2f)

        // global user pos (raw map coordinate)
        val userX = x * mapWidth + (mapWidth / 2)
        val userY = y * mapHeight + (mapHeight / 2)

        // gdzie chcemy wyświetlić użytkownika na ekranie?
        val targetScreenX = imageView.width / 2f
        val targetScreenY = (imageView.height * 0.75f)   // 80% wysokości ekranu

        // teraz po rotacji i zoomie obraz jest w "matrix"
        // musimy policzyć gdzie aktualnie wypadnie user
        val src = floatArrayOf(userX.toFloat(), userY.toFloat())
        val dst = FloatArray(2)
        matrix.mapPoints(dst, src)

        val currentScreenX = dst[0]
        val currentScreenY = dst[1]

        // ile trzeba przesunąć?
        val dx = targetScreenX - currentScreenX
        val dy = targetScreenY - currentScreenY

        matrix.postTranslate(dx, dy)

        imageView.imageMatrix = matrix
    }
}