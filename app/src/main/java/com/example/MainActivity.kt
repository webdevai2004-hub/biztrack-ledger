package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.ExpenseColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "business_db"
        ).build()
        val repository = TransactionRepository(database.transactionDao())
        val factory = MainViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("হোম") },
                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Report") },
                    label = { Text("রিপোর্ট") },
                    selected = currentDestination?.hierarchy?.any { it.route == "report" } == true,
                    onClick = {
                        navController.navigate("report") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(viewModel, onNavigateToAdd = { navController.navigate("add") }) }
            composable("report") { ReportScreen(viewModel) }
            composable("add") { AddTransactionScreen(viewModel, onNavigateBack = { navController.popBackStack() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigateToAdd: () -> Unit) {
    val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    
    val totalIncome = currentMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val profit = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("হিসাব রক্ষক", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("চলতি মাসের হিসাব", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("মোট আয়", color = IncomeColor, fontWeight = FontWeight.Bold)
                            Text("৳$totalIncome", style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("মোট ব্যয়", color = ExpenseColor, fontWeight = FontWeight.Bold)
                            Text("৳$totalExpense", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("বর্তমান স্থিতি / লাভ", fontWeight = FontWeight.Bold)
                        Text(
                            "৳$profit",
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) IncomeColor else ExpenseColor,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            Text(
                "সাম্প্রতিক লেনদেন", 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentMonthTransactions.take(20)) { t ->
                    TransactionItem(t, onDelete = { viewModel.deleteTransaction(t.id) })
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(sdf.format(Date(transaction.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = "${if(isIncome) "+" else "-"} ৳${transaction.amount}",
                color = if (isIncome) IncomeColor else ExpenseColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("নতুন লেনদেন") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    label = { Text("আয় (Income)") }
                )
                FilterChip(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    label = { Text("ব্যয় (Expense)") }
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("পরিমাণ (৳)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("বিবরণ") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.addTransaction(
                            amount = amount,
                            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                            description = description.ifBlank { if (isIncome) "আয়" else "ব্যয়" }
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("সেভ করুন", fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val groupedTransactions by viewModel.monthlyGroupedTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("মাসিক রিপোর্ট") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (groupedTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("কোনো তথ্য নেই")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val months = groupedTransactions.keys.toList()
                items(months) { monthLabel ->
                    val txs = groupedTransactions[monthLabel] ?: emptyList()
                    val totalInc = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val totalExp = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val profit = totalInc - totalExp

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("আয়: ৳$totalInc", color = IncomeColor, fontWeight = FontWeight.Medium)
                                Text("ব্যয়: ৳$totalExp", color = ExpenseColor, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "লাভ/ক্ষতি: ৳$profit",
                                color = if (profit >= 0) IncomeColor else ExpenseColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
