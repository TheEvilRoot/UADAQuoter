package com.theevilroot.uadaquoter

import android.app.Application
import daio.io.dresscode.DressCode
import daio.io.dresscode.declareDressCode

class App: Application() {

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        declareDressCode(
                DressCode("UADAF Light", R.style.AppTheme_UADAFDark),
                DressCode("UADAF Dark", R.style.AppTheme_UADAFLight)

        )
    }

}