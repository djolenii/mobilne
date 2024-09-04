package com.example.rmasprojekat.ui

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rmasprojekat.model.Canteen
import com.example.rmasprojekat.model.MapReview
import com.example.rmasprojekat.model.User
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    canteenViewModel: CanteenViewModel,
    authViewModel: AuthViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val canteens by canteenViewModel.canteens.observeAsState(emptyList())
    var selectedCanteen by remember { mutableStateOf<Canteen?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var isNearCanteen by remember { mutableStateOf(false) }
    val currentUser by authViewModel.currentUser.collectAsState()

    var selectedMealType by remember { mutableStateOf<String?>(null) }
    var showMealReviews by remember { mutableStateOf(false) }

    var showRankingDialog by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.8125, 20.4612), 12f) // Default to Belgrade
    }

    LaunchedEffect(Unit) {
        canteenViewModel.checkAndUpdateLocation(context)
    }

    LaunchedEffect(canteenViewModel.currentCity.value) {
        canteenViewModel.currentCity.value?.let { canteenViewModel.fetchCanteensForCity(it) }
    }

    // Function to update user location and check proximity to canteens
    fun updateUserLocation(location: Location) {
        userLocation = LatLng(location.latitude, location.longitude)
        selectedCanteen?.let { canteen ->
            val canteenLocation = LatLng(
                canteen.coordinates?.latitude ?: 0.0,
                canteen.coordinates?.longitude ?: 0.0
            )
            isNearCanteen = isUserNearCanteen(userLocation!!, canteenLocation)
        }
    }

    // Set up location updates
    DisposableEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { updateUserLocation(it) }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            onMapLoaded = {
                // Get user's location when map is loaded
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        ) {
            canteens.forEach { canteen ->
                canteen.coordinates?.let { coords ->
                    val position = LatLng(coords.latitude, coords.longitude)
                    Marker(
                        state = MarkerState(position = position),
                        title = canteen.name,
                        snippet = canteen.address,
                        onClick = {
                            selectedCanteen = canteen
                            scope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 15f))
                            }
                            true
                        }
                    )
                }
            }
        }

        selectedCanteen?.let { canteen ->
            val isSubscribed by canteenViewModel.isSubscribedToCanteen(currentUser?.id ?: "", canteen.id ?: "").collectAsState(initial = false)
            CanteenInfoCard(
                canteen = canteen,
                isNearCanteen = isNearCanteen,
                onAddReview = { showReviewDialog = true },
                onDismiss = { selectedCanteen = null },
                onMealTypeSelected = { mealType ->
                    selectedMealType = mealType
                    showMealReviews = true
                },
                canAddReview = canAddReview(canteen, currentUser),
                isSubscribed = isSubscribed,
                onSubscriptionToggle = {
                    canteenViewModel.toggleCanteenSubscription(canteen.id ?: "", currentUser?.id ?: "")
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBackClicked,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Back to Welcome Screen")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { showRankingDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Show Rankings")
            }
        }
    }

    if (showReviewDialog) {
        ReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { type, food, service, crowd ->
                selectedCanteen?.let { canteen ->
                    val review = MapReview(
                        type = type,
                        foodRating = food,
                        serviceRating = service,
                        crowdRating = crowd,
                        timestamp = System.currentTimeMillis()
                    )
                    canteenViewModel.addMapReview(canteen.id ?: "", review, currentUser)
                    Toast.makeText(context, "Uspesno ste dodali ocenu za $type", Toast.LENGTH_SHORT).show()

                    // Focus the camera on the selected canteen's marker
                    canteen.coordinates?.let { coords ->
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(coords.latitude, coords.longitude), 15f))
                        }
                    }
                }
                showReviewDialog = false
                selectedCanteen = null
            },
            disabledMealTypes = getDisabledMealTypes(selectedCanteen, currentUser)
        )
    }

    if (showMealReviews && selectedMealType != null) {
        MealReviewsDialog(
            canteen = selectedCanteen,
            mealType = selectedMealType!!,
            onDismiss = { showMealReviews = false }
        )
    }

    if (showRankingDialog) {
        RankingDialog(
            canteenViewModel = canteenViewModel,
            onDismiss = { showRankingDialog = false }
        )
    }
}

