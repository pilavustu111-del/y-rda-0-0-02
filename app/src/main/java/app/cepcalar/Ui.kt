@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.cepcalar

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

val BG = Color(0xFF0B0907)
val CARD = Color(0xFF16110B)
val CHIP = Color(0xFF211A12)
val GOLD = Color(0xFFE3A857)
val MUTED = Color(0xFFB8AB9A)
val INK = Color(0xFF1A1208)
val EDGE = Color(0x55E3A857)

fun tintFor(s: String, sat: Float = 0.35f, v: Float = 0.30f): Color =
    Color.hsv((abs(s.hashCode()) % 360).toFloat(), sat, v)

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

object ThumbCache {
    val cache = LruCache<String, ImageBitmap>(80)
}

@Composable
fun Art(song: Song?, modifier: Modifier = Modifier, corner: Dp = 6.dp, letter: Boolean = true) {
    val ctx = LocalContext.current
    val title = song?.title ?: ""
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(tintFor(title, 0.38f, 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        if (letter) {
            Text(
                (title.trim().firstOrNull()?.uppercase() ?: "♪"),
                color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 22.sp
            )
        }
        if (song != null) {
            if (song.artUrl != null) {
                AsyncImage(
                    model = song.artUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else if (!song.remote) {
                val bmp by produceState<ImageBitmap?>(null, song.mediaId) {
                    // Şarkı değişince eski kapak kalmasın: önce önbelleğe (yoksa boşa) sıfırla, sonra yükle.
                    val cached = ThumbCache.cache.get(song.mediaId)
                    value = cached
                    if (cached == null) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                ctx.contentResolver.loadThumbnail(song.uri, Size(320, 320), null).asImageBitmap()
                            }.getOrNull()
                        }?.also { ThumbCache.cache.put(song.mediaId, it) }
                    }
                }
                bmp?.let {
                    Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun CepApp(vm: MusicVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var full by rememberSaveable { mutableStateOf(false) }
    val perms = remember {
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.scan()
    }
    LaunchedEffect(Unit) { if (!vm.checkPermission()) launcher.launch(perms) }
    LaunchedEffect(Unit) {
        while (true) {
            vm.tick()
            delay(400)
        }
    }
    LaunchedEffect(vm.openFull) {
        if (vm.openFull) {
            full = true
            vm.openFull = false
        }
    }
    BackHandler(enabled = full) { full = false }

    MaterialTheme(colorScheme = darkColorScheme(primary = GOLD, background = BG, surface = CARD)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xFF1C140B), 0.4f to BG, 1f to BG))) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    when (tab) {
                        0 -> Home(vm, onGo = { tab = it }) { launcher.launch(perms) }
                        1 -> SearchTab(vm)
                        2 -> AssistantTab(vm)
                        3 -> LibraryTab(vm)
                        else -> LikedTab(vm)
                    }
                }
                if (vm.current != null) MiniPlayer(vm) { full = true }
                NavBar(tab) { tab = it }
            }
            AnimatedVisibility(
                visible = full,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) { FullPlayer(vm) { full = false } }
        }
    }
}

// ---------- Ana sayfa ----------

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) GOLD else CHIP)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, color = if (selected) INK else Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun SectionTitle(t: String, onAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(22.dp).background(GOLD))
        Text(
            t, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        )
        if (onAll != null) {
            Text("Tümünü Gör →", color = GOLD, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onAll))
        }
    }
}

