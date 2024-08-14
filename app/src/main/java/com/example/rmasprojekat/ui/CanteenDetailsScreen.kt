package com.example.rmasprojekat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rmasprojekat.R
import com.example.rmasprojekat.model.Canteen
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import com.example.rmasprojekat.model.Status
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun CanteenDetailsScreen(canteen: Canteen, onDismiss: () -> Unit, canteenViewModel: CanteenViewModel, authViewModel: AuthViewModel) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(canteen.name) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        // Custom indicator
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                ) {
                    listOf("Info", "Galerija", "Recenzije").forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }

                HorizontalPager(
                    count = 4,
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> CanteenInfoSection(canteen)
                        1 -> CanteenGallerySection(canteen)
                        2 -> CanteenReviewsSection(canteen, authViewModel)
                    }
                }
            }
        }
    )
}

@Composable
fun CanteenInfoSection(canteen: Canteen) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Informacije o menzi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(text = "Adresa: ${canteen.address}")
        Text(text = "Radno vreme: ${canteen.radnoVreme}")
        Text(text = "Kapacitet: ${canteen.kapacitet} mesta")
        Text(text = "Telefon: ${canteen.telefon}")

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CanteenGallerySection(canteen: Canteen) {
    val pagerState = rememberPagerState()
    val images = listOf(canteen.imageUrl)

    Column {
        HorizontalPager(
            count = images.size,
            state = pagerState,
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
        ) { page ->
            Image(
                painter = painterResource(id = R.drawable.canteen1), //ovde url
                contentDescription = "Slika menze ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        )
    }
}

@Composable
fun CanteenReviewsSection(canteen: Canteen, authViewModel: AuthViewModel) {
    LazyColumn {
        item {
            Text(
                text = "Recenzije",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(canteen.reviews) { review ->
            ReviewItem(review = review, authViewModel = authViewModel)
        }
    }
}



