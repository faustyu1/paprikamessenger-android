package ru.faustyu.paprika.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

object VoiceRecorder {
    private const val TAG = "VoiceRecorder"
    private var recorder: MediaRecorder? = null
    var currentFilePath: String? = null
        private set

    fun startRecording(context: Context) {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        currentFilePath = file.absolutePath

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e(TAG, "startRecording failed: $e")
                release()
                recorder = null
                currentFilePath = null
            }
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            currentFilePath
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording failed: $e")
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        currentFilePath?.let { File(it).delete() }
        currentFilePath = null
    }
}
