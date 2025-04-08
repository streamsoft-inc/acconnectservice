# ACConnectService
ACConnect service is a demo application that shows how to broadcast AC Connect service and communicate with exoplayer using **AC Connect** protocol developed by **Streamsoft Inc**. It allows you to connect to embedded server, load playlists and control the playback via commands, remotely. This application provides a modern, lightweight API designed for apps that need to interface with Artist Connection devices.

🧾 **Official protocol documentation:** [https://docs.artistconnection.net/acconnect](https://docs.artistconnection.net/acconnect)
📦 **Application GitHub repository:** [[https://github.com/streamsoft-inc/acconnectservice.git](https://github.com/streamsoft-inc/acconnectservice.git)]

---

## 🚀 Features

- 🔍 Emit services such as `_artist_connection._tcp.` and `_acconnect_streaming._tcp.`
- 📡 Send service status
- 🔌 Connect and disconnect from devices
- 🔁 Load playlist and control the playback with commands (play, stop, previous, next, seek, mute, setVolume)
- 📱 Includes fully functional sample Android app

---

## 🛠 Requirements

- Android 7.0+
- Kotlin 1.8.0+
- AGP 8.1+
---

## 📦 Installation

1. Clone the repo  
   ```bash
   git clone https://github.com/streamsoft-inc/acconnectservice.git
2. Open with Android Studio
3. Sync Gradle
4. Run the app on a device or emulator (API 24+)

---

## 🔄 How It Works

1. **Client app** discovers `_artist_connection._tcp.` services using mDNS (Bonjour)
2. **ACConnectService** acts as a **local server**:
   - Accepts connections
   - Handles playback commands
   - Controls the ExoPlayer instance
3. Communication is done via HTTP REST using the AC Connect protocol

## 🚀 Getting Started

### 📡 Start running service

```kotlin
  connectController.start(this)
```

Implement callbacks:

```kotlin
private fun success(mediaCommands: MediaCommands) {
        MainThreadDispatcher.post {
            callback.invoke(HttpMediaServerEvent(Result.success(mediaCommands)))
        }
    }

    private fun error(throwable: Throwable) {
        callback.invoke(HttpMediaServerEvent(Result.failure<Throwable>(throwable)))
    }

          post(endpoints.connect) {
                        try {
                            println("[ACC] server - connect")
                            val connectDTO = call.receive<ConnectDTO>()
                            success(MediaCommands.Connect(connectDTO))
                            connected = true
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    post(endpoints.disconnect){
                        println("[ACC] server - disconnect")
                        connected = false
                        success(MediaCommands.Disconnect())
                        call.respond(HttpStatusCode.OK)
                    }
                    get(endpoints.status) {
                        println("[ACC] server call - status")
                        try {
                            val status = requestCallbackChannel.status()
                            status?.let {
                                call.respond(HttpStatusCode.OK, status)
                            } ?: call.respond(HttpStatusCode.OK, DeviceStatusDTO("1", StatusType.ENDED, 0.0f))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            call.respond(HttpStatusCode.BadRequest, ErrorResponceDTO("Error, unable to get current status.", code = null))
                        }
                    }
```

and other ones for playback handling.

Handle callbacks and manage playback:

```kotlin
fun load(playlist: List<PlaylistItemDTO>) {
        buildMetadata(playlist).let { tracks ->
            prepareMetadata(tracks)

            mContext?.let {
                val intent = Intent(mContext, FullScreenActivity::class.java)
                val urls = tracks.tracks.map { it.url }
                intent.putStringArrayListExtra("VIDEO_URLS", ArrayList(urls))
                it.startActivity(intent)

                play(tracks.tracks.first().mediaId)
            }
        }
    }

    fun pause() {
        player.playWhenReady = false
        player.pause()
    }

    fun play(mediaId: String) {
        currentMetadata?.tracks?.indexOfFirst { item -> mediaId == item.mediaId }?.takeIf { it != -1 }?.let {
            val index = player.currentMediaItemIndex
            if (index != it) player.seekTo(it, C.TIME_UNSET)
            player.playWhenReady = true
        }
    }

    fun previous() {
        player.seekToPrevious()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            player.playWhenReady = true
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun stop() {
        player.stop()
        player.release()
        _stopEvent.postValue(true)
    }

    fun mute(mute: Boolean) {
        player.volume = if (mute) 0f else 1f
    }
```
---

## 🎷 Example Usage: Media Commands

Load sample playback list:

```kotlin
curl -v -X POST http://10.10.20.54:41693/media/load/playlist \
    -H "Content-Type: application/json" \
    -d '        [
    {
        "id": "1",
        "url": "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        "type": "audio",
        "duration": "3:45",
        "metadata": {
            "title": "Song Title",
            "artistName": "Artist Name",
            "albumName": "Album Name",
            "artworkUrl": "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "format": "mp4"
        }
    },
    {
        "id": "2",
        "url": "https://maitv-vod.lab.eyevinn.technology/VINN.mp4/master.m3u8",
        "type": "video",
        "duration": "10:30",
        "metadata": {
            "title": "VINN Stream",
            "artistName": "Eyewinn Labs",
            "albumName": "Live Streams",
            "artworkUrl": "https://example.com/artwork_vinn.jpg",
            "format": "mp4"
        }
    },
    {
        "id": "5",
        "url": "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        "type": "video",
        "duration": "5:00",
        "metadata": {
            "title": "Test Stream",
            "artistName": "Mux Inc.",
            "albumName": "Test Streams",
            "artworkUrl": "https://example.com/artwork_teststream.jpg",
            "format": "mp4"
        }
    }
]'
```

Play next:

```kotlin
curl -v -X POST http://10.10.20.54:45109/media/next
```

Play previous:

```kotlin
curl -v -X POST http://10.10.20.54:45109/media/previous
```

Mute:

```kotlin
curl -v -X POST http://10.10.20.54:45109/media/mute \
    -H "Content-Type: application/json" \
    -d '    
    {
        "value": "true"
    }'
```

Set volume:

```kotlin
 curl -v -X POST http://10.10.20.54:45109/media/volume \
    -H "Content-Type: application/json" \
    -d '    
    {
        "value": "20.0"
    }'
```

Seek to:

```kotlin
curl -v -X POST http://10.10.20.54:45109/media/move \
    -H "Content-Type: application/json" \
    -d '    
    {
        "position": "100.0"
    }
    '
```

Pause:

```kotlin
curl -v -X POST http://10.10.20.54:41693/media/pause \
```

Stop:

```kotlin
curl -v -X POST http://10.10.20.54:43545/media/stop \
```

---

## 🥚 Testing

You can use test media links like:
- [SoundHelix Audio](https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3)
- [Big Buck Bunny Video](https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4)

---

## ⚠️ Permissions

Make sure you include the following in your app's `AndroidManifest.xml`:

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## 📄 License

MIT License

---

## 👨‍💼 Author

**Aleksandar Lazić**
GitHub: [streamsoft-inc](https://github.com/streamsoft-inc)

---

