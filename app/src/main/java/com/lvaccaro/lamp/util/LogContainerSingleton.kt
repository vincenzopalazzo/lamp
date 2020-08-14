package com.lvaccaro.lamp.util

import java.util.*

/**
 * @author https://github.com/vincenzopalazzo
 */
class LogContainerSingleton {

    companion object {
        val instance = LogContainerSingleton()
    }

    private val logLineQueue: Queue<String> = ArrayDeque<String>()
    private var buffer = 100

    fun addLine(line: String) {
        if (logLineQueue.size == buffer) {
            //remove the first element and insert the new line at the end
            logLineQueue.remove()
        }
        logLineQueue.add(line)
    }

    fun getLines(): Queue<String> {
        return logLineQueue
    }

    fun isEmpty(): Boolean {
        return logLineQueue.isEmpty()
    }

    override fun toString(): String {
        val buffer = StringBuffer()
        val iterator = logLineQueue.iterator()
        iterator.forEach { it -> buffer.append(it).append("\n") }
        return buffer.toString()
    }
}