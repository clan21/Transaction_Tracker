package com.example.myapplication

import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.Window
import androidx.compose.ui.graphics.toArgb

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Xml
import java.util.Date
import android.os.Build
import org.xmlpull.v1.XmlSerializer
import androidx.annotation.RequiresApi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Send

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val db = DatabaseProvider.getDatabase(applicationContext)
            MaterialTheme {
                KazibuFastApp(
                    dao = db.transactionDao(),
                    archiveDao = db.monthlyArchiveDao(),
                    lendingDao = db.lendingDao(),
                    gcashDao = db.gcashDao()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KazibuFastApp(
    dao: TransactionDao,
    archiveDao: MonthlyArchiveDao,
    lendingDao: LendingDao,
    gcashDao: GcashDao
) {
    var currentScreen by remember { mutableStateOf("home") }
    var showSearchBar by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    // Reset the search whenever the screen changes
    LaunchedEffect(currentScreen) {
        showSearchBar = false
        searchText = ""
    }

    BackHandler {
        when {
            // If search is open, close it first
            showSearchBar -> {
                showSearchBar = false
                searchText = ""
            }
            // From History, go back to Home
            currentScreen == "history" -> {
                currentScreen = "home"
            }
            // From Wrap Up, go back to Home
            currentScreen == "wrapup" -> {
                currentScreen = "home"
            }
            currentScreen == "archives" -> {
                currentScreen = "home"
            }
            currentScreen == "lending" -> {
                currentScreen = "home"
            }
            currentScreen == "gcash" -> {
                currentScreen = "home"
            }
            // On Home, ask before closing the app
            currentScreen == "home" -> {
                showExitDialog = true
            }
        }
    }

    val context = LocalContext.current
    val transactions by dao.getAll().collectAsState(initial = emptyList())
    val lending by lendingDao.getAll().collectAsState(initial = emptyList())
    val gcash by gcashDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val totals = computeTotals(transactions, lending, gcash)

    val collection = totals.homeCollection // business income: cash + GCash, no personal
    val totalExpenses = totals.expenses
    val personalIncome = totals.personalIncome // includes GCash charges
    val personalExpense = totals.personalExpenses
    val balance = totals.balance
    val remit = totals.remit

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showRemitDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    if (context is Activity) {
        context.window.statusBarColor =
            MaterialTheme.colorScheme.background.toArgb()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Kazibufast Networks logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                NavigationDrawerItem(
                    label = { Text("Home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = currentScreen == "home",
                    onClick = {
                        currentScreen = "home"
                        showSearchBar = false
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("History") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = currentScreen == "history",
                    onClick = {
                        currentScreen = "history"
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Lending") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    selected = currentScreen == "lending",
                    onClick = {
                        currentScreen = "lending"
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("GCash") },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    selected = currentScreen == "gcash",
                    onClick = {
                        currentScreen = "gcash"
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Wrap Up") },
                    icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                    selected = currentScreen == "wrapup",
                    onClick = {
                        currentScreen = "wrapup"
                        showSearchBar = false

                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Monthly Archives") },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    selected = currentScreen == "archives",
                    onClick = {
                        currentScreen = "archives"
                        showSearchBar = false

                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Exit") },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        (context as? Activity)?.finish()
                    }
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Text("☰", fontSize = 28.sp)
                        }
                    },

                    title = {
                        Text(
                            when (currentScreen) {
                                "home" -> "Transaction Tracker"
                                "history" -> "History"
                                "lending" -> "Lending"
                                "gcash" -> "GCash"
                                "wrapup" -> "Wrap Up"
                                "archives" -> "Monthly Archives"
                                else -> "Transaction Tracker"
                            }
                        )
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFffffff)
                    ),

                    actions = {

                        if (currentScreen == "history" ||
                            currentScreen == "lending" ||
                            currentScreen == "gcash"
                        ) {

                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            when (currentScreen) {
                "home" -> {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            StatCard("Collection", collection, Modifier.weight(1f), Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50))
                            Spacer(Modifier.width(12.dp))
                            StatCard("Expenses", totalExpenses, Modifier.weight(1f), Icons.Default.Receipt, Color(0xFFF44336))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatCard("Personal Income", personalIncome, Modifier.weight(1f), Icons.Default.PersonAdd, Color(0xFF4CAF50))
                            Spacer(Modifier.width(12.dp))
                            StatCard("Personal Expenses", personalExpense, Modifier.weight(1f), Icons.Default.PersonRemove, Color(0xFFF44336))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatCard("Balance", balance, Modifier.weight(1f), Icons.Default.AccountBalance, Color(0xFF1C9999))
                            Spacer(Modifier.width(12.dp))
                            StatCard("Remittances", remit, Modifier.weight(1f), Icons.Default.Send, Color(0xFF6750A4))
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeActionButton(
                                text = "New Income",
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFF4CAF50)
                            ) { showIncomeDialog = true }

                            HomeActionButton(
                                text = "New Expense",
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFF44336)
                            ) { showExpenseDialog = true }

                            HomeActionButton(
                                text = "New Remit",
                                modifier = Modifier.weight(1f)
                            ) { showRemitDialog = true }
                        }

                        Spacer(Modifier.height(24.dp))

// Recent transactions
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        val recentTransactions = transactions
                            .sortedByDescending { it.id }
                            .take(4)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(recentTransactions) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    onToggle = {
                                        scope.launch {
                                            dao.update(
                                                transaction.copy(
                                                    isChecked = !transaction.isChecked
                                                )
                                            )
                                        }
                                    }
                                )
                            }

                            item {
                                Spacer(Modifier.height(0.dp))

                                Button(
                                    onClick = {
                                        currentScreen = "history"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Show More")
                                }

                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                "history" -> {
                    HistoryScreen(
                        transactions = transactions,
                        dao = dao,
                        searchText = searchText,
                        showSearchBar = showSearchBar,
                        onSearchTextChange = {
                            searchText = it
                        },
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    )
                }
                "lending" -> {
                    LendingScreen(
                        lending = lending,
                        dao = lendingDao,
                        searchText = searchText,
                        showSearchBar = showSearchBar,
                        onSearchTextChange = { searchText = it },
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    )
                }
                "gcash" -> {
                    GcashScreen(
                        gcash = gcash,
                        dao = gcashDao,
                        searchText = searchText,
                        showSearchBar = showSearchBar,
                        onSearchTextChange = { searchText = it },
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    )
                }
                "wrapup" -> {
                    WrapUpScreen(
                        transactions = transactions,
                        lending = lending,
                        gcash = gcash,
                        dao = dao,
                        archiveDao = archiveDao,
                        lendingDao = lendingDao,
                        gcashDao = gcashDao,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        onViewArchives = {
                            currentScreen = "archives"
                        }
                    )
                }
                "archives" -> {
                    ArchivesScreen(
                        archiveDao = archiveDao,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    if (showIncomeDialog) {
        AmountDialog(
            title = "New Income",
            onDismiss = { showIncomeDialog = false },
            onConfirm = {
                    amount,
                    details,
                    date,
                    isPersonal,
                    paymentMethod ->
                scope.launch {

                    dao.insert(
                        Transaction(
                            amount = amount,
                            details = details,
                            date = date,
                            type = if (isPersonal)
                                "personalIncome"
                            else
                                "income",
                            paymentMethod = paymentMethod
                        )
                    )
                }
                showIncomeDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            onDismiss = {
                showExpenseDialog = false
            },
            onConfirm = {
                    amount,
                    details,
                    date,
                    isPersonal,
                    paymentMethod,
                    expenseCategory ->

                scope.launch {
                    dao.insert(
                        Transaction(
                            amount = amount,
                            details = details,
                            date = date,
                            type = if (isPersonal)
                                "personal"
                            else
                                "expense",
                            paymentMethod = paymentMethod,
                            expenseCategory = expenseCategory
                        )
                    )
                }
                showExpenseDialog = false
            }
        )
    }

    if (showRemitDialog) {
        RemitDialog(
            onDismiss = { showRemitDialog = false },
            onConfirm = { amount, details, date, paymentMethod ->
                scope.launch {
                    dao.insert(
                        Transaction(
                            amount = amount,
                            details = details,
                            date = date,
                            type = "remit",
                            paymentMethod = paymentMethod
                        )
                    )
                }
                showRemitDialog = false
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },

            title = {
                Text("Close App?")
            },

            text = {
                Text("Would you like to close the app?")
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("YES")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text("NO")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = Color(0xFF1C9999)
) {
    MoneyCard(label, amount, modifier, icon, iconTint)
}

@OptIn(ExperimentalMaterial3Api::class)

fun LazyListScope.transactionGroup(
    title: String,
    transactions: List<Transaction>,
    onToggle: (Transaction) -> Unit = {}
) {
    if (transactions.isEmpty()) return

    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    items(transactions) { transaction ->
        TransactionRow(
            transaction = transaction,
            onToggle = {
                onToggle(transaction)
            }
        )
    }
}

fun sortForMonthlyReport(
    transactions: List<Transaction>
): List<Transaction> {

    val typeOrder = mapOf(
        "income" to 0,
        "expense" to 1,
        "personalIncome" to 2,
        "personal" to 3,
        "remit" to 4
    )

    val formatter = SimpleDateFormat(
        "MM/dd/yyyy",
        Locale.getDefault()
    )

    return transactions.sortedWith(
        compareBy<Transaction> {

            typeOrder[it.type] ?: 99

        }.thenBy {

            try {
                formatter.parse(it.date)?.time
                    ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }

        }.thenBy {

            it.id
        }
    )
}

@Suppress("DEPRECATION")
fun exportMonthlyReports(
    context: Context,
    transactions: List<Transaction>,
    lending: List<LendingEntry>,
    gcash: List<GcashTransaction>
) {
    if (transactions.isEmpty() && lending.isEmpty() && gcash.isEmpty()) {
        throw IllegalStateException(
            "There are no transactions to wrap up."
        )
    }

    val sortedTransactions =
        sortForMonthlyReport(transactions)

    val monthName = SimpleDateFormat(
        "MMMM_yyyy",
        Locale.getDefault()
    ).format(Date())

    val pdfFileName =
        "KazibuFast_${monthName}.pdf"

    val xmlFileName =
        "KazibuFast_${monthName}.xml"

    exportPdf(
        context = context,
        transactions = sortedTransactions,
        lending = lending,
        gcash = gcash,
        fileName = pdfFileName
    )

    exportXml(
        context = context,
        transactions = sortedTransactions,
        lending = lending,
        gcash = gcash,
        fileName = xmlFileName
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getDownloadsUri(): Uri {
    return MediaStore.Downloads.EXTERNAL_CONTENT_URI
}

fun exportPdf(
    context: Context,
    transactions: List<Transaction>,
    lending: List<LendingEntry>,
    gcash: List<GcashTransaction>,
    fileName: String
) {
    val resolver = context.contentResolver

    val values = ContentValues().apply {
        put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            fileName
        )

        put(
            MediaStore.MediaColumns.MIME_TYPE,
            "application/pdf"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getDownloadsUri()
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val uri = resolver.insert(
        collection,
        values
    ) ?: throw IllegalStateException(
        "Unable to create PDF file."
    )

    resolver.openOutputStream(uri)?.use { outputStream ->

        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val normalPaint = Paint().apply {
            textSize = 10f
        }

        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }

        val t = computeTotals(transactions, lending, gcash)

        val income = t.incomeTotal
        val expenses = t.expenses
        val personalIncome = t.personalIncome
        val personalExpense = t.personalExpenses
        val remit = t.remit
        val balance = t.balance

        var pageNumber = 1

        fun createPage(): PdfDocument.Page {

            return document.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()
            )
        }

        var page = createPage()
        var canvas = page.canvas
        var y = 40f

        fun newPage() {

            document.finishPage(page)

            pageNumber++

            page = createPage()
            canvas = page.canvas
            y = 40f
        }

        fun drawLine(text: String) {

            if (y > pageHeight - 40f) {
                newPage()
            }

            canvas.drawText(
                text,
                30f,
                y,
                normalPaint
            )

            y += 18f
        }

        canvas.drawText(
            "KAZIBUFAST",
            30f,
            y,
            titlePaint
        )

        y += 25f

        canvas.drawText(
            "MONTHLY TRANSACTION REPORT",
            30f,
            y,
            headerPaint
        )

        y += 25f

        canvas.drawText(
            SimpleDateFormat(
                "MMMM yyyy",
                Locale.getDefault()
            ).format(Date()),
            30f,
            y,
            normalPaint
        )

        y += 35f

        canvas.drawText(
            "SUMMARY",
            30f,
            y,
            headerPaint
        )

        y += 22f

        drawLine(
            "Total Transactions: ${transactions.size + lending.size + gcash.size}"
        )

        drawLine(
            "Total Income: ₱${"%.2f".format(income)}"
        )

        drawLine(
            "Total Expenses: ₱${"%.2f".format(expenses)}"
        )

        drawLine(
            "Personal Income: ₱${"%.2f".format(personalIncome)}"
        )

        drawLine(
            "Personal Expenses: ₱${"%.2f".format(personalExpense)}"
        )

        drawLine(
            "Remittances: ₱${"%.2f".format(remit)}"
        )

        drawLine("Borrowed (unreturned): ₱${"%.2f".format(t.borrowedOutstanding)}")
        drawLine("Returned: ₱${"%.2f".format(t.returnedTotal)}")
        drawLine("GCash Cash-in: ₱${"%.2f".format(t.cashIn)}")
        drawLine("GCash Cash-out: ₱${"%.2f".format(t.cashOut)}")
        drawLine("GCash Charges: ₱${"%.2f".format(t.gcashCharges)}")
        drawLine("Cash on Hand: ₱${"%.2f".format(t.cashOnHand)}")

        y += 5f

        canvas.drawText(
            "Balance: ₱${"%.2f".format(balance)}",
            30f,
            y,
            headerPaint
        )

        y += 35f

        canvas.drawText(
            "TRANSACTIONS",
            30f,
            y,
            headerPaint
        )

        y += 25f

        var previousType = ""

        for (transaction in transactions) {

            if (transaction.type != previousType) {

                if (y > pageHeight - 70f) {
                    newPage()
                }

                val sectionTitle = when (transaction.type) {
                    "income" -> "INCOME"
                    "expense" -> "EXPENSES"
                    "personalIncome" -> "PERSONAL INCOME"
                    "personal" -> "PERSONAL EXPENSES"
                    "remit" -> "REMITTANCES"
                    else -> "OTHER"
                }

                y += 8f

                canvas.drawText(
                    sectionTitle,
                    30f,
                    y,
                    headerPaint
                )

                y += 22f

                previousType = transaction.type
            }

            if (y > pageHeight - 40f) {
                newPage()
            }

            val amount = when (transaction.type) {
                "income", "personalIncome" ->
                    "+₱${"%.2f".format(transaction.amount)}"

                "expense", "personal" ->
                    "-₱${"%.2f".format(transaction.amount)}"

                else ->
                    "₱${"%.2f".format(transaction.amount)}"
            }

            val details = transaction.details
                .replace("\n", " ")
                .take(45)

            val checked =
                if (transaction.isChecked) "✓" else ""

            canvas.drawText(
                "${transaction.date}  $details",
                30f,
                y,
                normalPaint
            )

            canvas.drawText(
                amount,
                460f,
                y,
                normalPaint
            )

            if (checked.isNotEmpty()) {
                canvas.drawText(
                    checked,
                    540f,
                    y,
                    normalPaint
                )
            }

            y += 18f
        }

        if (lending.isNotEmpty()) {
            if (y > pageHeight - 70f) newPage()
            y += 8f
            canvas.drawText("LENDING", 30f, y, headerPaint)
            y += 22f

            for (e in lending.sortedBy { it.id }) {
                if (y > pageHeight - 40f) newPage()
                val note = when (e.status) {
                    "Returned" -> "Returned"
                    "Incomplete" -> "Incomplete, bal ${"%.2f".format(e.balance)}"
                    else -> "Borrowed"
                }
                canvas.drawText(
                    "${e.date}  ${e.details.replace("\n", " ").take(28)}  [$note]",
                    30f, y, normalPaint
                )
                canvas.drawText("₱${"%.2f".format(e.amount)}", 460f, y, normalPaint)
                y += 18f
            }
        }

        if (gcash.isNotEmpty()) {
            if (y > pageHeight - 70f) newPage()
            y += 8f
            canvas.drawText("GCASH", 30f, y, headerPaint)
            y += 22f

            for (g in gcash.sortedBy { it.id }) {
                if (y > pageHeight - 40f) newPage()
                val label = if (g.type == GCASH_CASH_IN) "Cash-in" else "Cash-out"
                canvas.drawText(
                    "${g.date}  ${g.details.replace("\n", " ").take(25)}  [$label, fee ${"%.2f".format(g.charge)}]",
                    30f, y, normalPaint
                )
                canvas.drawText("₱${"%.2f".format(g.amount)}", 460f, y, normalPaint)
                y += 18f
            }
        }

        document.finishPage(page)

        document.writeTo(outputStream)

        document.close()
    }
}

fun exportXml(
    context: Context,
    transactions: List<Transaction>,
    lending: List<LendingEntry>,
    gcash: List<GcashTransaction>,
    fileName: String
) {
    val resolver = context.contentResolver

    val values = ContentValues().apply {
        put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            fileName
        )

        put(
            MediaStore.MediaColumns.MIME_TYPE,
            "application/xml"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getDownloadsUri()
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val uri = resolver.insert(
        collection,
        values
    ) ?: throw IllegalStateException(
        "Unable to create XML file."
    )

    resolver.openOutputStream(uri)?.use { outputStream ->

        val serializer = Xml.newSerializer()

        serializer.setOutput(
            outputStream,
            "UTF-8"
        )

        serializer.startDocument(
            "UTF-8",
            true
        )

        serializer.startTag(
            null,
            "monthlyReport"
        )

        serializer.attribute(
            null,
            "month",
            SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
            ).format(Date())
        )

        serializer.startTag(
            null,
            "transactions"
        )

        for (transaction in transactions) {

            serializer.startTag(
                null,
                "transaction"
            )

            writeXmlValue(
                serializer,
                "id",
                transaction.id.toString()
            )

            writeXmlValue(
                serializer,
                "amount",
                "%.2f".format(transaction.amount)
            )

            writeXmlValue(
                serializer,
                "details",
                transaction.details
            )

            writeXmlValue(
                serializer,
                "date",
                transaction.date
            )

            writeXmlValue(
                serializer,
                "type",
                transaction.type
            )

            writeXmlValue(
                serializer,
                "isChecked",
                transaction.isChecked.toString()
            )

            writeXmlValue(
                serializer,
                "paymentMethod",
                transaction.paymentMethod
            )

            writeXmlValue(
                serializer,
                "expenseCategory",
                transaction.expenseCategory ?: ""
            )

            serializer.endTag(
                null,
                "transaction"
            )
        }

        serializer.endTag(
            null,
            "transactions"
        )

        serializer.startTag(null, "lending")
        for (e in lending) {
            serializer.startTag(null, "entry")
            writeXmlValue(serializer, "id", e.id.toString())
            writeXmlValue(serializer, "details", e.details)
            writeXmlValue(serializer, "date", e.date)
            writeXmlValue(serializer, "amount", "%.2f".format(e.amount))
            writeXmlValue(serializer, "returnedAmount", "%.2f".format(e.returnedAmount))
            writeXmlValue(serializer, "status", e.status)
            serializer.endTag(null, "entry")
        }
        serializer.endTag(null, "lending")

        serializer.startTag(null, "gcash")
        for (g in gcash) {
            serializer.startTag(null, "entry")
            writeXmlValue(serializer, "id", g.id.toString())
            writeXmlValue(serializer, "details", g.details)
            writeXmlValue(serializer, "date", g.date)
            writeXmlValue(serializer, "type", g.type)
            writeXmlValue(serializer, "amount", "%.2f".format(g.amount))
            writeXmlValue(serializer, "charge", "%.2f".format(g.charge))
            serializer.endTag(null, "entry")
        }
        serializer.endTag(null, "gcash")

        serializer.endTag(
            null,
            "monthlyReport"
        )

        serializer.endDocument()
    }
}

fun writeXmlValue(
    serializer: XmlSerializer,
    name: String,
    value: String
) {
    serializer.startTag(null, name)
    serializer.text(value)
    serializer.endTag(null, name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, details: String, date: String, isPersonal: Boolean, paymentMethod: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.getDefault()
            ).format(Date())
        )
    }
    var isPersonal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {}, // read-only, filled by the picker
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Text("📅")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "Cash",
                        onClick = {
                            paymentMethod = "Cash"
                        }
                    )
                    Text("Cash")

                    Spacer(Modifier.width(16.dp))
                    RadioButton(
                        selected = paymentMethod == "GCash",
                        onClick = {
                            paymentMethod = "GCash"
                        }
                    )
                    Text("GCash")
                }
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPersonal,
                        onCheckedChange = {
                            isPersonal = it
                        }
                    )
                    Text("Personal Income")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                amountText.toDoubleOrNull()?.let {
                    onConfirm(
                        it,
                        details,
                        selectedDate,
                        isPersonal,
                        paymentMethod
                    )
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        selectedDate = formatter.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, details: String, date: String, isPersonal: Boolean, paymentMethod: String,
                expenseCategory: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.getDefault()
            ).format(Date())
        )
    }
    var isPersonal by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var expenseCategory by remember { mutableStateOf("Others") }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Text("📅")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "Cash",
                        onClick = {
                            paymentMethod = "Cash"
                        }
                    )
                    Text("Cash")
                    Spacer(Modifier.width(16.dp))

                    RadioButton(
                        selected = paymentMethod == "GCash",
                        onClick = {
                            paymentMethod = "GCash"
                        }
                    )
                    Text("GCash")
                }
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = {
                        categoryExpanded = !categoryExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = expenseCategory,
                        onValueChange = {},
                        readOnly = true,

                        label = {
                            Text("Expense Category")
                        },

                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = categoryExpanded
                            )
                        },

                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = {
                            categoryExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Fuel")
                            },
                            onClick = {
                                expenseCategory = "Fuel"
                                categoryExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Salary")
                            },
                            onClick = {
                                expenseCategory = "Salary"
                                categoryExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Others")
                            },
                            onClick = {
                                expenseCategory = "Others"
                                categoryExpanded = false
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPersonal,
                        onCheckedChange = {
                            isPersonal = it
                        }
                    )
                    Text("Personal Expense")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                amountText.toDoubleOrNull()?.let { onConfirm(it, details, selectedDate, isPersonal, paymentMethod,
                    expenseCategory) }
            }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        selectedDate = formatter.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemitDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, details: String, date: String, paymentMethod: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.getDefault()
            ).format(Date())
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Remittance") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {}, // read-only, filled by the picker
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Text("📅")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "Cash",
                        onClick = { paymentMethod = "Cash" }
                    )
                    Text("Cash")

                    Spacer(Modifier.width(16.dp))

                    RadioButton(
                        selected = paymentMethod == "GCash",
                        onClick = { paymentMethod = "GCash" }
                    )
                    Text("GCash")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                amountText.toDoubleOrNull()?.let { onConfirm(it, details, selectedDate, paymentMethod) }
            }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        selectedDate = formatter.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun WrapUpScreen(
    transactions: List<Transaction>,
    lending: List<LendingEntry>,
    gcash: List<GcashTransaction>,
    dao: TransactionDao,
    archiveDao: MonthlyArchiveDao,
    lendingDao: LendingDao,
    gcashDao: GcashDao,
    modifier: Modifier = Modifier,
    onViewArchives: () -> Unit
) {
    var showConfirmation by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isWrappingUp by remember {
        mutableStateOf(false)
    }

    var resultMessage by remember {
        mutableStateOf<String?>(null)
    }

    // =========================
    // MONTHLY SUMMARY
    // =========================

    val t = computeTotals(transactions, lending, gcash)

    val totalTransactions = transactions.size + lending.size + gcash.size

    val totalIncome = t.incomeTotal
    val totalExpenses = t.expenses
    val totalPersonalIncome = t.personalIncome
    val totalPersonalExpenses = t.personalExpenses
    val totalRemittances = t.remit
    val totalCollections = t.collections
    val cashOnHand = t.cashOnHand

    // =========================
    // CURRENT MONTH
    // =========================

    val currentMonth = SimpleDateFormat(
        "MMMM yyyy",
        Locale.getDefault()
    ).format(Date())

    // =========================
    // SCREEN
    // =========================

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        // =========================
        // MONTH
        // =========================

        Text(
            text = currentMonth,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        // =========================
        // TOTAL TRANSACTIONS
        // =========================

        Text(
            text = "Total Transactions: $totalTransactions",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

// =========================
// SUMMARY CARDS
// =========================

        val cardPages = listOf(

            // PAGE 1
            listOf(
                "Total Collections" to t.collections,
                "Total Income" to t.incomeTotal,
                "Total Expenses" to t.expenses,
                "Personal Income" to t.personalIncome,
                "Personal Expenses" to t.personalExpenses,
                "Total Remittances" to t.remit
            ),

            // PAGE 2
            listOf(
                "Cash on Hand" to t.cashOnHand,
                "Fuel" to t.fuel,
                "Salary" to t.salary,
                "Others" to t.others,
                "GCash Income" to t.gcashIncome,
                "GCash Expenses" to t.gcashExpenses
            ),

            // PAGE 3
            listOf(
                "Borrowed" to t.borrowedOutstanding,
                "Returned" to t.returnedTotal,
                "GCash Cash-in" to t.cashIn,
                "GCash Cash-out" to t.cashOut,
                "GCash Charges" to t.gcashCharges,
                "GCash Balance" to t.gcashBalance
            )
        )

        val pagerState = rememberPagerState(
            pageCount = {
                cardPages.size
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { page ->

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ROW 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = cardPages[page][0].first,
                        amount = cardPages[page][0].second,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = cardPages[page][1].first,
                        amount = cardPages[page][1].second,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = cardPages[page][2].first,
                        amount = cardPages[page][2].second,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = cardPages[page][3].first,
                        amount = cardPages[page][3].second,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SummaryCard(
                        title = cardPages[page][4].first,
                        amount = cardPages[page][4].second,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = cardPages[page][5].first,
                        amount = cardPages[page][5].second,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            repeat(cardPages.size) { index ->

                val isSelected = pagerState.currentPage == index

                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(
                            if (isSelected) 20.dp else 8.dp
                        )
                        .height(8.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (isSelected)
                                Color(0xFF2BFFFF)
                            else
                                Color.LightGray
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =========================
        // WRAP UP BUTTON
        // =========================

        Button(
            onClick = {
                showConfirmation = true
            },
            enabled = !isWrappingUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1c9999),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isWrappingUp)
                    "Wrapping Up..."
                else
                    "WRAP UP THIS MONTH"
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // =========================
        // MONTH ARCHIVES BUTTON
        // =========================

        OutlinedButton(
            onClick = onViewArchives,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFf1f2d5),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "VIEW MONTH ARCHIVES"
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =========================
        // INFORMATION BOX
        // =========================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEFFFFF)
            )
        ) {

            Text(
                text =
                    "Wrapping up will generate your monthly " +
                            "PDF and XML reports and reset " +
                            "this month's transactions and GCash " +
                            "records. Fully returned lending entries " +
                            "are removed; unpaid balances carry over.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }

    // =========================
    // CONFIRMATION DIALOG
    // =========================

    if (showConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showConfirmation = false
            },

            title = {
                Text("Wrap Up Month?")
            },

            text = {
                Text(
                    "Are you sure you want to wrap up " +
                            "this month's transactions?\n\n" +
                            "A PDF report and a detailed XML " +
                            "backup will be generated. After " +
                            "both files are successfully created, " +
                            "the current transactions will be cleared."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showConfirmation = false
                        isWrappingUp = true

                        scope.launch {

                            try {

                                exportMonthlyReports(
                                    context = context,
                                    transactions = transactions,
                                    lending = lending,
                                    gcash = gcash
                                )

                                archiveDao.insert(
                                    MonthlyArchive(
                                        monthYear = currentMonth,
                                        totalTransactions = totalTransactions,
                                        totalCollections = totalCollections,
                                        totalIncome = totalIncome,
                                        totalExpenses = totalExpenses,
                                        totalPersonalIncome = totalPersonalIncome,
                                        totalPersonalExpenses = totalPersonalExpenses,
                                        totalRemittances = totalRemittances,
                                        cashOnHand = cashOnHand,
                                        totalBorrowed = lending.sumOf { it.amount },
                                        totalReturned = t.returnedTotal,
                                        gcashCashIn = t.cashIn,
                                        gcashCashOut = t.cashOut,
                                        gcashCharges = t.gcashCharges
                                    )
                                )

                                // Only clear after successful export
                                dao.deleteAll()
                                gcashDao.deleteAll()
                                lendingDao.deleteFullyReturned() // fully paid entries are done
                                lendingDao.rollOver()            // unpaid balances carry over

                                resultMessage =
                                    "Month successfully wrapped up."

                            } catch (e: Exception) {

                                e.printStackTrace()

                                resultMessage =
                                    "Wrap Up failed. " +
                                            "Your transactions were not deleted."
                            }

                            isWrappingUp = false
                        }
                    }
                ) {
                    Text("YES")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showConfirmation = false
                    }
                ) {
                    Text("NO")
                }
            }
        )
    }

    // =========================
    // RESULT DIALOG
    // =========================

    resultMessage?.let { message ->

        AlertDialog(
            onDismissRequest = {
                resultMessage = null
            },

            title = {

                Text(
                    if (message.startsWith("Month successfully"))
                        "Wrap Up Complete"
                    else
                        "Wrap Up Failed"
                )
            },

            text = {
                Text(message)
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        resultMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ArchivesScreen(
    archiveDao: MonthlyArchiveDao,
    modifier: Modifier = Modifier
) {
    val archives by archiveDao.getAllArchives().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    if (archives.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No archives yet")
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(archives) { archive ->
            ArchiveCard(
                archive = archive,
                onDelete = { scope.launch { archiveDao.deleteArchive(archive.id) } }
            )
        }
    }
}

@Composable
fun ArchiveCard(archive: MonthlyArchive, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(archive.monthYear, style = MaterialTheme.typography.titleMedium, color = Color.Blue)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete archive", tint = Color.Red)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Transactions: ${archive.totalTransactions}")
            Text("Collections: ₱${"%,.2f".format(archive.totalCollections)}")
            Text("Income: ₱${"%,.2f".format(archive.totalIncome)}")
            Text("Expenses: ₱${"%,.2f".format(archive.totalExpenses)}")
            Text("Personal Income: ₱${"%,.2f".format(archive.totalPersonalIncome)}")
            Text("Personal Expenses: ₱${"%,.2f".format(archive.totalPersonalExpenses)}")
            Text("Remittances: ₱${"%,.2f".format(archive.totalRemittances)}")
            Text("Borrowed: ${peso(archive.totalBorrowed)}")
            Text("Returned: ${peso(archive.totalReturned)}")
            Text("GCash Cash-in: ${peso(archive.gcashCashIn)}")
            Text("GCash Cash-out: ${peso(archive.gcashCashOut)}")
            Text("GCash Charges: ${peso(archive.gcashCharges)}")
            Spacer(Modifier.height(4.dp))
            Text(
                "Cash on Hand: ₱${"%,.2f".format(archive.cashOnHand)}"
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Archive?") },
            text = { Text("This permanently removes this month's archived record. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    MoneyCard(title, amount, modifier)
}

@Composable
fun parseTransactionsXml(inputStream: InputStream): List<Transaction> {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(inputStream, "UTF-8")

    val result = mutableListOf<Transaction>()
    var eventType = parser.eventType

    var amount = 0.0
    var details = ""
    var date = ""
    var type = ""
    var isChecked = false

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "transaction" -> {
                        amount = 0.0; details = ""; date = ""; type = ""; isChecked = false
                    }
                    "amount" -> amount = parser.nextText().toDoubleOrNull() ?: 0.0
                    "details" -> details = parser.nextText()
                    "date" -> date = parser.nextText()
                    "type" -> type = parser.nextText()
                    "isChecked" -> isChecked = parser.nextText().toBoolean()
                }
            }
            XmlPullParser.END_TAG -> {
                if (parser.name == "transaction") {
                    result.add(
                        Transaction(
                            amount = amount,
                            details = details,
                            date = date,
                            type = type,
                            isChecked = isChecked
                        )
                    )
                }
            }
        }
        eventType = parser.next()
    }
    return result
}