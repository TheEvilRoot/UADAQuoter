package com.theevilroot.uadaquoter.activities

import android.Manifest
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.nekocode.badge.BadgeDrawable
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jetradar.permissions.PermissionsDeniedException
import com.theevilroot.uadaquoter.App
import com.theevilroot.uadaquoter.R
import com.theevilroot.uadaquoter.adapters.MessagesAdapter
import com.theevilroot.uadaquoter.adapters.QuotesAdapter
import com.theevilroot.uadaquoter.objects.Message
import com.theevilroot.uadaquoter.objects.MessageAction
import com.theevilroot.uadaquoter.objects.Quote
import com.theevilroot.uadaquoter.utils.DialogCanceledException
import com.theevilroot.uadaquoter.utils.bind
import com.theevilroot.uadaquoter.utils.log
import daio.io.dresscode.dressCodeStyleId
import daio.io.dresscode.matchDressCode
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    private lateinit var imm: InputMethodManager
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var messagesAdapter: MessagesAdapter

    private val appbar by bind<BottomAppBar>(R.id.app_bar)
    private val quotesView by bind<RecyclerView>(R.id.quotes_view)
    private val loadingProcess by bind<ProgressBar>(R.id.progressBar)
    private val appbarButton by bind<FloatingActionButton>(R.id.app_bar_button)
    private val messagesView by bind<RecyclerView>(R.id.messages_view)
    private val loadingView by bind<View>(R.id.loading_view)

    private var permissionGranted: Boolean = false

    private val permissionsDelegate = App.instance.permissionsActivityDelegate
    private val api = App.instance.api
    private val compositeDisposable = CompositeDisposable()

    private val mIdUserdataNotSpecified = 1
    private val mIdPermissionDenied = 2
    private val mIdNetworkError = 3
    private val mIdFirstRun = 4
    private val mIdQuotesLoaded = 5
    private val mIdServiceUnavailable = 6
    private val mIdCacheError = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)
        permissionsDelegate.attach(this)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setupQuotesView()
        setupMessagesView()

        checkPermissions()
    }

    /** Main logic stuff **/

    private fun checkPermissions() =
            compositeDisposable.add(App.instance.butler
                    .require(true,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        requestUserdata()
                        init()
                    }) {
                        if (it is PermissionsDeniedException) {
                            addPermissionDeniedMessage()
                            init()
                        } else addErrorMessage(it)
                    })

    private fun requestUserdata() {
        if (api.username() == null)
            compositeDisposable.add(api.requestUserdata(this, "")
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe({ api.username(it) }, {
                        if (it !is DialogCanceledException)
                            addErrorMessage(it)
                        addUserdataNotSpecifiedMessage()
                    }))
    }

    private fun init() {
        showLoading()
        compositeDisposable.add(checkConnection()
                .subscribe(this::checkService) {
                    if (it is NetworkErrorException)
                        addNetworkErrorMessage()
                    else addErrorMessage(it)
                    loadFromCache()
                })
    }

    private fun checkService() {
        compositeDisposable.add(api.isServiceAvailable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFromServer) {
                    addServiceUnavailableMessage()
                    loadFromCache()
                    it.printStackTrace()
                })
    }

    private fun loadFromCache() {
        clearQuotesDatabase(true)
        compositeDisposable.add(api.getCache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    addQuote(it)
                }, {
                    hideLoading()
                    if (it is FileNotFoundException) {
                        addFirstRunMessage()
                    } else addErrorMessage(it)
                }) {
                    addQuotesLoadedMessage()
                    hideLoading()
                })
    }

    private fun loadFromServer() {
        clearQuotesDatabase(true)
        compositeDisposable.add(api.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    addQuote(it)
                }, {
                    addErrorMessage(it)
                    loadFromCache()
                }) {
                    hideLoading()
                    addQuotesLoadedMessage()
                    api.setCache(App.instance.quotes)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                log("Cache saved")
                            }, this::addCacheErrorMessage)
                })
    }

    private fun checkConnection() = Completable.create {
        val service = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = service.activeNetworkInfo
        if (activeNetwork.isConnected)
            it.onComplete()
        else it.onError(NetworkErrorException())
    }

    private fun addQuote(quote: Quote) {
        App.instance.quotes.add(quote)
        quotesAdapter.notifyItemInserted(App.instance.quotes.size - 1)
    }

    private fun clearQuotesDatabase(notifyAdapter: Boolean) {
        if (App.instance.quotes.isNotEmpty()) {
            App.instance.quotes.clear()
            if (notifyAdapter)
                quotesAdapter.notifyDataSetChanged()
        }
    }

    /** Messages stuff **/

    private fun addUserdataNotSpecifiedMessage() {
        addMessage("UserdataNotSpecified",
                "UserdataNotSpecifiedMessage",
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdUserdataNotSpecified)
    }

    private fun addErrorMessage(t: Throwable) {
        t.printStackTrace()
        addMessage("Error", t::class.java.simpleName, android.R.color.holo_red_dark, R.drawable.ic_trash_can)
    }

    private fun addPermissionDeniedMessage() {
        addMessage("PermissionDenied",
                "PermissionDeniedMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdPermissionDenied)
    }

    private fun addNetworkErrorMessage() {
        addMessage("NetworkError",
                "NetworkErrorMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdNetworkError)
    }

    private fun addFirstRunMessage() {
        addMessage("FirstRun",
                "FirstRunMessage",
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdFirstRun)
    }

    private fun addQuotesLoadedMessage() {
        addMessage("QuotesLoaded",
                App.instance.quotes.count().toString(),
                android.R.color.holo_green_dark,
                R.drawable.ic_trash_can,
                id = mIdQuotesLoaded)
    }

    private fun addServiceUnavailableMessage() {
        addMessage("ServiceUnavailable",
                "ServiceUnavailableMessage",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdServiceUnavailable)
    }

    private fun addCacheErrorMessage(t: Throwable) {
        addMessage("CacheError",
                "CacheErrorMessage\n${t.localizedMessage}",
                android.R.color.holo_red_dark,
                R.drawable.ic_trash_can,
                id = mIdCacheError)
    }

    private fun addMessage(title: String,
                           message: String,
                           @ColorRes color: Int,
                           @DrawableRes icon: Int,
                           actions: List<MessageAction> = listOf(MessageAction()),
                           id: Int? = null) {
        messagesAdapter.addMessage(Message(title, message, color, icon, actions, id))
    }


    /** UI setting up stuff **/

    private fun setupMessagesView() {
        messagesAdapter = MessagesAdapter(this::updateMessagesBadge)
        messagesView.layoutManager = GridLayoutManager(this, 1)
        messagesView.adapter = messagesAdapter
        ItemTouchHelper(object: ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean =
                    true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                messagesAdapter.dismissMessage(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(messagesView)
    }

    private fun setupQuotesView() {
        quotesAdapter = QuotesAdapter()
        quotesView.layoutManager = GridLayoutManager(this, 1)
        quotesView.adapter = quotesAdapter
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingView.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tb_reload -> checkPermissions()
            R.id.tb_add -> {
                startActivity(Intent(this, NewQuoteActivity::class.java))
            }
            R.id.tb_test_light -> {
                dressCodeStyleId = R.style.AppTheme_UADAFLight
                matchDressCode()
            }
            R.id.tb_test_dark -> {
                dressCodeStyleId = R.style.AppTheme_UADAFDark
                matchDressCode()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateMessagesBadge() {
        val count = messagesAdapter.messagesCount()
        if (count != 0) {
            appbar.navigationIcon = BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_NUMBER)
                    .badgeColor(resources.getColor(android.R.color.holo_red_dark))
                    .number(count)
                    .build()
        } else appbar.navigationIcon = null
    }

    /** System stuff **/

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
        permissionsDelegate.detach()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return true
    }

   /** override fun onCreate(savedInstanceState: Bundle?) {
        matchDressCode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // ================================================================

        setupQuotesView()
        setupMessagesView()
        checkPermission()


        // ================================================================

        permissionsDelegate.attach(this)
        compositeDisposable.add(App.instance.butler.require(true,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({

                }) {

                })


    }

    private fun checkPermission() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= 23)
                return requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 6741)
        }
        onPermissionGranted()
    }

    private fun setupMessagesView() {
        messagesAdapter = MessagesAdapter()
        messagesView.layoutManager = GridLayoutManager(this, 1)
        messagesView.adapter = messagesAdapter
        ItemTouchHelper(object: ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean =
                    false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                messagesAdapter.dismissMessage(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(messagesView)
    }

    private fun setupQuotesView() {
        quotesAdapter = QuotesAdapter()
        quotesView.layoutManager = GridLayoutManager(this, 1)
        quotesView.adapter = quotesAdapter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        // =================================================================================

        when (requestCode) {
            6741 -> if (grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

        // =================================================================================

        permissionsDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onDestroy() {
        super.onDestroy()
        permissionsDelegate.detach()
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
        QuoterApi.loadCache(filesDir, { quotes ->
            QuoterApi.quotes.clear()
            QuoterApi.quotes.addAll(quotes)
            QuoterApi.quotes.sortBy { it.id }
            runOnUiThread { showList() }
            updateUI()
        }) {

            runOnUiThread(this::hideLoading)
            if (it is FileNotFoundException) {
                messagesAdapter.addMessage(Message("Первый запуск?",
                        "Похоже у вас нет загруженных цитат, а сеть недоступна. Включите передачу данных или Wi-Fi что бы загрузить последние цитаты.",
                        android.R.color.holo_green_dark,
                        R.drawable.ic_trash_can, listOf(
                        MessageAction("Включить Wi-Fi", MessageActionType.TYPE_ACTION, action = { context ->
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            return@MessageAction true
                        }),
                        MessageAction("Попробовать снова", MessageActionType.TYPE_ACTION, action = { _ ->
                            reload()
                            return@MessageAction true
                        }),
                        MessageAction()
                )))
            }else {
                messagesAdapter.addMessage(Message("Ошибка получение цитат", "Неудалось получить ни кэш цитат, ни обновить цитаты с сервера.\n${e?.localizedMessage}\n${it?.localizedMessage}", android.R.color.holo_red_dark, R.drawable.ic_trash_can, listOf(
                        MessageAction("Попробовать снова", MessageActionType.TYPE_ACTION, action = { _ ->
                            load()
                            return@MessageAction true
                        }),
                        MessageAction("Отправить отчет", MessageActionType.TYPE_ACTION, action = { context ->
                            val intent = Intent(Intent.ACTION_SENDTO)
                            intent.type = "quote/plain"
                            intent.putExtra(Intent.EXTRA_SUBJECT, "UADAQuoter report")
                            intent.putExtra(Intent.EXTRA_TEXT, e?.message )
                            intent.data = Uri.parse("mailto:theevilroot6741@gmail.com")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            return@MessageAction false
                        }),
                        MessageAction()
                )))
            }
        }
    }

    private fun load() {
        showLoading()
        loadRemote()
    }

    private fun reload(): Boolean {
        if (permissionGranted) {
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
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tb_reload -> reload()
            R.id.tb_add -> {
               startActivity(Intent(this, NewQuoteActivity::class.java))
            }
            R.id.tb_test_light -> {
                dressCodeStyleId = R.style.AppTheme_UADAFLight
                matchDressCode()
            }
            R.id.tb_test_dark -> {
                dressCodeStyleId = R.style.AppTheme_UADAFDark
                matchDressCode()
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
        messagesAdapter.addMessage(Message("Ошибка доступа", "У приложения нет доступа к хранилищу на устройстве что-бы сохранять кэш и данные пользователя.", android.R.color.holo_red_dark, R.drawable.ic_trash_can, listOf(
                MessageAction("Попробовать снова", MessageActionType.TYPE_ACTION, action = { _ ->
                    reload()
                })
        )))
    }

    private fun addPermissionError() {
        messagesAdapter.addMessage(Message("Ошибка доступа", "У приложения нет доступа к хранилищу на устройстве что-бы сохранять кэш и данные пользователя.", android.R.color.holo_red_dark, R.drawable.ic_trash_can, listOf(
                MessageAction("Попробовать снова", MessageActionType.TYPE_ACTION, action = { _ ->

                })
        )))
    } **/

}