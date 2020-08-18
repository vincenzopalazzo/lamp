package com.lvaccaro.lamp.util

class LampKeys {
    companion object{
        const val ADDRESS_KEY = "ADDRESS_KEY"
        const val AMOUNT_KEY = "amount"
        const val LABEL_KEY = "label"
        const val MESSAGE_KEY = "message"
        const val WITHDRAW_COMMAND = "withdraw"
        const val DECODEPAY_COMMAND = "decodepay"
        const val CONNECT_COMMAND = "connect"
        const val MESSAGE_JSON_KEY = "message"

        //Key notification
        const val NODE_NOTIFICATION_SHUTDOWN: String = "SHUTDOWN_NODE_NOTIFICATION"
        const val NODE_NOTIFICATION_FUNDCHANNEL: String = "NODE_NOTIFICATION_FUNDCHANNEL"

        //Key store data
        const val LOG_POSITION_FILE: String = "LOG_POSITION_FILE"
    }
}