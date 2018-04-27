package com.theevilroot.uadaquoter.activities

import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.TextWatcherWrapper
import me.philio.pinentry.PinEntryView
import org.jsoup.Jsoup
import kotlin.concurrent.thread
// Change it to com.theevilroot.uadaquoter.References.CODE_PREFIX
import com.theevilroot.uadaquoter.PrivateReferences.CODE_PREFIX
import java.io.File

class NewQuoteActivity: AppCompatActivity() {

    lateinit var app: App
    lateinit var toolbar: Toolbar
    lateinit var authorView: EditText
    lateinit var adderView: EditText
    lateinit var quoteView: EditText
    lateinit var addButton: Button
    lateinit var statusView: TextView

    lateinit var statusInAnimation: Animation
    lateinit var statusOutAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_quote_activity)
        app = application as App
        toolbar = findViewById(R.id.toolbar)
        authorView = findViewById(R.id.aq_author)
        adderView = findViewById(R.id.aq_adder)
        quoteView = findViewById(R.id.aq_quote)
        addButton = findViewById(R.id.aq_add)
        statusView = findViewById(R.id.aq_status)
        statusInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        statusOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addButton.setOnClickListener {
            val author = authorView.text.toString()
            val adder = adderView.text.toString()
            val quote = quoteView.text.toString()
            if(author.isBlank() || adder.isBlank() || quote.isBlank()) {
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
                        showStatus("Добавление...", android.R.color.holo_green_light, Runnable {
                            try {
                                val response = Jsoup.connect("http://52.48.142.75/backend/Quoter.php").data("task", "ADD").data("addby", adder).data("author", author).data("quote", quote).data("key", "${CODE_PREFIX}${code}").post()
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
                TODO("Доделать завтра! Уже галава кепит сегодня!")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showStatus(msg: String, @ColorRes color: Int, action: Runnable) {
        thread(true) {
            runOnUiThread {if(statusView.visibility == View.VISIBLE) return@runOnUiThread ; statusView.text = msg ; statusView.setTextColor(getColor(color)) ; statusView.visibility = View.VISIBLE; statusView.startAnimation(statusInAnimation) }
            action.run()
            runOnUiThread { if(statusView.visibility == View.GONE) return@runOnUiThread ; statusView.clearAnimation() ; statusView.startAnimation(statusOutAnimation) ; statusView.visibility = View.GONE }
        }
    }

    private fun showStatus(msg: String, @ColorRes color: Int, time: Long) {
        showStatus(msg, color, Runnable { Thread.sleep(time) })
    }

}