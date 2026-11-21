package app.cepcalar

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

data class ChatMsg(val fromUser: Boolean, val text: String)

class MusicVM(app: Application) : AndroidViewModel(app) {
    private val ctx = app

    var songs by mutableStateOf(emptyList<Song>())
    var stats by mutableStateOf(Store.loadStats(app))
    var hasPermission by mutableStateOf(false)
    var allVideos by mutableStateOf(Store.getAllVideos(app))
    var filter by mutableStateOf(0) // 0 Tümü, 1 Müzik, 2 Klipler
    var libMode by mutableStateOf(0) // Kitaplık: 0 tüm parçalar, 1 en çok dinlenen

    var controller by mutableStateOf<MediaController?>(null)
    var current by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
    var position by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var openFull by mutableStateOf(false)

    var lyrics by mutableStateOf<Lyrics?>(null)
    var lyricsLoading by mutableStateOf(false)

    var online by mutableStateOf(false)
    var similar by mutableStateOf(emptyList<Artist>())
    var mine by mutableStateOf(emptyList<Artist>())

    // Yapay zekâ asistanı
    var apiKey by mutableStateOf(Store.getKey(app))
    var aiModel by mutableStateOf(Store.getModel(app))
    var chat by mutableStateOf(emptyList<ChatMsg>())
    var aiBusy by mutableStateOf(false)
    private val history = ArrayList<Pair<String, String>>()

    // Öneri parçalar (30 sn önizleme) ve beğenilenler
    var recoTracks by mutableStateOf(emptyList<Song>())
    var likes by mutableStateOf(Store.loadLikes(app))
    private var lastReco = 0L

