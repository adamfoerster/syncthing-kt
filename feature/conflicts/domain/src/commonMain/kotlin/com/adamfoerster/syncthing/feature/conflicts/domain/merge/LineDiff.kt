package com.adamfoerster.syncthing.feature.conflicts.domain.merge

/** One step of an edit script turning `a` into `b`. Indices point into the original lists. */
sealed interface DiffOp {
    data class Equal(val aIndex: Int, val bIndex: Int) : DiffOp
    data class Delete(val aIndex: Int) : DiffOp
    data class Insert(val bIndex: Int) : DiffOp
}

/**
 * Line diff using Myers' O((N+M)·D) algorithm, after trimming the common prefix and suffix.
 * When the files differ in more than [maxEdits] lines the middle is reported as one replace,
 * which keeps memory bounded (the trace is O(D²)).
 */
object LineDiff {
    const val DEFAULT_MAX_EDITS = 4_000

    fun diff(a: List<String>, b: List<String>, maxEdits: Int = DEFAULT_MAX_EDITS): List<DiffOp> {
        var prefix = 0
        while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
        var suffix = 0
        while (suffix < a.size - prefix && suffix < b.size - prefix &&
            a[a.size - 1 - suffix] == b[b.size - 1 - suffix]
        ) suffix++

        // Compare ints instead of strings in the hot loop.
        val ids = HashMap<String, Int>()
        val midA = IntArray(a.size - prefix - suffix) { ids.getOrPut(a[prefix + it]) { ids.size } }
        val midB = IntArray(b.size - prefix - suffix) { ids.getOrPut(b[prefix + it]) { ids.size } }

        val ops = ArrayList<DiffOp>(a.size + b.size)
        repeat(prefix) { ops += DiffOp.Equal(it, it) }
        val middle = myers(midA, midB, maxEdits)
        if (middle != null) {
            middle.forEach { op ->
                ops += when (op) {
                    is DiffOp.Equal -> DiffOp.Equal(op.aIndex + prefix, op.bIndex + prefix)
                    is DiffOp.Delete -> DiffOp.Delete(op.aIndex + prefix)
                    is DiffOp.Insert -> DiffOp.Insert(op.bIndex + prefix)
                }
            }
        } else {
            midA.indices.forEach { ops += DiffOp.Delete(prefix + it) }
            midB.indices.forEach { ops += DiffOp.Insert(prefix + it) }
        }
        repeat(suffix) { ops += DiffOp.Equal(a.size - suffix + it, b.size - suffix + it) }
        return ops
    }

    private fun myers(a: IntArray, b: IntArray, maxEdits: Int): List<DiffOp>? {
        val n = a.size
        val m = b.size
        val max = n + m
        if (max == 0) return emptyList()
        val offset = max + 1
        val v = IntArray(2 * max + 3)
        // trace[d] holds v[-(d+1)..(d+1)] as it was before round d — all the backtrack needs.
        val trace = ArrayList<IntArray>()
        var editDistance = -1

        search@ for (d in 0..max) {
            if (d > maxEdits) return null
            trace += v.copyOfRange(offset - d - 1, offset + d + 2)
            var k = -d
            while (k <= d) {
                var x = if (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])) {
                    v[offset + k + 1]
                } else {
                    v[offset + k - 1] + 1
                }
                var y = x - k
                while (x < n && y < m && a[x] == b[y]) {
                    x++
                    y++
                }
                v[offset + k] = x
                if (x >= n && y >= m) {
                    editDistance = d
                    break@search
                }
                k += 2
            }
        }

        val ops = ArrayList<DiffOp>(max)
        var x = n
        var y = m
        for (d in editDistance downTo 0) {
            val snapshot = trace[d]
            fun vAt(k: Int) = snapshot[k + d + 1]
            val k = x - y
            val prevK = if (k == -d || (k != d && vAt(k - 1) < vAt(k + 1))) k + 1 else k - 1
            val prevX = vAt(prevK)
            val prevY = prevX - prevK
            while (x > prevX && y > prevY) {
                ops += DiffOp.Equal(x - 1, y - 1)
                x--
                y--
            }
            if (d > 0) {
                ops += if (x == prevX) DiffOp.Insert(y - 1) else DiffOp.Delete(x - 1)
            }
            x = prevX
            y = prevY
        }
        ops.reverse()
        return ops
    }
}
