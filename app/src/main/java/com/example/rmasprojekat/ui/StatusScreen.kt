package com.example.rmasprojekat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = canteenNickname,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(statuses.values.toList(), key = { it.id ?: "" }) { status ->
                key(status.id) {
                    StatusItem(status = status, authViewModel = authViewModel, canteenViewModel = canteenViewModel)
                }
            }
        }
    }
}