package com.example.nawigacja_wewnatrz_budynku.otherScripts

import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point3D

class AStar {

    fun aStarMultiFloor(
        floors: Map<Int, Array<IntArray>>,  // mapowanie piętro -> siatka
        start: Point3D,
        goal: Point3D,
        connections: List<Pair<Point3D, Point3D>>
    ): List<Point3D> {

        fun heuristic(a: Point3D, b: Point3D): Double {
            val dx = kotlin.math.abs(a.x - b.x)
            val dy = kotlin.math.abs(a.y - b.y)
            return kotlin.math.max(dx, dy).toDouble()
        }

        fun neighbors(p: Point3D): List<Point3D> {
            val result = mutableListOf<Point3D>()
            val grid = floors[p.floor] ?: return result  // brak mapy dla piętra = brak sąsiadów

            val rows = grid.size
            val cols = grid[0].size

            val directions = listOf(
                Point3D(0, -1, p.floor),  // góra
                Point3D(0, 1, p.floor),   // dół
                Point3D(-1, 0, p.floor),  // lewo
                Point3D(1, 0, p.floor),   // prawo
                Point3D(-1, -1, p.floor), // lewy-górny
                Point3D(1, -1, p.floor),  // prawy-górny
                Point3D(-1, 1, p.floor),  // lewy-dolny
                Point3D(1, 1, p.floor)    // prawy-dolny
            )
            for (d in directions) {
                val nx = p.x + d.x
                val ny = p.y + d.y
                if (nx in 0 until cols && ny in 0 until rows && grid[ny][nx] == 0) {
                    result.add(Point3D(nx, ny, p.floor))
                }
            }

            for ((from, to) in connections) {
                if (p == from) result.add(to)
                if (p == to) result.add(from)
            }

            return result
        }

        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Point3D, Point3D?>()

        val gScore = mutableMapOf<Point3D, Double>().withDefault { Double.POSITIVE_INFINITY }
        val fScore = mutableMapOf<Point3D, Double>().withDefault { Double.POSITIVE_INFINITY }

        gScore[start] = 0.0
        fScore[start] = heuristic(start, goal)

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore.getValue(it) }!!

            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }

            openSet.remove(current)

            for (neighbor in neighbors(current)) {
                val dx = kotlin.math.abs(neighbor.x - current.x)
                val dy = kotlin.math.abs(neighbor.y - current.y)
                val cost = if (dx == 1 && dy == 1) 1.414 else 1.0

                val tentativeGScore = gScore.getValue(current) + cost
                if (tentativeGScore < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(neighbor, goal)
                    openSet.add(neighbor)
                }
            }
        }

        return emptyList()
    }

    fun reconstructPath(cameFrom: Map<Point3D, Point3D?>, current: Point3D): List<Point3D> {
        val path = mutableListOf(current)
        var curr = current
        while (cameFrom[curr] != null) {
            curr = cameFrom[curr]!!
            path.add(0, curr)
        }
        return path
    }
}
