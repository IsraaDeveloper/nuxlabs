package com.israadev.nuxlauncher.core.skin

object OfflineSkinServerManager {
    private var instance: OfflineSkinServer? = null

    @Synchronized
    fun getOrCreateServer(): OfflineSkinServer {
        if (instance == null) {
            instance = OfflineSkinServer()
        }
        return instance!!
    }

    @Synchronized
    fun stopServer() {
        instance?.stop()
        instance = null
    }
}
