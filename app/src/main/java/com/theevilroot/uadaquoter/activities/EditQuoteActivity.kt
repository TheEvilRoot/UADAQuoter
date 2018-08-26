package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.ColorRes
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
import android.widget.Toast
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.objects.Quote
import kotlin.concurrent.thread
// Change it to com.theevilroot.uadaquoter.references.References.CODE_PREFIX
import com.theevilroot.uadaquoter.references.PrivateReferences.CODE_PREFIX
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog
import com.theevilroot.uadaquoter.utils.showPINDialog

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
        adderView.setText(QuoterAPI.getAdderName(this))
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
            showPINDialog(this) { str, dialog, _ ->
                if (str.length == 4) {
                    val code = str.toIntOrNull() ?: return@showPINDialog
                    if (code == 6741) {
                        showStatus("Сохранение...", android.R.color.holo_green_light, Runnable {
                            QuoterAPI.edit(quote.id, adder, text, CODE_PREFIX + code, {
                                runOnUiThread { Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show() }
                                this.finish()
                            }, {
                                it?.printStackTrace()
                                runOnUiThread { Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show() }
                            })
                        })
                        dialog.dismiss()
                    }
                }
            }
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
                showAdderNameDialog(this, QuoterAPI.getAdderName(this), { editText, textView, alertDialog ->
                    if (editText.text.toString().isBlank()) {
                        textView.text = "Введите что-нибудь, кроме ничего"
                        return@showAdderNameDialog textView.setTextColor(getColor(android.R.color.holo_red_light))
                    }
                    if (QuoterAPI.getAdderName(this) == editText.text.toString())
                        return@showAdderNameDialog alertDialog.dismiss()
                    QuoterAPI.setAdderName(this, editText.text.toString())
                    alertDialog.dismiss()
                }, true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showStatus(msg: String, @ColorRes color: Int, action: Runnable) {
        thread(true) {
            runOnUiThread { if (statusView.visibility == View.VISIBLE) return@runOnUiThread; statusView.text = msg; statusView.setTextColor(resources.getColor(color)); statusView.visibility = View.VISIBLE; statusView.startAnimation(statusInAnimation) }
            action.run()
            runOnUiThread { if (statusView.visibility == View.GONE) return@runOnUiThread; statusView.clearAnimation(); statusView.startAnimation(statusOutAnimation); statusView.visibility = View.GONE }
        }
    }

    private fun showStatus(msg: String, @ColorRes color: Int, time: Long) {
        showStatus(msg, color, Runnable { Thread.sleep(time) })
    }
}