package com.lvaccaro.lamp.logview

import android.os.FileObserver
import android.util.Log
import androidx.lifecycle.*
import com.lvaccaro.lamp.util.LampKeys
import kotlinx.coroutines.*
import java.io.File
import java.io.LineNumberReader

/**
 * @author https://github.com/vincenzopalazzo
 */
class LogViewModel(private val state: SavedStateHandle) : ViewModel() {

    companion object {
        val TAG = LogViewModel::class.java.canonicalName
    }

    private lateinit var logObserver: FileLogObserver
    private lateinit var logReader: LineNumberReader
    var lastResult = MutableLiveData<String>()
    internal var daemon = MutableLiveData<String>("lightningd")
    //TODO(vincenzopalazzo): Store this data, it is very important for
    //restore the file line when the activity will be destroy
    private var actualLine = 0
    private var stateValue = 0;

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

    internal fun saveState(){
        if(actualLine - stateValue > 50){
            state.set(LampKeys.LOG_POSITION_FILE, actualLine)
            stateValue = actualLine
            Log.d(TAG, "Updated log state with $actualLine")
        }
    }

    /**
     * Cancel all coroutines when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        logObserver.stopWatching()
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
        logObserver = FileLogObserver(path, "${daemon.value}.log", this)
        logObserver.startWatching()
        logReader = LineNumberReader(file.reader())
        Log.d(LogActivity.TAG, "------------------------------------------")
        Log.d(LogActivity.TAG, "File log: ${file.absolutePath}")
        Log.d(LogActivity.TAG, "File dim: ${file.length() / 1048576} Mb")
        Log.d(LogActivity.TAG, "------------------------------------------")
        // if in this method actual line is different from 0, it mean that I'm restoring from a previous session
        if(state.contains(LampKeys.LOG_POSITION_FILE)){
            stateValue = state.get(LampKeys.LOG_POSITION_FILE)!!
            Log.d(TAG, "State value restored $stateValue")
            if(stateValue > actualLine){
                actualLine = stateValue
            }
            Log.d(TAG, "------------- $actualLine ---------------")
        }
        Log.d(TAG, "Restore last position inside the file: $actualLine")
        this.onUIScope(actualLine != 0)
    }

    /**
     * This method is called inside the launch reload, for this reason
     * I need to make a little more work to restore the log position line.
     *
     * Because the actual line is use also
     */
    private fun onUIScope(restore: Boolean = false) {
        uiScope.launch {
            if(restore && actualLine > 300){
                Log.d(TAG, "Jump log inside the file from $actualLine")
                //This should be mean the position inside the file
                //If I'm resoting the file, in most of cases if I'm restoring the position of file
                //the variable actualLine equal to the last line that lamp had read in the previous session
                actualLine = actualLine - 200
                Log.d(TAG, "Restore log file, line in the file ${actualLine}")
            }
            logReader.lineNumber = actualLine
            Log.d(TAG, "Start to read from line ${logReader.lineNumber}")
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
            if (line.length > 700) return@launch
            lastResult.value = line.plus("\n")
        }
        actualLine++
        return true
    }

    fun setLogDaemon(nameDaemon: String) {
        this.daemon.value = nameDaemon
    }

    class FileLogObserver(root: File, val nameFile: String, val viewModel: LogViewModel) :
        FileObserver(root.absolutePath) {
        override fun onEvent(event: Int, path: String?) {
            if (path == null) return
            if (path?.equals(nameFile)) {
                when (event) {
                    MODIFY -> seeChange()
                }
            }
        }

        private fun seeChange() {
            viewModel.onUIScope()
        }
    }
}