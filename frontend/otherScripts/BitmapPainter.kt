package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.nawigacja_wewnatrz_budynku.R
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Floor
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point3D
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import kotlin.math.pow

//class BitmapPainter(
//    private val context: Context
//){
//    val paint = Paint().apply {
//        color = Color.RED
//        strokeWidth = 1f
//        style = Paint.Style.STROKE
//        isAntiAlias = false
//    }
//    private lateinit var originalBitmap : Bitmap
//    private lateinit var copyBitmap : Bitmap
//    private lateinit var canvas: Canvas
//    var mapWidth  = 0
//    var mapHeight = 0
//    var floor = 0
//
//    fun drawPath(path: List<Point3D>, imageView: ImageView, grid: Array<IntArray>, map: Bitmap) : Int
//    {
//        originalBitmap = map
//        copyBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        mapWidth = originalBitmap.width / grid[0].size
//        mapHeight = originalBitmap.height / grid.size
//        canvas = Canvas(copyBitmap)
//        floor = path[0].floor
//        for(i in 0 until path.size - 1)
//        {
//            if(path[1].floor != floor)
//            {
//                drawIcon(imageView, path[i+1].floor, Point(path[0].x, path[0].y))
//                return i
//            }
//            canvas.drawLine((path[i].x.toFloat() * mapWidth) + (mapWidth/2), (path[i].y.toFloat() * mapHeight) + (mapHeight/2), (path[i + 1].x.toFloat() * mapWidth) + (mapWidth/2), (path[i + 1].y.toFloat() * mapHeight) + (mapHeight/2), paint)
//        }
//        imageView.setImageBitmap(copyBitmap)
//        return -1
//    }
//    fun drawIcon(imageView: ImageView, nextFloor: Int, point: Point)
//    {
//        val originalIcon : Bitmap
//        val scale  = 0.2f
//        if(nextFloor > floor)
//        {
//            originalIcon = BitmapFactory.decodeResource(context.resources, R.drawable.stairs_up)
//            val icon = originalIcon.scale(
//                (originalIcon.width * scale).toInt(),
//                (originalIcon.height * scale).toInt()
//            )
//            val x = (point.x.toFloat() * mapWidth) + (mapWidth / 2) - (icon.width / 2)
//            val y = (point.y.toFloat() * mapHeight) + (mapHeight / 2) - (icon.height / 2)
//            canvas.drawBitmap(icon, x, y, null)
//            imageView.setImageBitmap(copyBitmap)
//        }
//        else if(nextFloor < floor)
//        {
//            originalIcon = BitmapFactory.decodeResource(context.resources, R.drawable.stairs_down)
//            val icon = originalIcon.scale(
//                (originalIcon.width * scale).toInt(),
//                (originalIcon.height * scale).toInt()
//            )
//            val x = (point.x.toFloat() * mapWidth) + (mapWidth / 2) - (icon.width / 2)
//            val y = (point.y.toFloat() * mapHeight) + (mapHeight / 2) - (icon.height / 2)
//            canvas.drawBitmap(icon, x, y, null)
//            imageView.setImageBitmap(copyBitmap)
//        }
//    }
//}
class BitmapPainter(private val context: Context) {
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.input_color)
        strokeWidth = 8f  // zostanie przeskalowana do tileSize
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    var tileSize = 20
    private var floor = 0
    private var iconsList = mutableListOf<Pair<Int,Point>>()

    fun drawPath(
        path: List<Point3D>,
        imageView: ImageView,
        grid: Array<IntArray>
    ): Int {
        tileSize = calculateTileSize(imageView.width, imageView.height, grid[0].size, grid.size)

        val bitmapWidth = grid[0].size * tileSize
        val bitmapHeight = grid.size * tileSize

        bitmap = createBitmap(bitmapWidth, bitmapHeight)
        canvas = Canvas(bitmap)
        floor = path[0].floor

        // narysuj tło (opcjonalne)
        drawGrid(grid)

        // narysuj ścieżkę
        for (i in 0 until path.size - 1) {
            if (path[i + 1].floor != floor) {
                iconsList.add(Pair(path[i + 1].floor, Point(path[i].x, path[i].y)))
                //drawIcon(imageView, path[i + 1].floor, Point(path[i].x, path[i].y))
                return i
            }

            val startX = path[i].x * tileSize + tileSize / 2f
            val startY = path[i].y * tileSize + tileSize / 2f
            val endX = path[i + 1].x * tileSize + tileSize / 2f
            val endY = path[i + 1].y * tileSize + tileSize / 2f

            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        imageView.setImageBitmap(bitmap)
        return -1
    }

    private fun drawGrid(grid: Array<IntArray>) {
        val cellPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        for (y in grid.indices) {
            for (x in grid[0].indices) {
                cellPaint.color = when (grid[y][x]) {
                    0 -> ContextCompat.getColor(context, R.color.opposite_input_color)
                    1 -> ContextCompat.getColor(context, R.color.background_color)
                    else -> Color.LTGRAY
                }

                canvas.drawRect(
                    (x * tileSize).toFloat(),
                    (y * tileSize).toFloat(),
                    ((x + 1) * tileSize).toFloat(),
                    ((y + 1) * tileSize).toFloat(),
                    cellPaint
                )
            }
        }
    }

    fun drawIcon(imageView: ImageView, nextFloor: Int, point: Point, rotation: Float) {
        val iconRes = when {
            nextFloor > floor -> R.drawable.stairs_up
            nextFloor < floor -> R.drawable.stairs_down
            else -> return
        }

        val originalIcon = BitmapFactory.decodeResource(context.resources, iconRes)
        val iconSize = (bitmap.height * 0.03f).toInt()
        val icon = originalIcon.scale(iconSize, iconSize)
        val centerX = point.x * tileSize + tileSize / 2f
        val centerY = point.y * tileSize + tileSize / 2f

        var matrix = Matrix()
        matrix.postTranslate(-icon.width / 2f, -icon.height / 2f)
        matrix.postRotate(rotation * -1)
        matrix.postTranslate(centerX, centerY)

        canvas.drawBitmap(icon, matrix, null)
        imageView.setImageBitmap(bitmap)
    }

    fun drawIcons(imageView: ImageView, rotation: Float)
    {
        for (icon in iconsList)
        {
            drawIcon(imageView, icon.first, icon.second, rotation)
        }
        iconsList.clear()
    }

    private fun calculateTileSize(viewWidth: Int, viewHeight: Int, cols: Int, rows: Int): Int {
        val maxWidthPerTile = viewWidth / cols
        val maxHeightPerTile = viewHeight / rows
        return (minOf(maxWidthPerTile, maxHeightPerTile))
    }
}
