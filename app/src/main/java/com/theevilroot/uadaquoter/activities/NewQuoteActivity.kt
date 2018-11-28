package com.theevilroot.uadaquoter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.utils.DialogCanceledException
import com.theevilroot.uadaquoter.utils.bind
import daio.io.dresscode.matchDressCode
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers

class NewQuoteActivity: AppCompatActivity() {

    private val authorView by bind<AutoCompleteTextView>(R.id.edit_quote_author)
    private val adderView by bind<EditText>(R.id.edit_quote_adder)
    private val quoteView by bind<EditText>(R.id.edit_quote_quote)
    private val saveButton by bind<Button>(R.id.edit_quote_save)
    private val backButton by bind<ImageButton>(R.id.edit_quote_back_button)
    private val titleView by bind<TextView>(R.id.edit_quote_title_view)
    private val subtitleView by bind<TextView>(R.id.edit_quote_subtitle_view)
    private val personalDataButton by bind<ImageButton>(R.id.edit_quote_personal_data)

    private val api = App.instance.api
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_quote)
        init()
    }

    @SuppressLint("CheckResult")
    private fun init() {
        titleView.text = "Добавление цитаты"
        saveButton.text = "Добавить"
        adderView.setText(api.username() ?: "")
        compositeDisposable.add(Observable.combineLatest <CharSequence, CharSequence, CharSequence, Boolean> (
                authorView.textChanges(),
                adderView.textChanges(),
                quoteView.textChanges(), Function3 { author, adder, quote -> author.isNotBlank() && adder.isNotBlank() && quote.isNotBlank() })
                .subscribe(saveButton::setEnabled))

        compositeDisposable.add(saveButton.clicks().subscribe {
            val author = authorView.text.toString()
            val adder = adderView.text.toString()
            val quote = quoteView.text.toString()

            compositeDisposable.add(api.showSecurityDialog(this@NewQuoteActivity, App.instance.references.getPostfix()::equals)
                    .subscribe({ key ->
                        compositeDisposable.add(api.add(App.instance.references.getPrefix() + key, adder, author, quote)
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

    private fun showSuccess() {
        authorView.text.clear()
        adderView.text.clear()
        quoteView.text.clear()
        subtitleView.text = "Успешно"
    }

    private fun showError(t: Throwable) {
        t.printStackTrace()
        subtitleView.text = "Ошибка: ${t::class.java.simpleName}"
    }

    private fun showInvalidKey() {
        subtitleView.text = "Ключ неверен"
    }

    override fun onDestroy() {
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
        super.onDestroy()
    }

    /** @SuppressLint("SetTextI18n")
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
    } **/

}