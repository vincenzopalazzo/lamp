package com.lvaccaro.lamp.logview

import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.rootDir

import kotlinx.android.synthetic.main.activity_log.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import java.io.*
import java.util.stream.Collectors

class LogActivity : AppCompatActivity() {

    companion object {
        val TAG = LogActivity::class.java.canonicalName
    }

    private var daemon = "lightningd"
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


        logViewModel = ViewModelProvider(this).get(LogViewModel::class.java)
        logViewModel.lastResult.observe(this, Observer<String> { lastResult ->
            run {
                editText.append(lastResult)
            }
        })

        logViewModel.launchReadLog(rootDir().path.plus("/$daemon.log"))
    }

    override fun onResume() {
        super.onResume()
//        readLog()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_lightning -> {
                daemon = "lightningd"
  //              readLog()
                true
            }
            R.id.action_tor -> {
                daemon = "tor"
    //            readLog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun readLog() {
        title = "Log $daemon"
        val logFile = File(rootDir(), "$daemon.log")
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found", Toast.LENGTH_LONG).show()
            return
        }

        editText.setText("Waiting log")

        doAsync {
            val stream = LineNumberReader(logFile.reader())
            stream.forEachLine {
                runOnUiThread {
                    editText.append(it)
                }
            }

            onComplete {
                showToastMessage("Log loaded")
            }
        }

        /*
        val loadLogTask = LoadLogTask(this, et)
        loadLogTask.execute(logFile)
        */
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, message, duration).show()
    }

    private class LoadLogTask(val activity: LogActivity, val editText: EditText) :
        AsyncTask<File, String, String>() {

        override fun doInBackground(vararg params: File?): String? {
            var text: String
            var logReader = LineNumberReader(params[0]?.reader())
            var lines: List<String> = logReader.lines().collect(Collectors.toList())
            text = readBuffer(lines)
            return text
        }

        override fun onPreExecute() {
            super.onPreExecute()
            activity.showToastMessage("Loading log")
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            editText.setText(result)
            activity.showToastMessage("Log ready")
        }

        fun readBuffer(lines: List<String>): String {
            val sb = StringBuilder()
            val linesIt = lines.iterator()
            while (linesIt.hasNext()) {
                sb.append(linesIt.next())
            }
            return sb.toString()
        }
    }
}
