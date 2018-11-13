package com.theevilroot.uadaquoter.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.theevilroot.uadaquoter.*
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.showAdderNameDialog
import daio.io.dresscode.matchDressCode

class MainActivity : AppCompatActivity() {

    private lateinit var app: App
    private lateinit var imm: InputMethodManager
    private lateinit var quotesAdapter: QuotesAdapter

    private val appbar by bind<BottomAppBar>(R.id.app_bar)
    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val loadingProcess by bind<ProgressBar>(R.id.progressBar)
    private val appbarButton by bind<FloatingActionButton>(R.id.app_bar_button)

    private var localLoadingError: Boolean = false
    private var permissionGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)
        app = application as App
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        quotesAdapter = QuotesAdapter()
        quotesView.layoutManager = GridLayoutManager(this, 1)
        quotesView.adapter = quotesAdapter
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_window_close)
        supportActionBar!!.title = "Цитаты"
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= 23)
                return requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 6741)
        }
        onPermissionGranted()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            6741 -> if (grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun loadUserdata() {
        if (QuoterApi.getAdderName(this).isBlank()) {
            showAdderNameDialog(this, "", { editText, textView, alertDialog ->
                if (editText.text.toString().isBlank()) {
                    textView.text = "Введите что-нибудь, кроме ничего"
                    return@showAdderNameDialog textView.setTextColor(resources.getColor(android.R.color.holo_red_light))
                }
                QuoterApi.setAdderName(this, editText.text.toString())
                alertDialog.dismiss()
            }, false)
        }
    }

    private fun showLoading() {
        quotesView.visibility = View.GONE
        loadingProcess.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingProcess.visibility = View.GONE
    }

    private fun showList() {
        quotesView.visibility = View.VISIBLE
        loadingProcess.visibility = View.GONE
    }

    private fun updateUI() {
        runOnUiThread {
            supportActionBar!!.title = getString(R.string.app_name)
            supportActionBar!!.subtitle = "Загружено ${QuoterApi.quotes.size} цитат ${if (QuoterApi.quotes.isNotEmpty() &&  QuoterApi.quotes[0].cached) "из кэша" else ""}"
            quotesAdapter.notifyDataSetChanged()
        }
    }

    private fun loadRemote() {
        QuoterApi.getTotal({ count ->
            QuoterApi.getFromTo(0, count, { quotes ->
                QuoterApi.quotes.clear()
                QuoterApi.quotes.addAll(quotes)
                QuoterApi.saveCache(filesDir, {  }) {  }
                runOnUiThread { showList() }
                updateUI()
            }, {
                onRemoteError(it)
            })
        }) {
            onRemoteError(it)
        }
    }

    private fun onRemoteError(e: Throwable?) {
        Log.e("onRemoteError", "", e)
        if(!localLoadingError) {
            QuoterApi.loadCache(filesDir, { quotes ->
                QuoterApi.quotes.clear()
                QuoterApi.quotes.addAll(quotes)
                QuoterApi.quotes.sortBy { it.id }
                runOnUiThread { showList() }
                updateUI()
            }) {
                it?.printStackTrace()
                localLoadingError = true
            }
        } else {
            runOnUiThread {
                // showStatus("Похоже на проблему с подключением, не находите? Вот: ${e?.localizedMessage}")
                hideLoading()
            }
        }
    }

    private fun load() {
        showLoading()
        loadRemote()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tb_reload -> if (permissionGranted) {
                load()
            } else {
                if (Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 6741)
                    return true
                }
                val result = arrayOf(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE))
                if (result.all { it == PermissionChecker.PERMISSION_GRANTED }) {
                   onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
            R.id.tb_add -> {
               startActivity(Intent(this, NewQuoteActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onPermissionGranted() {
        load()
        loadUserdata()
        permissionGranted = true
    }

    private fun onPermissionDenied() {
        hideLoading()
        permissionGranted = false
        // showStatus("У приложения нет доступа к хранилищу на устройстве что-бы сохранять кэш и данные пользователя. Нажмите на кнопку 'Обновить' сверху экрана, если это не поможет, то необходимо дать данные права через настройки устройства!")
    }

}