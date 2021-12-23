package com.punchlab.punchclassifier.converters

object ConverterUtils {

    fun List<Int>.splitByZeros(minPunchLength: Int = 5):List<Pair<Int, Int>> {
        if (this.size < minPunchLength) return listOf(Pair(0, 0))
        val out = mutableListOf<Pair<Int, Int>>()
        var start = 0
        var stop = 0
        var punch = this[0] != 0
        for ((i, v) in this.withIndex()) {
            if (punch) {
                if (v != 0) continue
                else {
                    stop = i
                    punch = false
                }
            } else {
                if (v == 0) continue
                else {
                    start = i
                    punch = true
                }
            }
            if ((stop - start) > minPunchLength) {
                out.add(Pair(start, stop))
            }
        }
        return out
    }
}