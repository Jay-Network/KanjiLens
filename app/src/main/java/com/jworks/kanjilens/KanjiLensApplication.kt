package com.jworks.kanjilens

import android.app.Application
import com.jworks.kanjilens.data.nlp.KuromojiTokenizer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KanjiLensApplication : Application() {

    @Inject
    lateinit var kuromojiTokenizer: KuromojiTokenizer

    override fun onCreate() {
        super.onCreate()
        // Initialize Kuromoji on background thread (takes ~1-3 seconds)
        Thread { kuromojiTokenizer.initialize() }.start()
    }
}
