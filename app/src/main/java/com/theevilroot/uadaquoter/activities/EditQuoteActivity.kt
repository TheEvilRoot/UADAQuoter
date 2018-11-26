package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog
import com.theevilroot.uadaquoter.utils.showPINDialog
import daio.io.dresscode.matchDressCode

class EditQuoteActivity : AppCompatActivity() {

    private val authorView by bind<AutoCompleteTextView>(R.id.edit_quote_author)
    private val adderView by bind<EditText>(R.id.edit_quote_adder)
    private val quoteView by bind<EditText>(R.id.edit_quote_quote)
    private val saveButton by bind<Button>(R.id.edit_quote_save)
    private val backButton by bind<ImageButton>(R.id.edit_quote_back_button)
    private val titleView by bind<TextView>(R.id.edit_quote_title_view)
    private val subtitleView by bind<TextView>(R.id.edit_quote_subtitle_view)
    private val personalDataButton by bind<ImageButton>(R.id.edit_quote_personal_data)

    private lateinit var quote: Quote

  /**  @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_quote)

        quote = Quote.fromJson(JsonParser().parse(intent?.extras?.getString("quote")).asJsonObject)
        authorView.setText(quote.author)
        authorView.isEnabled = false
        adderView.setText(QuoterApi.getAdderName(this))
        quoteView.setText(quote.quote)
        saveButton.text = "Сохранить"
        
        saveButton.setOnClickListener {_ ->
            val text = quoteView.text.toString()
            val adder = adderView.text.toString()

            if(quoteView.text.toString() == quote.quote) {
                subtitleView.text = "Что-то изменилось?"
                return@setOnClickListener
            }
            if(text.isBlank() || adder.isBlank()) {
                subtitleView.text = "Заполните все поля!"
                return@setOnClickListener
            }
            showPINDialog(this) { str, dialog, _ ->
                if (str == CODE) {
                    subtitleView.text = "Сохранение..."
                    QuoterApi.edit(quote.id, adder, text, CODE_PREFIX + str, {
                        runOnUiThread { Toast.makeText(this, "Успешно", Toast.LENGTH_SHORT).show() }
                        this.finish()
                    }, {
                        it?.printStackTrace()
                        runOnUiThread { subtitleView.text = "Ошибка" }
                    })
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
    } **/

}