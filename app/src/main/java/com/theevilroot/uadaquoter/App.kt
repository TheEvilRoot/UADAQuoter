package com.theevilroot.uadaquoter

import android.app.Application
import android.net.Uri
import com.theevilroot.uadaquoter.objects.License
import daio.io.dresscode.DressCode
import daio.io.dresscode.declareDressCode

class App: Application() {

    companion object {
        lateinit var instance: App
    }

    val appLicenses = listOf(
            License("Test", "OpenSource", "Фистандантилус", Uri.parse("https://github.com/TheEvilRoot"))
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        declareDressCode(
                DressCode("UADAF Light", R.style.AppTheme_UADAFDark),
                DressCode("UADAF Dark", R.style.AppTheme_UADAFLight)
        )
    }

}