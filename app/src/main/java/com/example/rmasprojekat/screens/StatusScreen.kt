package com.example.rmasprojekat.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.example.rmasprojekat.model.Status
import com.example.rmasprojekat.viewmodel.AuthViewModel

@Composable
fun StatusScreen(canteenViewModel: CanteenViewModel, authViewModel: AuthViewModel) {
    val currentCanteen = canteenViewModel.getCurrentCanteen()
    val canteenName = currentCanteen?.name ?: "N/A"
    val canteenNickname = currentCanteen?.nickname ?: "N/A"

    val statuses by canteenViewModel.statuses.collectAsState()

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = canteenName,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = canteenNickname,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (statuses.isEmpty()) {
            // Display empty state with an icon and message
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning, // Use a modern icon from the Material library
                    contentDescription = "No statuses",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trenutno nema statusa za izabranu menzu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Show statuses
            LazyColumn {
                items(statuses.values.toList(), key = { it.id ?: "" }) { status ->
                    key(status.id) {
                        StatusItem(
                            status = status,
                            authViewModel = authViewModel,
                            canteenViewModel = canteenViewModel
                        )
                    }
                }
            }
        }
    }
}
