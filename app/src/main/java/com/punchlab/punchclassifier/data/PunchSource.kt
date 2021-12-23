package com.punchlab.punchclassifier.data


object PunchSource {
    val mockPunches: List<Punch> = listOf(
        Punch(0, 200, 85),
        Punch(1, 200, 85),
        Punch(2, 200, 85),
        Punch(3, 200, 85),
        Punch(4, 200, 85),
        Punch(5, 200, 85),
        Punch(6, 200, 85)
    )

    val truePunches = mutableListOf<Punch>()
}