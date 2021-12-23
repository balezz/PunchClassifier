package com.punchlab.punchclassifier.data


data class Punch(
    val punchTypeIndex: Int,
    val duration: Int,
    val quality: Int){
    val name: String = PunchClass.fromInt(punchTypeIndex).toString()

    override fun toString(): String {
        return "${PunchClass.fromInt(punchTypeIndex)}, duration: $duration, quality: $quality"
    }

    companion object{
        fun fromBounds(punchIdxs: List<Int>, bounds: Pair<Int, Int>) : Punch {
            val startTime: Double = bounds.first * 0.03333
            val duration: Int = (bounds.second - bounds.first) * 33
            val punchIndex: Int = punchIdxs
                .slice(bounds.first .. bounds.second)
                .groupBy { it }
                .mapValues { it.value.size }
                .maxByOrNull { it.value }!!.key
            return Punch(punchIndex, duration, 85)
        }
    }

}

enum class PunchClass(val position: Int) {
    NO_PUNCH(0),
    LEFT_JAB(1),
    RIGHT_JAB(2),
    LEFT_HOOK(3),
    RIGHT_HOOK(4),
    LEFT_UPPERCUT(5),
    RIGHT_UPPERCUT(6);
    companion object{
        private val map = values().associateBy(PunchClass::position)
        fun fromInt(position: Int): PunchClass = map.getValue(position)
    }
}