@Composable
fun Hint(t: String, modifier: Modifier = Modifier) {
    Text(t, color = MUTED, fontSize = 14.sp, modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
fun HomeHeader(vm: MusicVM) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Yır") }
                    withStyle(SpanStyle(color = GOLD)) { append("daş") }
                },
                fontSize = 34.sp, fontWeight = FontWeight.Bold
            )
            Text("Müzik, sana özel.", color = MUTED, fontSize = 15.sp)
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).border(1.dp, EDGE, CircleShape).background(CARD)
                .clickable { vm.shuffleAll(vm.songs) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Shuffle, "Karışık çal", tint = GOLD) }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).height(54.dp)
            .clip(RoundedCornerShape(27.dp)).border(1.dp, EDGE, RoundedCornerShape(27.dp)).background(CARD)
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = MUTED)
        Text("Şarkı, sanatçı veya klip ara…", color = MUTED, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun HeroCard(cover: Song?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(210.dp).clip(shape)
            .border(1.dp, EDGE, shape).background(Color(0xFF1A130B))
    ) {
        if (cover != null) {
            Art(cover, Modifier.align(Alignment.CenterEnd).fillMaxHeight().aspectRatio(1f), 0.dp, letter = false)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color(0xFF1A130B), 0.55f to Color(0xE61A130B), 1f to Color(0x331A130B))
            )
        )
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = GOLD, modifier = Modifier.size(18.dp))
                Text("Yapay Zekâ Önerisi", color = GOLD, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                "Ruh Haline Göre\nŞarkılar", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Nasıl hissettiğini söyle, listene bakıp sana uygun parçaları çalayım.",
                color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp).widthIn(max = 230.dp)
            )
            Row(
                Modifier.padding(top = 14.dp).clip(RoundedCornerShape(50)).border(1.dp, GOLD, RoundedCornerShape(50))
                    .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = GOLD, modifier = Modifier.size(18.dp))
                Text("Asistana Sor", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
fun PlaylistCard(title: String, sub: String, cover: Song?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.width(150.dp).height(190.dp).clip(shape).border(1.dp, EDGE, shape).clickable(onClick = onClick)
    ) {
        Art(cover, Modifier.fillMaxSize(), 0.dp, letter = false)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0907)), startY = 160f)
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("• $sub", color = MUTED, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun AssistantBanner(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp).clip(shape).border(1.dp, EDGE, shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2B1E10), Color(0xFF14100B))))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = GOLD, modifier = Modifier.size(20.dp))
                Text(
                    "Yırdaş Müzik Asistanı", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                "Ruh haline ve zevklerine göre sana özel çalma listeleri hazırlar.",
                color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            "Keşfet →", color = GOLD, fontSize = 14.sp,
            modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(50))
                .border(1.dp, GOLD, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun Home(vm: MusicVM, onGo: (Int) -> Unit, onAsk: () -> Unit) {
    val list = when (vm.filter) {
        1 -> vm.songs.filter { !it.isVideo }
        2 -> vm.songs.filter { it.isVideo }
        else -> vm.songs
    }
    val ranked = remember(vm.songs, vm.stats) {
        vm.songs.filter { vm.count(it) > 0 }.sortedByDescending { vm.count(it) }
    }
    val queue = remember(list, vm.stats, vm.recoTracks) { vm.mixed(list.sortedByDescending { vm.count(it) }, 4) }
    val shown = queue.take(14)
    val videos = remember(vm.songs) { vm.songs.filter { it.isVideo } }
    val mixList = remember(vm.songs) { vm.songs.shuffled().take(40) }
    val hero = ranked.firstOrNull() ?: vm.songs.firstOrNull()

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { HomeHeader(vm) }
        item { SearchPill { onGo(1) } }
        item { HeroCard(hero) { onGo(2) } }
        if (!vm.hasPermission) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Telefondaki müzikleri bulabilmem için izin ver.", color = Color.White, fontSize = 16.sp)
                    Button(
                        onClick = onAsk,
                        colors = ButtonDefaults.buttonColors(containerColor = GOLD, contentColor = INK)
                    ) { Text("İzin ver") }
                    Text(
                        "İzin penceresi çıkmazsa Ayarlar > Uygulamalar > Yırdaş > İzinler bölümünden Müzik ve ses ile Fotoğraf ve video iznini aç.",
                        color = MUTED, fontSize = 13.sp
                    )
                }
            }
        } else if (vm.songs.isEmpty()) {
            item { Hint("Bu telefonda henüz müzik bulunamadı. Dosyalar eklenince liste kendiliğinden güncellenir.") }
        }

        if (vm.songs.isNotEmpty()) {
            item { SectionTitle("Senin İçin Hazırlandı") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val src = ranked.ifEmpty { vm.songs }.take(30)
                        PlaylistCard("Çok Dinlediklerin", "${src.size} parça", src.firstOrNull()) {
                            vm.play(vm.mixed(src, 4), 0)
                        }
                    }
                    item {
                        PlaylistCard("Karışık Çal", "${mixList.size} parça", mixList.firstOrNull()) {
                            vm.play(vm.mixed(mixList, 4), 0)
                        }
                    }
                    if (videos.isNotEmpty()) {
                        item {
                            PlaylistCard("Klipler", "${videos.size} klip", videos.firstOrNull()) { vm.play(videos, 0) }
                        }
                    }
                    items(vm.similar.take(6)) { a ->
                        StationCard(a) { vm.playStation(a) }
                    }
                }
            }
        }

        item { AssistantBanner { onGo(2) } }

        if (ranked.isNotEmpty()) {
            item { SectionTitle("En Çok Dinlediklerin") { vm.libMode = 1; onGo(3) } }
            itemsIndexed(ranked.take(5)) { i, s ->
                SongRow(s, vm, rank = i + 1, sub = "${vm.count(s)} kez · " + s.artist.ifEmpty { "Sanatçı bilinmiyor" }) {
                    vm.play(ranked, i)
                }
            }
        }

        item { SectionTitle("Parçaların") { vm.libMode = 0; onGo(3) } }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Tümü", vm.filter == 0) { vm.filter = 0 }
                Chip("Müzik", vm.filter == 1) { vm.filter = 1 }
                Chip("Klipler", vm.filter == 2) { vm.filter = 2 }
            }
        }
        items(shown) { s -> SongRow(s, vm) { vm.play(queue, queue.indexOf(s)) } }

        if (vm.mine.isNotEmpty()) {
            item { SectionTitle("Senin Radyoların") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) { items(vm.mine) { a -> StationCard(a) { vm.playStation(a) } } }
            }
        } else if (!vm.online && vm.songs.isNotEmpty()) {
            item { Hint("İnternete bağlanınca en çok dinlediğin sanatçılara göre istasyonlar burada çıkar.", Modifier.padding(top = 16.dp)) }
        }
        if (videos.isNotEmpty() && vm.filter != 1) {
            item { SectionTitle("Klipler") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(videos) { v ->
                        Column(Modifier.width(220.dp).clickable { vm.play(videos, videos.indexOf(v)) }) {
                            Art(v, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 12.dp)
                            Text(
                                v.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(v.artist.ifEmpty { "Klip" }, color = MUTED, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationCard(a: Artist, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.width(150.dp).height(190.dp).clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onClick)) {
        AsyncImage(
            model = a.pic, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0907)), startY = 160f)
            )
        )
        Text(
            "RADYO", color = GOLD, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(a.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("• ve benzerleri", color = MUTED, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun SongRow(s: Song, vm: MusicVM, sub: String? = null, rank: Int? = null, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val now = vm.current?.mediaId == s.mediaId
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                "$rank", color = if (rank == 1) GOLD else MUTED, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp), fontSize = 16.sp
            )
        }
        Art(s, Modifier.size(56.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                s.title, color = if (now) GOLD else Color.White, fontSize = 17.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.isVideo) {
                    Text(
                        "KLİP", color = MUTED, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp).border(1.dp, MUTED, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp)
                    )
                }
                Text(
                    sub ?: s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = MUTED, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (s.remote) {
            val liked = vm.isLiked(s)
            IconButton(onClick = { vm.toggleLike(s) }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (liked) "Beğenmekten vazgeç" else "Beğen",
                    tint = if (liked) GOLD else Color.White
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Daha fazla", tint = Color.White)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Sıradakine ekle") }, onClick = {
                    vm.addNext(s)
                    menu = false
                })
                if (!s.link.isNullOrBlank()) {
                    DropdownMenuItem(text = { Text("Deezer'da aç") }, onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(s.link))) }
                        menu = false
                    })
                }
            }
        }
    }
}

