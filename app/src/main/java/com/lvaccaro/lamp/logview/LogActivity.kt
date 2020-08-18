package com.lvaccaro.lamp.logview

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir

import kotlinx.android.synthetic.main.activity_log.*

class LogActivity : AppCompatActivity() {

    companion object {
        val TAG = LogActivity::class.java.canonicalName
    }

    private lateinit var logViewModel: LogViewModel
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        editText = findViewById(R.id.editText)
        editText.movementMethod = ScrollingMovementMethod()
        editText.isVerticalScrollBarEnabled = true


        logViewModel = ViewModelProvider(this,
            SavedStateViewModelFactory(this.application, this)).get(LogViewModel::class.java)
        logViewModel.lastResult.observe(this, Observer<String> { lastResult ->
            run {
                editText.append(lastResult)
            }
        })

        logViewModel.daemon.observe(this, Observer<String> { update ->
            run {
                editText.setText("")
                logViewModel.launchReadLog(rootDir())
            }
        })
        //this mean that the
        if(savedInstanceState == null)
            logViewModel.launchReadLog(rootDir())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(::logViewModel.isInitialized)
            logViewModel.saveState()

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_lightning -> {
                showToastMessage("C-lightning log are loading")
                logViewModel.setLogDaemon("lightningd")
                true
            }
            R.id.action_tor -> {
                showToastMessage("Tor log are loading")
                logViewModel.setLogDaemon("tor")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, message, duration).show()
    }
}
