package com.punchlab.punchclassifier.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class PunchRnnClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    private val inputShape = interpreter.getInputTensor(0).shape()
    private val outputShape = interpreter.getOutputTensor(0).shape()

    companion object {
        private const val TAG = "PunchRnnClassifier"
        private const val MODEL_FILENAME = "lstm_v01.tflite"
        private const val LABELS_FILENAME = "labels.txt"
        private const val CPU_NUM_THREADS = 4

        fun create(context: Context): PunchRnnClassifier {
            val options = Interpreter.Options().apply {
                setNumThreads(CPU_NUM_THREADS)
            }
            return PunchRnnClassifier(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context, MODEL_FILENAME
                    ), options
                ),
                FileUtil.loadLabels(context, LABELS_FILENAME)
            )
        }
    }

    fun classify(keyPoints: List<FloatArray>): List<Int> {
        // Preprocess the pose estimation result to a array shape (1, 30, 36)
        Log.d(TAG, "Input shape: ${inputShape.contentToString()}, " +
                       "Output shape: ${outputShape.contentToString()}")


        // Postprocess the model output to human readable class names
        val outputTensor = Array(outputShape[1]){FloatArray(outputShape[2])}
        repeat(outputTensor.size) { FloatArray(outputShape[2]) }
        interpreter.run(arrayOf(keyPoints.toTypedArray()), arrayOf(outputTensor))

        return outputTensor
            .map { floatArray -> floatArray.withIndex().maxByOrNull { it.value }?.index!! }
    }

    fun close() {
        interpreter.close()
    }
}