@Composable
fun CanteenInfoCard(
    canteen: Canteen,
    isNearCanteen: Boolean,
    onAddReview: () -> Unit,
    onDismiss: () -> Unit,
    onMealTypeSelected: (String) -> Unit,
    canAddReview: Boolean,
    isSubscribed: Boolean,
    onSubscriptionToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = canteen.name, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onSubscriptionToggle) {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                        contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                        tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            AsyncImage(
                model = canteen.imageUrl,
                contentDescription = "Canteen image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = canteen.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = canteen.address, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Ocene Danas", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Dorucak", "Rucak", "Vecera").forEach { mealType ->
                    Button(onClick = { onMealTypeSelected(mealType) }) {
                        Text(mealType)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddReview,
                enabled = isNearCanteen && canAddReview
            ) {
                Text(
                    if (!isNearCanteen) "Get Closer to Add Review"
                    else if (!canAddReview) "Vec ste dodali rezencije"
                    else "Add Review"
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
fun MealReviewsDialog(
    canteen: Canteen?,
    mealType: String,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentReviewIndex by remember { mutableStateOf(0) }
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun getReviewsForDate(date: Calendar): List<MapReview> {
        return canteen?.mapReviews
            ?.filter { it.type == mealType && isSameDate(it.timestamp, date.timeInMillis) }
            ?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    var currentDateReviews by remember { mutableStateOf(getReviewsForDate(selectedDate)) }

    // Today's date for comparison
    val today = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$mealType Reviews") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Date navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
                            currentDateReviews = getReviewsForDate(selectedDate)
                            currentReviewIndex = 0
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
                    }

                    Text(dateFormatter.format(selectedDate.time))

                    IconButton(
                        onClick = {
                            if (selectedDate.before(today)) {
                                selectedDate.add(Calendar.DAY_OF_MONTH, 1)
                                currentDateReviews = getReviewsForDate(selectedDate)
                                currentReviewIndex = 0
                            }
                        },
                        enabled = selectedDate.before(today) && selectedDate.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Review display
                if (currentDateReviews.isNotEmpty()) {
                    MapReviewItem(currentDateReviews[currentReviewIndex])

                    // Navigation between reviews
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                currentReviewIndex = (currentReviewIndex - 1 + currentDateReviews.size) % currentDateReviews.size
                            },
                            enabled = currentDateReviews.size > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous review")
                        }

                        IconButton(
                            onClick = {
                                currentReviewIndex = (currentReviewIndex + 1) % currentDateReviews.size
                            },
                            enabled = currentDateReviews.size > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next review")
                        }
                    }
                } else {
                    Text("Nema recenzija za ovaj datum.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun MapReviewItem(review: MapReview) {
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(review) {
        review.user?.get()?.addOnSuccessListener { documentSnapshot ->
            user = documentSnapshot.toObject(User::class.java)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user?.imageUrl,
            contentDescription = "User avatar",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(user?.username ?: "Unknown User", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        Text(formatTimestamp(review.timestamp), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Ratings with stars - Displayed in a column
        Column {
            RatingStars("Hrana", review.foodRating)
            Spacer(modifier = Modifier.height(8.dp))
            RatingStars("Usluga", review.serviceRating)
            Spacer(modifier = Modifier.height(8.dp))
            RatingStars("Gužva", review.crowdRating)
        }
    }
}


@Composable
fun RatingStars(label: String, rating: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun ReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int) -> Unit,
    disabledMealTypes: List<String>
) {
    var mealType by remember { mutableStateOf("") }
    var foodRating by remember { mutableStateOf(3) }
    var serviceRating by remember { mutableStateOf(3) }
    var crowdRating by remember { mutableStateOf(3) }

    val mealTypes = listOf("Dorucak", "Rucak", "Vecera")
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Review") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Meal Type", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    mealTypes.forEach { type ->
                        OutlinedButton(
                            onClick = { mealType = type },
                            enabled = type !in disabledMealTypes,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (mealType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                RatingSlider("Hrana", foodRating) { foodRating = it }
                RatingSlider("Usluga", serviceRating) { serviceRating = it }
                RatingSlider("Gužva", crowdRating) { crowdRating = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(mealType, foodRating, serviceRating, crowdRating)
                },
                enabled = mealType.isNotEmpty()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun isReviewFromToday(timestamp: Long): Boolean {
    val reviewDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()

    return reviewDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            reviewDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun canAddReview(canteen: Canteen, currentUser: User?): Boolean {
    if (currentUser == null) return false

    val todayReviews = canteen.mapReviews.filter { isReviewFromToday(it.timestamp) }
    val userReviewedMealTypes = todayReviews
        .filter { it.user?.id == currentUser.id }
        .map { it.type }

    return userReviewedMealTypes.size < 3
}

fun getDisabledMealTypes(canteen: Canteen?, currentUser: User?): List<String> {
    if (canteen == null || currentUser == null) return listOf("Dorucak", "Rucak", "Vecera")

    val todayReviews = canteen.mapReviews.filter { isReviewFromToday(it.timestamp) }
    return todayReviews
        .filter { it.user?.id == currentUser.id }
        .map { it.type }
}

@Composable
fun RatingSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3
        )
    }
}

@Composable
fun RankingDialog(
    canteenViewModel: CanteenViewModel,
    onDismiss: () -> Unit
) {
    val rankings by canteenViewModel.fetchUserRankings().collectAsState(initial = emptyMap())
    var selectedTab by remember { mutableStateOf("Likes") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rankings", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                RankingTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                when (selectedTab) {
                    "Likes" -> RankingTable("Likes", rankings["likes"] ?: emptyList(), "likesCount")
                    "Statuses" -> RankingTable("Statuses", rankings["statuses"] ?: emptyList(), "postCount")
                    "Reviews" -> RankingTable("Reviews", rankings["reviews"] ?: emptyList(), "reviewCount")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RankingTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Likes", "Statuses", "Reviews").forEach { tab ->
            RankingTabCard(
                title = tab,
                isSelected = selectedTab == tab,
                onSelect = { onTabSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RankingTabCard(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


@Composable
fun RankingTable(title: String, users: List<User>, countField: String) {
    Column {
        Text(
            text = "$title Rankings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn {
            items(users.take(3).mapIndexed { index, user -> index to user }) { (index, user) ->
                RankingItem(user, index, countField)
            }
        }
    }
}

@Composable
fun RankingItem(user: User, position: Int, countField: String) {
    val borderColor = when (position) {
        0 -> Color(0xFFFFD700) // Gold
        1 -> Color(0xFFC0C0C0) // Silver
        2 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Transparent
    }

    val count = when (countField) {
        "likesCount" -> user.likesCount
        "postCount" -> user.postCount
        "reviewCount" -> user.reviewCount
        else -> 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(2.dp, borderColor, CircleShape)
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = user.imageUrl,
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Rank",
                    tint = borderColor,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Član od: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(user.memberSince.toDate())}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}



// Helper function to check if user is near the canteen (e.g., within 100 meters)
fun isUserNearCanteen(userLocation: LatLng, canteenLocation: LatLng): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        canteenLocation.latitude, canteenLocation.longitude,
        results
    )
    return results[0] <= 100 // 100 meters
}

fun isSameDate(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}


fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}