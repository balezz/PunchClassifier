package com.punchlab.punchclassifier.data

data class Person(val keyPoints: List<KeyPoint>, val score: Float){
    override fun toString(): String {
        return "Person score: $score"
    }
}