    private var counted = false
    private var lyricsJob: Job? = null
    private var recoJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            current = mediaItem?.let { songFromItem(it) }
            counted = false
            position = 0
            loadLyrics()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@MusicVM.isPlaying = isPlaying
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            this@MusicVM.repeatMode = repeatMode
        }
    }

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scan()
        }
    }

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            c.addListener(listener)
            controller = c
            c.currentMediaItem?.let { current = songFromItem(it) }
            isPlaying = c.isPlaying
            shuffle = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            loadLyrics()
        }, ContextCompat.getMainExecutor(ctx))

        ctx.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        ctx.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)

        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        online = isOnline()
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = true
                    refreshReco()
                    if (lyrics == null) loadLyrics()
                }

                override fun onLost(network: Network) {
                    online = isOnline()
                }
            })
        }
        checkPermission()
    }

    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun checkPermission(): Boolean {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        hasPermission = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        return hasPermission
    }

    fun scan() {
        if (!checkPermission()) return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { Library.scan(ctx, allVideos) }
            songs = list
            refreshReco()
        }
    }

    fun setShowAllVideos(v: Boolean) {
        allVideos = v
        Store.setAllVideos(ctx, v)
        scan()
    }

    // ---- Çalma ----
    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.setMediaItems(list.map { it.toItem() }, index.coerceIn(0, list.size - 1), 0L)
        c.prepare()
        c.play()
        if (list[index.coerceIn(0, list.size - 1)].isVideo) openFull = true
    }

    fun shuffleAll(list: List<Song>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        val q = mixed(list, 5)
        c.shuffleModeEnabled = true
        play(q, Random.nextInt(q.size))
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }
    fun prev() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPrevious()
    }

    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun addNext(s: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(s), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, s.toItem())
    }

    /** UI her ~400 ms çağırır: konumu günceller ve 30 sn dinlenince sayacı artırır. */
    fun tick() {
        val c = controller ?: return
        position = c.currentPosition
        duration = c.duration.let { if (it < 0) 0 else it }
        val cur = current ?: return
        if (!counted && c.isPlaying && !cur.remote) {
            val need = if (duration > 0) min(30000L, duration / 2) else 30000L
            if (position >= need) {
                counted = true
                stats = Store.inc(ctx, cur.statKey)
            }
        }
    }

    // ---- Sözler ----
    private fun loadLyrics() {
        lyricsJob?.cancel()
        lyrics = null
        val s = current ?: return
        if (s.remote) return
        lyricsJob = viewModelScope.launch {
            lyricsLoading = true
            lyrics = withContext(Dispatchers.IO) { Net.lyrics(ctx, s) }
            lyricsLoading = false
        }
    }

    // ---- Öneriler ----
    fun count(s: Song) = stats[s.statKey] ?: 0

    fun topArtistNames(n: Int): List<String> {
        val score = HashMap<String, Int>()
        for (s in songs) {
            if (s.artist.isEmpty()) continue
            score[s.artist] = (score[s.artist] ?: 0) + count(s) * 100 + 1
        }
        return score.entries.sortedByDescending { it.value }.map { it.key }.take(n)
    }

    fun refreshReco(force: Boolean = false) {
        if (!isOnline()) { online = false; return }
        online = true
        if (!force && recoTracks.isNotEmpty() && System.currentTimeMillis() - lastReco < 30 * 60_000L) return
        val names = topArtistNames(4)
        if (names.isEmpty()) return
        recoJob?.cancel()
        recoJob = viewModelScope.launch {
            val own = ArrayList<Artist>()
            val rel = ArrayList<Artist>()
            withContext(Dispatchers.IO) {
                for (n in names) {
                    val a = Net.searchArtist(n) ?: continue
                    own.add(a)
                    rel.addAll(Net.related(a.id, 6))
                }
            }
            val ownIds = own.map { it.id }.toSet()
            val sim = rel.distinctBy { it.id }.filter { it.id !in ownIds }.take(14)
            mine = own
            similar = sim
            val tracks = withContext(Dispatchers.IO) { sim.take(8).flatMap { Net.topTracks(it.id).take(3) } }
            recoTracks = tracks.filter { !isLiked(it) }.distinctBy { it.mediaId }.shuffled()
            lastReco = System.currentTimeMillis()
        }
    }

    /** Listenin arasına her [every] parçada bir öneri parça serpiştirir. */
    fun mixed(list: List<Song>, every: Int = 4, extra: List<Song> = recoTracks): List<Song> {
        if (extra.isEmpty() || list.isEmpty()) return list
        val have = list.map { it.mediaId }.toSet()
        val pool = extra.filter { it.mediaId !in have }.toMutableList()
        val out = ArrayList<Song>()
        list.forEachIndexed { i, s ->
            out.add(s)
            if ((i + 1) % every == 0 && pool.isNotEmpty()) out.add(pool.removeAt(0))
        }
        return out
    }

    // ---- Beğenilenler ----
    fun isLiked(s: Song) = likes.any { it.mediaId == s.mediaId }

    fun toggleLike(s: Song) {
        likes = if (isLiked(s)) likes.filter { it.mediaId != s.mediaId } else likes + s
        Store.saveLikes(ctx, likes)
    }

    // ---- Yapay zekâ asistanı ----
    fun saveModel(m: String) {
        Store.setModel(ctx, m)
        aiModel = Store.getModel(ctx)
    }

    fun saveKey(k: String) {
        Store.setKey(ctx, k)
        apiKey = k.trim()
    }

    fun ask(text: String) {
        val t = text.trim()
        if (t.isEmpty() || aiBusy) return
        chat = chat + ChatMsg(true, t)
        if (apiKey.isBlank()) {
            chat = chat + ChatMsg(false, "Önce sağ üstteki ayar simgesinden API anahtarını ekle.")
            return
        }
        if (!isOnline()) {
            chat = chat + ChatMsg(false, "Yapay zekâ için internet lazım. Bağlanınca tekrar dene.")
            return
        }
        aiBusy = true
        val snap = songs.sortedWith(compareByDescending<Song> { count(it) }.thenBy { it.title.lowercase() }).take(500)
        val lib = snap.mapIndexed { i, s ->
            "$i|${s.title.replace('|', ' ')}|${s.artist.replace('|', ' ')}|${count(s)}"
        }.joinToString("\n")
        val msgs = ArrayList<Pair<String, String>>(history.takeLast(6))
        while (msgs.isNotEmpty() && msgs[0].first != "user") msgs.removeAt(0)
        msgs.add("user" to t)
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Net.ask(apiKey, aiModel, Ai.system(lib), msgs) }
            val raw = r.text.orEmpty()
            val plan = Ai.parse(raw)
            if (plan == null) {
                chat = chat + ChatMsg(false, r.error ?: "Cevabı anlayamadım, bir daha dener misin?")
                aiBusy = false
                return@launch
            }
            history.add("user" to t)
            history.add("assistant" to raw)
            val local = plan.picks.distinct().mapNotNull { snap.getOrNull(it) }.take(25)
            val found = withContext(Dispatchers.IO) {
                plan.discover.take(4).flatMap { n ->
                    val a = Net.searchArtist(n)
                    if (a == null) emptyList() else Net.topTracks(a.id).take(3)
                }
            }.distinctBy { it.mediaId }
            val queue = if (local.isNotEmpty()) mixed(local, 3, found) else found
            var line = plan.reply
            if (queue.isNotEmpty()) line += "\n\n▶ ${queue.size} parça çalıyor."
            chat = chat + ChatMsg(false, line)
            aiBusy = false
            if (queue.isNotEmpty()) play(queue, 0)
        }
    }

    fun playStation(a: Artist) {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { Net.topTracks(a.id) }
            if (tracks.isNotEmpty()) play(tracks, 0)
        }
    }

    override fun onCleared() {
        ctx.contentResolver.unregisterContentObserver(mediaObserver)
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
