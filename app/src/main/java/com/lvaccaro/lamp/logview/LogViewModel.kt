package com.lvaccaro.lamp.logview

import android.os.FileObserver
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.io.File
import java.io.LineNumberReader

class LogViewModel : ViewModel() {

    private lateinit var logObserver: FileLogObserver
    private lateinit var logReader: LineNumberReader
    var lastResult = MutableLiveData<String>()
    internal var daemon = MutableLiveData<String>("lightningd")
    //TODO(vincenzopalazzo): Store this data, it is very important for
    //restore the file line when the activity will be destroy
    private var actualLine = 0

    /**
     * This is the job for all coroutines started by this ViewModel.
     * Cancelling this job will cancel all coroutines started by this ViewModel.
     */
    private val viewModelJob = SupervisorJob()

    /**
     * This is the main scope for all coroutines launched by MainViewModel.
     * Since we pass viewModelJob, you can cancel all coroutines
     * launched by uiScope by calling viewModelJob.cancel()
     */
    private val uiScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    /**
     * Cancel all coroutines when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /**
     * Heavy operation that cannot be done in the Main Thread
     */
    fun launchReadLog(path: File) {
        val file = File(path, "${daemon.value}.log")
        if (!file.exists()) {
            lastResult.value = "Log file not found"
            return
        }
        logObserver = FileLogObserver(file, this)
        logReader = LineNumberReader(file.reader())
        Log.d(LogActivity.TAG, "------------------------------------------")
        Log.d(LogActivity.TAG, "File log: ${file.absolutePath}")
        Log.d(LogActivity.TAG, "File dim: ${file.length() / 1048576} Mb")
        Log.d(LogActivity.TAG, "------------------------------------------")
        onUIScope()
    }

    private fun onUIScope(){
        uiScope.launch {
            logReader.lineNumber = actualLine
            readLog()
        }
    }

    private suspend fun readLog() {
        withContext(Dispatchers.IO) {
            while (readByStep()) {
                delay(50)
            }
        }
    }

    private fun readByStep(): Boolean {
        val line: String = logReader.readLine() ?: return false
        viewModelScope.launch {
            // with log level to IO, esplora generate a lot of log like hex string
            //This don't have send for the user, and also we need to resolve this
            if(line.length > 700) return@launch
            lastResult.value = line.plus("\n")
        }
        actualLine++
        return true
    }

    fun setLogDaemon(nameDaemon: String){
        this.daemon.value = nameDaemon
    }

    class FileLogObserver(val logFile: File, val viewModel: LogViewModel): FileObserver(logFile.path){
        override fun onEvent(event: Int, path: String?) {
            if (path == null) return
            if (path?.equals(logFile.name)) {
                when (event) {
                    FileObserver.MODIFY -> seeChange()
                }
            }
        }
        private fun seeChange() {
            viewModel.onUIScope()
        }
    }
}