// ---------- Ara / Kitaplık / En çok ----------

@Composable
fun Header(t: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(t, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SearchTab(vm: MusicVM) {
    var q by rememberSaveable { mutableStateOf("") }
    val res = remember(q, vm.songs) {
        if (q.isBlank()) emptyList()
        else vm.songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Ara")
        TextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Ne dinlemek istiyorsun?") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MUTED) },
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (q.isNotBlank() && res.isEmpty()) Hint("Sonuç yok.")
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(res) { s -> SongRow(s, vm) { vm.play(res, res.indexOf(s)) } }
        }
    }
}

@Composable
fun LibraryTab(vm: MusicVM) {
    val shown = remember(vm.songs, vm.recoTracks) { vm.mixed(vm.songs, 5) }
    Column(Modifier.fillMaxSize()) {
        Header("Kitaplığım") {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(GOLD).clickable { vm.shuffleAll(vm.songs) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Shuffle, "Karışık çal", tint = INK) }
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Tüm parçalar", vm.libMode == 0) { vm.libMode = 0 }
            Chip("En çok dinlenen", vm.libMode == 1) { vm.libMode = 1 }
        }
        if (vm.libMode == 1) {
            TopContent(vm)
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${vm.songs.size} parça · Tüm videoları göster", color = MUTED, fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = vm.allVideos, onCheckedChange = { vm.setShowAllVideos(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = GOLD, checkedThumbColor = INK)
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown) { s ->
                    val c = vm.count(s)
                    SongRow(
                        s, vm,
                        sub = (if (c > 0) "$c kez dinlendi · " else "") + s.artist.ifEmpty { "Sanatçı bilinmiyor" }
                    ) { vm.play(shown, shown.indexOf(s)) }
                }
            }
        }
    }
}

