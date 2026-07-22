package com.toblad.khwab.speech

import android.content.Context
import android.util.Log
import java.io.File

data class ModelPaths(
    val encoder: String,
    val decoder: String,
    val joiner: String,
    val tokens: String,
    val bpe: String
)

class ModelLoader(
    private val context: Context
) {

    companion object {
        private const val TAG = "ModelLoader"
        private const val ASSET_DIR = "models/zipformer"
        private const val OUTPUT_DIR = "zipformer"
    }

    fun loadModel(): ModelPaths {

        val outputDir = File(context.filesDir, OUTPUT_DIR)

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val assetManager = context.assets
        val files = assetManager.list(ASSET_DIR)
            ?: throw IllegalStateException("Zipformer assets not found.")

        files.forEach { fileName ->
            val outFile = File(outputDir, fileName)

            if (!outFile.exists()) {
                assetManager.open("$ASSET_DIR/$fileName").use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copied: $fileName")
            }
        }

        fun requireFile(fileName: String): File {
            val file = File(outputDir, fileName)
            require(file.exists()) {
                "Missing model file: $fileName"
            }
            return file
        }

        val encoder =
            requireFile("encoder-epoch-99-avg-1-chunk-16-left-128.onnx")

        val decoder =
            requireFile("decoder-epoch-99-avg-1-chunk-16-left-128.onnx")

        val joiner =
            requireFile("joiner-epoch-99-avg-1-chunk-16-left-128.onnx")

        val tokens =
            requireFile("tokens.txt")

        val bpe =
            requireFile("bpe.model")

        Log.d(TAG, "Zipformer model prepared successfully.")

        return ModelPaths(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            joiner = joiner.absolutePath,
            tokens = tokens.absolutePath,
            bpe = bpe.absolutePath
        )
    }
}