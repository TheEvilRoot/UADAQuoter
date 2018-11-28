package com.theevilroot.uadaquoter.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonParser
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.DialogCanceledException
import com.theevilroot.uadaquoter.utils.bind
import daio.io.dresscode.matchDressCode
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class EditQuoteActivity : AppCompatActivity() {

    private val authorView by bind<AutoCompleteTextView>(R.id.edit_quote_author)
    private val adderView by bind<EditText>(R.id.edit_quote_adder)
    private val quoteView by bind<EditText>(R.id.edit_quote_quote)
    private val saveButton by bind<Button>(R.id.edit_quote_save)
    private val backButton by bind<ImageButton>(R.id.edit_quote_back_button)
    private val titleView by bind<TextView>(R.id.edit_quote_title_view)
    private val subtitleView by bind<TextView>(R.id.edit_quote_subtitle_view)
    private val personalDataButton by bind<ImageButton>(R.id.edit_quote_personal_data)

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val api = App.instance.api

    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_quote)

        compositeDisposable.add(extractQuote()
                .subscribe(this::initViewsWithQuote, this::onParseError))

    }

    private fun initViewsWithQuote(quote: Quote) {
        authorView.setText(quote.author)
        authorView.isEnabled = false
        adderView.setText(api.username() ?: "")
        quoteView.setText(quote.quote)
        titleView.append(" #${quote.id}")
        saveButton.text = "Сохранить"

        compositeDisposable.add(Observable.combineLatest <CharSequence, CharSequence, Boolean> (
                adderView.textChanges(),
                quoteView.textChanges(), BiFunction<CharSequence,CharSequence ,Boolean> { adder, text -> adder.isNotBlank() && text.isNotBlank()  })
                .subscribe(saveButton::setEnabled))

        compositeDisposable.add(saveButton.clicks().subscribe {
            val editor = adderView.text.toString()
            val text = quoteView.text.toString()

            compositeDisposable.add(api.showSecurityDialog(this@EditQuoteActivity, App.instance.references.getPostfix()::equals)
                    .subscribe({ key ->
                        compositeDisposable.add(api.edit(App.instance.references.getPrefix() + key, quote.id, editor, text)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(this::showSuccess, this::showError))
                    }, { throwable ->
                        if (throwable is DialogCanceledException)
                            showInvalidKey()
                        else showError(throwable)
                    }))
        })
    }

    private fun showInvalidKey() {
        subtitleView.text = "Ключ неверен"
    }

    private fun showSuccess() {
        subtitleView.text = "Успешно"
    }

    private fun showError(t: Throwable) {
        t.printStackTrace()
        subtitleView.text = "Ошибка: ${t::class.java.simpleName}"
    }

    private fun onParseError(t: Throwable) {
        Toast.makeText(this, "Неудалось получить цитату", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun extractQuote(): Single<Quote> = Single.create <Quote> {
        val quote = try {
            Quote.fromJson(JsonParser().parse(intent?.extras?.getString("quote")).asJsonObject)
        } catch (e: Exception) {
            return@create it.onError(e)
        }

        it.onSuccess(quote)
    }

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