@Composable
fun TopContent(vm: MusicVM) {
    val ranked = remember(vm.songs, vm.stats) {
        vm.songs.filter { vm.count(it) > 0 }.sortedByDescending { vm.count(it) }.take(50)
    }
    Column(Modifier.fillMaxSize()) {
        if (ranked.isEmpty()) Hint("Henüz dinleme kaydı yok. Bir parçayı yaklaşık 30 saniye dinleyince sayılır.")
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(ranked) { i, s ->
                SongRow(
                    s, vm, rank = i + 1,
                    sub = "${vm.count(s)} kez · " + s.artist.ifEmpty { "Sanatçı bilinmiyor" }
                ) { vm.play(ranked, i) }
            }
        }
    }
}

// ---------- Alt çubuklar ----------

@Composable
fun MiniPlayer(vm: MusicVM, onOpen: () -> Unit) {
    val s = vm.current ?: return
    val frac = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onOpen)
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Art(s, Modifier.size(50.dp), 10.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(s.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = MUTED, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, "Önceki", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).border(2.dp, GOLD, CircleShape).clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (vm.isPlaying) "Duraklat" else "Oynat", tint = Color.White, modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, "Sonraki", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))
        ) { Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(GOLD)) }
    }
}

@Composable
fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "Ana Sayfa" to Icons.Filled.Home,
        "Ara" to Icons.Filled.Search,
        "Asistan" to Icons.Filled.AutoAwesome,
        "Kitaplığım" to Icons.Filled.LibraryMusic,
        "Beğenilen" to Icons.Filled.Favorite,
    )
    Row(
        Modifier.fillMaxWidth().background(BG).navigationBarsPadding().padding(top = 8.dp, bottom = 6.dp)
    ) {
        items.forEachIndexed { i, (label, icon) ->
            Column(
                Modifier.weight(1f).clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, label, tint = if (i == selected) GOLD else MUTED, modifier = Modifier.size(26.dp))
                Text(label, color = if (i == selected) GOLD else MUTED, fontSize = 11.sp, maxLines = 1)
                Box(
                    Modifier.padding(top = 4.dp).width(32.dp).height(2.dp)
                        .background(if (i == selected) GOLD else Color.Transparent)
                )
            }
        }
    }
}

// ---------- Şimdi çalıyor ----------

