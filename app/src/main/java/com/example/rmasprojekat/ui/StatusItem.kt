package com.example.rmasprojekat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rmasprojekat.R
import com.example.rmasprojekat.model.Status
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun StatusItem(status: Status, authViewModel: AuthViewModel, modifier: Modifier = Modifier) {
    var comment by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedTimestamp = dateFormat.format(status.timestamp.toDate())

    var username by remember { mutableStateOf("Loading...") }
    var userImage by remember { mutableStateOf<String?>(null) }

    // Fetch the username from the reference
    LaunchedEffect(status.user) {
        status.user?.let { userRef ->
            authViewModel.fetchUserDetails(userRef) { user ->
                user?.let {
                    username = it.username
                    userImage = it.imageUrl
                } ?: run {
                    username = "Unknown"
                }
            }
        }
    }


    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Optional User Image
                    AsyncImage(
                        model = userImage ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = username, fontWeight = FontWeight.Bold)
                        Text(
                            text = formattedTimestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                // Like Button
                IconButton(onClick = { /* Handle like action */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_like),
                        contentDescription = "Like",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = status.message)

            // Optional Status Image

            status.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .scale(coil.size.Scale.FILL)
                        .build()
                    ),
                    contentDescription = "Status Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Adjust height as needed
                )
            }


            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                    .padding(18.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* Handle comment submission */ })
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { /* Handle comment action */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = "Comment", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
