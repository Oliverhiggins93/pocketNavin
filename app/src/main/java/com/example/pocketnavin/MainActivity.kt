package com.example.pocketnavin

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketnavin.ui.theme.PocketNavinTheme
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketNavinTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Soundboard : Screen("soundboard", "Soundboard", { Icon(Icons.Default.Home, contentDescription = null) })
    object Gallery : Screen("gallery", "Gallery", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val items = listOf(Screen.Soundboard, Screen.Gallery)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Soundboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Soundboard.route) { SoundboardScreen() }
            composable(Screen.Gallery.route) { GalleryScreen() }
        }
    }
}

data class SoundItem(val name: String, val fileName: String)

@Composable
fun SoundboardScreen() {
    val context = LocalContext.current
    var sounds by remember { mutableStateOf(emptyList<SoundItem>()) }

    LaunchedEffect(Unit) {
        val soundFiles = context.assets.list("sounds")?.filter { it.endsWith(".aac") } ?: emptyList()
        // Sort files to ensure they appear in order (e.g., WA0002, WA0003...)
        sounds = soundFiles.sorted().mapIndexed { index, fileName ->
            // Use "Navin" + the sequence number for cleaner display
            val name = "Navin ${index + 1}"
            SoundItem(name, fileName)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Title and Profile Image Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pocket Navin",
                style = MaterialTheme.typography.headlineLarge
            )
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                // To use your own image:
                // 1. Place it in res/drawable (e.g., navin_profile.jpg)
                // 2. Replace R.drawable.ic_launcher_foreground with R.drawable.navin_profile
                Image(
                    painter = painterResource(id = R.drawable.navin_profile),
                    contentDescription = "Navin",
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (sounds.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No .aac sounds found in assets/sounds")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Changed to 2 for larger buttons
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sounds) { sound ->
                    SoundButton(sound)
                }
            }
        }
    }
}

@Composable
fun SoundButton(sound: SoundItem) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            playSound(context, sound.fileName)
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f), // Square buttons
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "▶", style = MaterialTheme.typography.displaySmall)
            }
        }
        Text(
            text = sound.name,
            style = MaterialTheme.typography.titleMedium, // Slightly larger text
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1
        )
    }
}

fun playSound(context: Context, fileName: String) {
    try {
        val afd = context.assets.openFd("sounds/$fileName")
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        mediaPlayer.prepare()
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    var images by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        images = context.assets.list("images")?.filter { 
            it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(".jpeg")
        } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Navin's best bits",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (images.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No images found in assets/images")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(images) { imageName ->
                    GalleryImage(imageName)
                }
            }
        }
    }
}

@Composable
fun GalleryImage(imageName: String) {
    val context = LocalContext.current
    val bitmap = remember(imageName) {
        try {
            val inputStream: InputStream = context.assets.open("images/$imageName")
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = imageName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
        )
    } ?: Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text("Error loading $imageName", style = MaterialTheme.typography.bodySmall)
    }
}
