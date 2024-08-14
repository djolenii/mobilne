package com.example.rmasprojekat.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.rmasprojekat.model.Canteen
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rmasprojekat.viewmodel.LocationViewModel

@Composable
fun WelcomeScreen(
    canteenViewModel: CanteenViewModel = viewModel(),
    onShowMapClicked: () -> Unit,
    onCanteenClicked: (Canteen) -> Unit,
    isLocationEnabled: Boolean
) {
    val context = LocalContext.current
    val currentCity by canteenViewModel.currentCity.observeAsState("Loading...")
    val canteens by canteenViewModel.canteens.observeAsState(emptyList())
    val currentIndex by canteenViewModel.currentCanteenIndex.observeAsState(0)
    val isLoading by canteenViewModel.isLoading.observeAsState(initial = true)


    LaunchedEffect(isLocationEnabled) {
        if (isLocationEnabled && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            canteenViewModel.checkAndUpdateLocation(context)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                .border(2.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = currentCity, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = onShowMapClicked) {
                    Icon(imageVector = Icons.Rounded.Map, contentDescription = "Map Icon", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading || canteens.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        else {
            CanteenSlideShow(
                canteens = canteens,
                onCanteenClicked = onCanteenClicked,
                currentIndex = currentIndex,
                onIndexChanged = { canteenViewModel.setCurrentCanteenIndex(it) }
            )
        }
    }
}

@Composable
fun CanteenSlideShow(
    canteens: List<Canteen>,
    onCanteenClicked: (Canteen) -> Unit,
    currentIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        if (canteens.isNotEmpty()) {
            val currentCanteen = canteens[currentIndex]

            // Canteen Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                    .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = MaterialTheme.colorScheme.secondary, spotColor = MaterialTheme.colorScheme.secondary )
                    .clickable { onCanteenClicked(currentCanteen) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        currentCanteen.imageUrl?.let { imageUrl ->
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .scale(coil.size.Scale.FILL)
                                        .build()
                                ),
                                contentDescription = currentCanteen.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop // Podesi način prikaza slike
                            )
                        }
                        // Navigation buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .align(Alignment.BottomCenter), // Positioning at the bottom
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                if (currentIndex > 0) onIndexChanged(currentIndex - 1)
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                                        .padding(4.dp)
                                )
                            }
                            IconButton(onClick = {
                                if (currentIndex < canteens.size - 1) onIndexChanged(currentIndex + 1)
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = currentCanteen.name,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentCanteen.nickname,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
