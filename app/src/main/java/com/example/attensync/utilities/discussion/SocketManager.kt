package com.example.attensync.utilities.discussion

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketManager {

    private var mSocket: Socket? = null
    // Replace with your server URL
    private const val SERVER_URL = "http://10.0.2.2:3000" 

    fun getSocket(): Socket {
        if (mSocket == null) {
            try {
                mSocket = IO.socket(SERVER_URL)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
        return mSocket!!
    }

    fun establishConnection() {
        mSocket?.connect()
    }

    fun closeConnection() {
        mSocket?.disconnect()
    }
}
