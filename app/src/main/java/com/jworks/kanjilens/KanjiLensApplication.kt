package com.jworks.kanjilens

import android.app.Application
import android.util.Log
import com.jworks.kanjilens.data.nlp.KuromojiTokenizer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KanjiLensApplication : Application() {

    @Inject
    lateinit var kuromojiTokenizer: KuromojiTokenizer

    override fun onCreate() {
        super.onCreate()

        // Global crash handler — logs unhandled exceptions before the process dies
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KanjiLens", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Kuromoji on background thread (takes ~1-3 seconds)
        Thread { kuromojiTokenizer.initialize() }.start()
    }
}
