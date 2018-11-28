package com.theevilroot.uadaquoter

import android.app.Application
import android.net.Uri
import com.jetradar.permissions.MrButler
import com.jetradar.permissions.PermissionsActivityDelegate
import com.theevilroot.uadaquoter.objects.License
import com.theevilroot.uadaquoter.objects.Message
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.references.PrivateReferences
import daio.io.dresscode.DressCode
import daio.io.dresscode.declareDressCode

class App: Application() {

    companion object {
        lateinit var instance: App
    }

    val appLicenses = listOf(
            License("Test", "OpenSource", "Фистандантилус", Uri.parse("https://github.com/TheEvilRoot"))
    )

    val quotes: ArrayList<Quote> = ArrayList()
    val messages: ArrayList<Message> = ArrayList()

    val permissionsActivityDelegate = PermissionsActivityDelegate()
    val butler = MrButler(permissionsActivityDelegate)
    val references = PrivateReferences()

    lateinit var api: RxQuoterApi.QuoterServiceApi

    override fun onCreate() {
        super.onCreate()
        instance = this
        api = RxQuoterApi.QuoterServiceApi(applicationContext, false)
        declareDressCode(
                DressCode("UADAF Light", R.style.AppTheme_UADAFDark),
                DressCode("UADAF Dark", R.style.AppTheme_UADAFLight)
        )
    }

}