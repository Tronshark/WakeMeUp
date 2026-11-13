package com.example.wakemeup.ui.about

// Author: TronShark
// Version: v1.0

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WakeMeUp",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version v1.0",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Author: TronShark",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Tronshark/WakeMeUp"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("GitHub Repository")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Tronshark"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Donation")
            }
        }
    }
}
