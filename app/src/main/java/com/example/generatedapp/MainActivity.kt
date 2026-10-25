package com.example.generatedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashboardScreen()
        }
    }
}

@Composable
fun DashboardScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back button */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            UserBalanceCard(balance = 1000.00)
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons()
            Spacer(modifier = Modifier.height(16.dp))
            RecentTransactions(transactions = listOf(
                Transaction("Sent to John Doe", -50.00),
                Transaction("Received from Jane Smith", 75.00),
                Transaction("Sent to Alice Johnson", -30.00)
            ))
        }
    }
}

@Composable
fun UserBalanceCard(balance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        backgroundColor = Color.White,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Your Balance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$$balance", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ActionButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { /* Handle send money */ }) {
            Text(text = "Send Money")
        }
        Button(onClick = { /* Handle receive money */ }) {
            Text(text = "Receive Money")
        }
    }
}

data class Transaction(val description: String, val amount: Double)

@Composable
fun RecentTransactions(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { transaction ->
            Divider(color = Color.LightGray, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = transaction.description, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = if (transaction.amount > 0) "+${transaction.amount}" else "${transaction.amount}", color = if (transaction.amount > 0) Color.Green else Color.Red, fontSize = 16.sp)
            }
        }
    }
}