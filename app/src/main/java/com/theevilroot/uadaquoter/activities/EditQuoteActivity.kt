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
        adderView.setText(quote.adder)
        adderView.isEnabled = false
        quoteView.setText(quote.text)
        saveButton.text = "Сохранить"
        saveButton.setOnClickListener {
            if(quoteView.text.toString() == quote.text) {
                showStatus("Что изменилось?", android.R.color.holo_green_dark, 1000)
                return@setOnClickListener
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // menuInflater.inflate(R.menu.add_quote_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
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