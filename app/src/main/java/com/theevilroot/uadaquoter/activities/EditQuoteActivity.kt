package com.theevilroot.uadaquoter.activities

import android.graphics.Color
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
import org.jsoup.Jsoup
import java.io.File
import kotlin.concurrent.thread

class EditQuoteActivity : AppCompatActivity() {

    lateinit var app: App
    lateinit var toolbar: Toolbar
    lateinit var authorView: EditText
    lateinit var adderView: EditText
    lateinit var quoteView: EditText
    lateinit var saveButton: Button
    lateinit var statusView: TextView

    lateinit var statusInAnimation: Animation
    lateinit var statusOutAnimation: Animation

    private lateinit var quote: Quote

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_quote_activity)
        app = application as App
        toolbar = findViewById(R.id.toolbar)
        authorView = findViewById(R.id.eq_author)
        adderView = findViewById(R.id.eq_adder)
        quoteView = findViewById(R.id.eq_quote)
        saveButton = findViewById(R.id.eq_save)
        statusView = findViewById(R.id.eq_status)
        statusInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        statusOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
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
                            try {
                                val response = Jsoup.connect("http://52.48.142.75:8888/backend/quoter").
                                        data("task", "EDIT").
                                        data("id", quote.id.toString()).
                                        data("edited_by", adder).
                                        data("new_text", text).
                                        data("key", "${PrivateReferences.CODE_PREFIX}$code").post()
                                val json = JsonParser().parse(response.text()).asJsonObject
                                if(json["error"].asBoolean) {
                                    runOnUiThread { statusView.text = "Ошибка!" }
                                    Thread.sleep(1000)
                                    return@Runnable
                                }
                                runOnUiThread {
                                    authorView.setText("")
                                    adderView.setText("")
                                    quoteView.setText("")
                                    statusView.text = "Успешно!!!"
                                }
                                Thread.sleep(1000)
                            }catch (e: Exception) {
                                e.printStackTrace()
                                runOnUiThread { statusView.text = "Ошибка!" }
                                Thread.sleep(1000)
                            }
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