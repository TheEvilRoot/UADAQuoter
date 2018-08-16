package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import me.philio.pinentry.PinEntryView
import java.io.File
import kotlin.concurrent.thread

class EditQuoteActivity : AppCompatActivity() {

    lateinit var app: App

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val authorView by bind<EditText>(R.id.eq_author)
    private val adderView by bind<EditText>(R.id.eq_adder)
    private val quoteView by bind<EditText>(R.id.eq_quote)
    private val saveButton by bind<Button>(R.id.eq_save)
    private val statusView by bind<TextView>(R.id.eq_status)

    private val statusInAnimation: Animation by lazy { AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left) }
    private val statusOutAnimation: Animation by lazy { AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right) }

    private lateinit var quote: Quote

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_quote_activity)
        app = application as App
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        quote = Quote.fromJson(JsonParser().parse(intent.extras.getString("quote")).asJsonObject)
        authorView.setText(quote.author)
        authorView.isEnabled = false
        adderView.setText(app.adderName)
        quoteView.setText(quote.text)
        saveButton.text = "Сохранить"
        saveButton.setOnClickListener {
            if(quoteView.text.toString() == quote.text) {
                showStatus("Что изменилось?", android.R.color.holo_green_dark, 1000)
                return@setOnClickListener
            }
            val text = quoteView.text.toString()
            val adder = adderView.text.toString()
            if(text.isBlank() || adder.isBlank()) {
                showStatus("Заполните все поля!", android.R.color.holo_red_light, 1000)
                return@setOnClickListener
            }
            val view = layoutInflater.inflate(R.layout.security_code_dialog_layout, null)
            val dialog = AlertDialog.Builder(this, R.style.AppTheme_Dialog).setView(view).create()
            val pinView = view.findViewById<PinEntryView>(R.id.security_code_field)
            pinView.addTextChangedListener(TextWatcherWrapper(onChange = {str, _,_,_ ->
                if(str.length == 4) {
                    val code = str.toIntOrNull() ?: return@TextWatcherWrapper
                    if(code == 6741) {
                        showStatus("Сохранение...", android.R.color.holo_green_light, Runnable {
                            QuoterAPI.edit(quote.id, adder, text, PrivateReferences.CODE_PREFIX + code, {
                                runOnUiThread {
                                    authorView.setText("")
                                    adderView.setText("")
                                    quoteView.setText("")
                                    statusView.text = "Успешно!!!"
                                }
                            }, {
                                runOnUiThread { statusView.text = "Ошибка: ${it?.localizedMessage}" }
                            })
                            Thread.sleep(1000)
                        })
                        dialog.dismiss()
                    }
                }
            }))
            dialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_quote_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.tb_personal_data -> {
                val view = layoutInflater.inflate(R.layout.personal_data_layout, null)
                val dialog = AlertDialog.Builder(this, R.style.AppTheme_Dialog).setView(view).create()
                val adderNameView = view.findViewById<EditText>(R.id.personal_data_adder_name_field)
                val saveButton = view.findViewById<Button>(R.id.personal_data_save)
                adderNameView.setText(app.adderName)
                saveButton.setOnClickListener {
                    val name = adderNameView.text.toString()
                    if(name == app.adderName) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }
                    showStatus("Изменение данных", android.R.color.holo_green_light, Runnable {
                        val file = File(filesDir, "user.json")
                        if(!file.exists())
                            file.createNewFile()
                        val json = JsonObject()
                        json.addProperty("adderName", name)
                        file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
                        app.adderName = name
                        runOnUiThread { statusView.text = "Изменено!" }
                        Thread.sleep(1000)
                    })
                    dialog.dismiss()
                }
                dialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showStatus(msg: String, @ColorRes color: Int, action: Runnable) {
        thread(true) {
            runOnUiThread { if (statusView.visibility == View.VISIBLE) return@runOnUiThread; statusView.text = msg; statusView.setTextColor(getColor(color)); statusView.visibility = View.VISIBLE; statusView.startAnimation(statusInAnimation) }
            action.run()
            runOnUiThread { if (statusView.visibility == View.GONE) return@runOnUiThread; statusView.clearAnimation(); statusView.startAnimation(statusOutAnimation); statusView.visibility = View.GONE }
        }
    }

    private fun showStatus(msg: String, @ColorRes color: Int, time: Long) {
        showStatus(msg, color, Runnable { Thread.sleep(time) })
    }
}