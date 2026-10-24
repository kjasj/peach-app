package video.peach.android

import android.app.Application
import androidx.room.Room
import video.peach.android.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PeachApplication : Application() {
    // 离开 Activity 后仍需完成一次观看上报和播放会话取消；不重放失败请求。
    val outgoing = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var database: PeachDatabase
    lateinit var preferences: Preferences
    lateinit var secrets: SecretStore
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, PeachDatabase::class.java, "peach-client.db").build()
        preferences = Preferences(this)
        secrets = SecretStore(this)
    }
}
