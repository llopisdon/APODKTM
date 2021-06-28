package com.machineinteractive.apodktm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

const val TAG = "@APOD"
const val APOD_EPOCH_MONTH = 6
const val APOD_EPOCH_YEAR = 1995

@HiltAndroidApp
class APODKTMApp : Application()