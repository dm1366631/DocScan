package com.docscan.app

import android.app.Application
import org.opencv.android.OpenCVLoader

class DocScanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug()
    }
}