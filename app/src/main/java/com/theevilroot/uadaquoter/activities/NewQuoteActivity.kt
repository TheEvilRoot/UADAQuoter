package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.references.PrivateReferences
import kotlin.concurrent.thread
// Change it to com.theevilroot.uadaquoter.references.References.CODE_PREFIX
// Change it to com.theevilroot.uadaquoter.references.References.CODE
import com.theevilroot.uadaquoter.references.PrivateReferences.CODE_PREFIX
import com.theevilroot.uadaquoter.references.PrivateReferences.CODE
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog
import com.theevilroot.uadaquoter.utils.showPINDialog
import daio.io.dresscode.matchDressCode

class NewQuoteActivity: AppCompatActivity() {

    private val authorView by bind<AutoCompleteTextView>(R.id.edit_quote_author)
    private val adderView by bind<EditText>(R.id.edit_quote_adder)
    private val quoteView by bind<EditText>(R.id.edit_quote_quote)
    private val saveButton by bind<Button>(R.id.edit_quote_save)
    private val backButton by bind<ImageButton>(R.id.edit_quote_back_button)
    private val titleView by bind<TextView>(R.id.edit_quote_title_view)
    private val subtitleView by bind<TextView>(R.id.edit_quote_subtitle_view)
    private val personalDataButton by bind<ImageButton>(R.id.edit_quote_personal_data)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_quote)

        titleView.text = "Добавление цитаты"
        saveButton.text = "Добавить"
        adderView.setText(QuoterApi.getAdderName(this))

        saveButton.setOnClickListener { _ ->
            val author = authorView.text.toString()
            val adder = adderView.text.toString()
            val quote = quoteView.text.toString()

            if(author.isBlank() || adder.isBlank() || quote.isBlank()) {
                subtitleView.text = "Заполните все поля!"
                return@setOnClickListener
            }
            showPINDialog(this) { str, dialog, _ ->
                if (str == PrivateReferences.CODE) {
                    QuoterApi.add(adder, author, quote, CODE_PREFIX + CODE, {
                        runOnUiThread {
                            authorView.setText("")
                            adderView.setText("")
                            quoteView.setText("")
                            subtitleView.text = "Успешно"
                        }
                    }, {
                        it?.printStackTrace()
                        runOnUiThread { subtitleView.text = "Ошибка" }
                    })
                    Thread.sleep(1000)
                    dialog.dismiss()
                }
            }
        }
        personalDataButton.setOnClickListener {
            showAdderNameDialog(this, QuoterApi.getAdderName(this), { editText, textView, alertDialog ->
                if (editText.text.toString().isBlank()) {
                    textView.text = "Введите что-нибудь, кроме ничего"
                    return@showAdderNameDialog textView.setTextColor(resources.getColor(android.R.color.holo_red_light))
                }
                if (QuoterApi.getAdderName(this) == editText.text.toString())
                    return@showAdderNameDialog alertDialog.dismiss()
                QuoterApi.setAdderName(this, editText.text.toString())
                alertDialog.dismiss()
            }, true)
        }
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

}