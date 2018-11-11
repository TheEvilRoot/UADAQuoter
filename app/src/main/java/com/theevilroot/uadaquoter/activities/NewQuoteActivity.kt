package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.theevilroot.uadaquoter.*
import kotlin.concurrent.thread
// Change it to com.theevilroot.uadaquoter.references.References.CODE_PREFIX
import com.theevilroot.uadaquoter.references.PrivateReferences.CODE_PREFIX
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog
import com.theevilroot.uadaquoter.utils.showPINDialog

class NewQuoteActivity: AppCompatActivity() {

    private lateinit var app: App

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val authorView by bind<AutoCompleteTextView>(R.id.eq_author)
    private val adderView by bind<EditText>(R.id.eq_adder)
    private val quoteView by bind<EditText>(R.id.eq_quote)
    private val addButton by bind<Button>(R.id.eq_save)
    private val statusView by bind<TextView>(R.id.eq_status)

    private val statusInAnimation: Animation by lazy { AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left) }
    private val statusOutAnimation: Animation by lazy { AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_quote)
        app = application as App
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addButton.setOnClickListener {
            val author = authorView.text.toString()
            val adder = adderView.text.toString()
            val quote = quoteView.text.toString()
            if(author.isBlank() || adder.isBlank() || quote.isBlank()) {
                return@setOnClickListener showStatus("Заполните все поля!", android.R.color.holo_red_light, 1000)
            }
            showPINDialog(this) { str, dialog, _ ->
                if (str.length == 4) {
                    val code = str.toIntOrNull() ?: return@showPINDialog
                    if (code == 6741) {
                        showStatus("Добавление...", android.R.color.holo_green_light, Runnable {
                            QuoterApi.add(adder, author, quote, "$CODE_PREFIX$code", {
                                runOnUiThread {
                                    authorView.setText("")
                                    adderView.setText("")
                                    quoteView.setText("")
                                    statusView.text = "Успешно"
                                }
                            }, {
                                runOnUiThread { statusView.text = "Ошибка: ${it?.localizedMessage}" }
                            })
                            Thread.sleep(1000)
                        })
                        dialog.dismiss()
                    }
                }
            }
        }
        adderView.setText(QuoterApi.getAdderName(this))
        authorView.setAdapter(ArrayAdapter<String>(this, R.layout.item_author, QuoterApi.quotes.asSequence().map { it.author }.distinct().toList().toTypedArray()))
        authorView.setDropDownBackgroundResource(R.drawable.background_text_edit)
        authorView.dropDownVerticalOffset = 8
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_quote, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.tb_personal_data -> {
                showAdderNameDialog(this, QuoterApi.getAdderName(this), { editText, textView, alertDialog ->
                    if (editText.text.toString().isBlank()) {
                        textView.text = "Введите что-нибудь, кроме ничего"
                        return@showAdderNameDialog textView.setTextColor(resources.getColor(android.R.color.holo_red_light))
                    }
                    if (QuoterApi.getAdderName(this) == editText.text.toString())
                        return@showAdderNameDialog alertDialog.dismiss()
                    showStatus("Изменено", android.R.color.holo_green_light, Runnable {
                        QuoterApi.setAdderName(this, editText.text.toString())
                        Thread.sleep(1000)
                    })
                    alertDialog.dismiss()
                }, true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showStatus(msg: String, @ColorRes color: Int, action: Runnable) {
        thread(true) {
            runOnUiThread {if(statusView.visibility == View.VISIBLE) return@runOnUiThread ; statusView.text = msg ; statusView.setTextColor(resources.getColor(color)) ; statusView.visibility = View.VISIBLE; statusView.startAnimation(statusInAnimation) }
            action.run()
            runOnUiThread { if(statusView.visibility == View.GONE) return@runOnUiThread ; statusView.clearAnimation() ; statusView.startAnimation(statusOutAnimation) ; statusView.visibility = View.GONE }
        }
    }

    private fun showStatus(msg: String, @ColorRes color: Int, time: Long) {
        showStatus(msg, color, Runnable { Thread.sleep(time) })
    }

}