@Composable
fun FullPlayer(vm: MusicVM, onClose: () -> Unit) {
    val s = vm.current
    val tint = tintFor(s?.title ?: "", 0.45f, 0.38f)
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint, BG)))
            .pointerInput(Unit) {}.statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, "Kapat", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Text(
                "ŞİMDİ ÇALIYOR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }
        if (s != null) {
            if (s.isVideo) {
                AndroidView(
                    factory = { c ->
                        PlayerView(c).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            player = vm.controller
                        }
                    },
                    update = { it.player = vm.controller },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                )
            } else {
                Art(s, Modifier.align(Alignment.CenterHorizontally).size(220.dp), 8.dp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
                if (s.remote) {
                    val liked = vm.isLiked(s)
                    IconButton(onClick = { vm.toggleLike(s) }) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (liked) "Beğenmekten vazgeç" else "Beğen",
                            tint = if (liked) GOLD else Color.White, modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            var drag by remember { mutableStateOf<Float?>(null) }
            val dur = vm.duration.coerceAtLeast(1L)
            val frac = drag ?: (vm.position.toFloat() / dur).coerceIn(0f, 1f)
            Slider(
                value = frac,
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { vm.seekTo((it * dur).toLong()) }
                    drag = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x55FFFFFF)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt((frac * dur).toLong()), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(fmt(vm.duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, "Karıştır", tint = if (vm.shuffle) GOLD else Color.White)
                }
                IconButton(onClick = { vm.prev() }) {
                    Icon(Icons.Filled.SkipPrevious, "Önceki", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White).clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (vm.isPlaying) "Duraklat" else "Oynat", tint = INK, modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Filled.SkipNext, "Sonraki", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Tekrarla",
                        tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White else GOLD
                    )
                }
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0x33000000))
            ) {
                val ly = vm.lyrics
                if (ly == null) {
                    Text(
                        when {
                            vm.lyricsLoading -> "Sözler aranıyor…"
                            s.remote -> "Bu 30 saniyelik önizlemede söz yok."
                            !vm.online -> "Sözler için internete bağlan. Bulunan sözler telefona kaydedilir."
                            else -> "Bu parça için söz bulunamadı."
                        },
                        color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LyricsList(ly, vm.position) { vm.seekTo(it) }
                }
            }
        }
    }
}

@Composable
fun LyricsList(ly: Lyrics, pos: Long, onSeek: (Long) -> Unit) {
    val state = remember(ly) { LazyListState() }
    val active = if (ly.synced) ly.lines.indexOfLast { it.timeMs <= pos + 200 } else -1
    LaunchedEffect(active, ly) {
        if (active >= 0) state.animateScrollToItem(active, -140)
    }
    LazyColumn(state = state, contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(ly.lines) { i, l ->
            Text(
                l.text.ifBlank { "♪" },
                fontSize = if (ly.synced) 22.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (!ly.synced || i == active) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 6.dp).clickable(enabled = ly.synced) { onSeek(l.timeMs) }
            )
        }
    }
}

// ---------- Asistan ----------

@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CHIP, unfocusedContainerColor = CHIP,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = GOLD,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = MUTED, unfocusedPlaceholderColor = MUTED,
    focusedLabelColor = MUTED, unfocusedLabelColor = MUTED,
)

@Composable
fun Bubble(m: ChatMsg) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            m.text, color = if (m.fromUser) INK else Color.White, fontSize = 16.sp,
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                .background(if (m.fromUser) GOLD else CHIP).padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun AssistantTab(vm: MusicVM) {
    var text by rememberSaveable { mutableStateOf("") }
    var editKey by rememberSaveable { mutableStateOf(false) }
    var keyDraft by rememberSaveable { mutableStateOf("") }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val t = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!t.isNullOrBlank()) vm.ask(t)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(vm.chat.size, vm.aiBusy) {
        val n = vm.chat.size + (if (vm.aiBusy) 1 else 0)
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Header("Asistan") {
            IconButton(onClick = { editKey = !editKey; keyDraft = "" }) {
                Icon(Icons.Filled.Settings, "Ayarlar", tint = Color.White)
            }
        }
        if (vm.apiKey.isBlank() || editKey) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Asistan Google Gemini ile çalışır ve ücretsizdir, kart gerekmez. aistudio.google.com/apikey adresine " +
                        "Google hesabınla girip \"Create API key\" de, çıkan anahtarı buraya yapıştır. " +
                        "Anahtar yalnızca bu telefonda saklanır.",
                    color = Color.White, fontSize = 15.sp
                )
                TextField(
                    value = keyDraft, onValueChange = { keyDraft = it }, singleLine = true,
                    placeholder = { Text(if (vm.apiKey.isBlank()) "AIza..." else "Yeni anahtar") },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(8.dp), colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                var modelDraft by rememberSaveable { mutableStateOf(vm.aiModel) }
                TextField(
                    value = modelDraft, onValueChange = { modelDraft = it }, singleLine = true,
                    label = { Text("Model (varsayılanı bırakabilirsin)") },
                    shape = RoundedCornerShape(8.dp), colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            vm.saveModel(modelDraft)
                            if (keyDraft.isNotBlank()) vm.saveKey(keyDraft)
                            if (keyDraft.isNotBlank() || vm.apiKey.isNotBlank()) { keyDraft = ""; editKey = false }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GOLD, contentColor = INK)
                    ) { Text("Kaydet") }
                    if (vm.apiKey.isNotBlank()) {
                        Button(
                            onClick = { vm.saveKey(""); keyDraft = ""; editKey = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CHIP, contentColor = Color.White)
                        ) { Text("Anahtarı sil") }
                    }
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (vm.chat.isEmpty()) {
                    item {
                        Hint(
                            "Ruh halini ya da ne dinlemek istediğini söyle. Telefonundaki listene bakıp sana göre çalarım. " +
                                "Mesela: \"Ruh halim kötü, iyileştirecek oyun havaları aç\" ya da \"Hüzünlü şarkılar aç\"."
                        )
                    }
                }
                items(vm.chat) { m -> Bubble(m) }
                if (vm.aiBusy) item { Bubble(ChatMsg(false, "Listene bakıyorum…")) }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Ruh halim kötü, iyileştir", "Hüzünlü şarkılar aç", "Oyun havası, heyecanlı",
                    "Sakin bir şeyler", "Bana enerji ver"
                ).forEach { q -> Chip(q, false) { vm.ask(q) } }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text, onValueChange = { text = it }, maxLines = 3,
                    placeholder = { Text("Ruh halin ne? Ne çalayım?") },
                    shape = RoundedCornerShape(24.dp), colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ruh halini söyle")
                    }
                    runCatching { speech.launch(i) }
                }) { Icon(Icons.Filled.Mic, "Sesle söyle", tint = GOLD) }
                IconButton(onClick = { vm.ask(text); text = "" }, enabled = !vm.aiBusy) {
                    Icon(Icons.Filled.Send, "Gönder", tint = if (vm.aiBusy) MUTED else GOLD)
                }
            }
        }
    }
}

// ---------- Beğenilenler ----------

@Composable
fun LikedTab(vm: MusicVM) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Header("Beğendiklerin") {
            if (vm.likes.isNotEmpty()) {
                IconButton(onClick = {
                    val body = buildString {
                        append("Yırdaş - beğendiğim öneriler\n\n")
                        vm.likes.forEachIndexed { i, s ->
                            append("${i + 1}. ${s.artist} - ${s.title}\n")
                            if (!s.link.isNullOrBlank()) append("   ${s.link}\n")
                        }
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    runCatching { ctx.startActivity(Intent.createChooser(send, "Listeyi paylaş")) }
                }) { Icon(Icons.Filled.Share, "Listeyi paylaş", tint = Color.White) }
            }
        }
        if (vm.likes.isEmpty()) {
            Hint("Listelerin arasına karışan yeni parçalarda kalbe dokun, burada birikir. Sonra paylaş simgesiyle listeyi alıp indirebilirsin.")
        } else {
            Hint("Önizleme 30 saniyedir. Tam hâlini indirmek için listeyi paylaşabilir ya da parçanın menüsünden Deezer'da açabilirsin.")
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(vm.likes) { i, s -> SongRow(s, vm) { vm.play(vm.likes, i) } }
        }
    }
}
