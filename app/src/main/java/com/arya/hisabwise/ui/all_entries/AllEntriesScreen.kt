package com.arya.hisabwise.ui.all_entries

import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.arya.hisabwise.ui.theme.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.arya.hisabwise.data.local.ExpenseEntity
import com.arya.hisabwise.data.local.MemberEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun AllEntriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AllEntriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    GlowingBackground {
        Scaffold(
            containerColor = Color.Transparent,
        bottomBar = {
            if (state.hisab != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .imePadding()
                ) {
                    AddEntryButton(onClick = { viewModel.onEvent(AllEntriesEvent.OpenDialogForCreate) })
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // STICKY HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hisab for ${state.hisab?.name ?: "..."}",
                        color = PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    val countText = buildAnnotatedString {
                        append("total ")
                        withStyle(style = SpanStyle(color = PrimaryAccent)) {
                            append("${state.filteredExpenses.size}")
                        }
                        append(" entries")
                    }
                    
                    Text(
                        text = countText,
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }
            }

            // MAIN SCROLLABLE CONTENT
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SEARCH BAR
                item {
                    var isSearchFocused by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(SurfaceColor, RoundedCornerShape(28.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MutedText,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onEvent(AllEntriesEvent.SearchQueryChanged(it)) },
                                textStyle = TextStyle(
                                    color = PrimaryText,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(PrimaryAccent),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty() && !isSearchFocused) {
                                        Text(
                                            text = "Search entries...",
                                            color = MutedText,
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // ENTRIES LIST
                items(state.filteredExpenses, key = { it.id }) { expense ->
                    EntryItem(
                        modifier = Modifier.animateItem(),
                        expense = expense,
                        members = state.members,
                        onClick = { viewModel.onEvent(AllEntriesEvent.OpenDialogForEdit(expense)) }
                    )
                }
            }
        }
    }

    if (state.isDialogOpen) {
        TransactionDialog(
            expense = state.selectedExpense,
            members = state.members,
            onDismiss = { viewModel.onEvent(AllEntriesEvent.CloseDialog) },
            onSave = { payerId, receiverId, remark, desc, amt, splitIds ->
                viewModel.onEvent(
                    AllEntriesEvent.SaveExpense(
                        expenseId = state.selectedExpense?.id ?: 0,
                        payerId = payerId,
                        receiverId = receiverId,
                        remark = remark,
                        description = desc,
                        amount = amt,
                        splitAmongIds = splitIds
                    )
                )
            },
            onDelete = {
                state.selectedExpense?.let {
                    viewModel.onEvent(AllEntriesEvent.DeleteExpense(it))
                }
            }
        )
    }
}
}

@Composable
private fun EntryItem(
    modifier: Modifier = Modifier,
    expense: ExpenseEntity,
    members: List<MemberEntity>,
    onClick: () -> Unit
) {
    val payerName = members.find { it.id == expense.payerId }?.name ?: "Unknown"
    val isReceived = expense.receiverId != null // Transfer = Received
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SurfaceColor, RoundedCornerShape(28.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left third: Payer Name
        Text(
            text = payerName,
            color = PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Middle third: Remark
        Text(
            text = expense.remark.ifBlank { "No remark" },
            color = MutedText,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Right third: Amount
        Text(
            text = String.format("%.2f", expense.amount),
            color = if (isReceived) PositiveColor else NegativeColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AddEntryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(PrimaryAccent, CircleShape)
            .clip(CircleShape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add entry",
                color = PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PrimaryText.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = PrimaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDialog(
    expense: ExpenseEntity?,
    members: List<MemberEntity>,
    onDismiss: () -> Unit,
    onSave: (Int, Int?, String, String, Double, String) -> Unit,
    onDelete: () -> Unit
) {
    val isEditMode = expense != null

    var payerId by remember { mutableStateOf(expense?.payerId ?: members.firstOrNull()?.id ?: 0) }
    var receiverId by remember { mutableStateOf(expense?.receiverId) }
    
    var remark by remember { mutableStateOf(expense?.remark ?: "") }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var amount by remember { mutableStateOf(expense?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    
    // Split among selection
    val initialSplitIds = expense?.splitAmongIds?.split(",")?.filter { it.isNotBlank() }?.mapNotNull { it.toIntOrNull() }?.toSet()
        ?: members.map { it.id }.toSet() // default all ticked
        
    var splitIds by remember { mutableStateOf(initialSplitIds) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(SurfaceColor, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEditMode) "Edit Transaction" else "New Transaction",
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Paid By Dropdown
                var payerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded }
                ) {
                    val selectedPayer = members.find { it.id == payerId }?.name ?: "Select Payer"
                    
                    OutlinedTextField(
                        value = selectedPayer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By", color = MutedText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = PrimaryAccent
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false },
                        modifier = Modifier.background(SurfaceColor)
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name, color = PrimaryText) },
                                onClick = {
                                    payerId = member.id
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Remark
                var isRemarkFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(BgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        textStyle = TextStyle(color = PrimaryText, fontSize = 16.sp, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(PrimaryAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isRemarkFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            if (remark.isEmpty() && !isRemarkFocused) {
                                Text("Remark (e.g. Dinner)", color = MutedText, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            innerTextField()
                        }
                    )
                }

                // Description
                var isDescFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(BgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = TextStyle(color = PrimaryText, fontSize = 16.sp, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(PrimaryAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isDescFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            if (description.isEmpty() && !isDescFocused) {
                                Text("Description (Optional)", color = MutedText, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            innerTextField()
                        }
                    )
                }

                // Amount
                var isAmountFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(BgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        textStyle = TextStyle(color = PrimaryText, fontSize = 16.sp, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(PrimaryAccent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isAmountFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            if (amount.isEmpty() && !isAmountFocused) {
                                Text("0.00", color = MutedText, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            innerTextField()
                        }
                    )
                }

                // Paid To (Optional)
                var receiverExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = receiverExpanded,
                    onExpandedChange = { receiverExpanded = !receiverExpanded }
                ) {
                    val selectedReceiver = if (receiverId == null) "None (Group Expense)" else members.find { it.id == receiverId }?.name ?: "None"
                    
                    OutlinedTextField(
                        value = selectedReceiver,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid To (Optional)", color = MutedText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = PrimaryAccent
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = receiverExpanded,
                        onDismissRequest = { receiverExpanded = false },
                        modifier = Modifier.background(SurfaceColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Group Expense)", color = PrimaryText) },
                            onClick = {
                                receiverId = null
                                receiverExpanded = false
                            }
                        )
                        members.filter { it.id != payerId }.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name, color = PrimaryText) },
                                onClick = {
                                    receiverId = member.id
                                    receiverExpanded = false
                                }
                            )
                        }
                    }
                }

                // Split Among
                if (receiverId == null) {
                    Text("Split Among", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .background(BgColor, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        members.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        val newSet = splitIds.toMutableSet()
                                        if (newSet.contains(member.id)) newSet.remove(member.id)
                                        else newSet.add(member.id)
                                        splitIds = newSet
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(member.name, color = PrimaryText, fontSize = 14.sp)
                                Checkbox(
                                    checked = splitIds.contains(member.id),
                                    onCheckedChange = { checked ->
                                        val newSet = splitIds.toMutableSet()
                                        if (checked) newSet.add(member.id) else newSet.remove(member.id)
                                        splitIds = newSet
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryAccent,
                                        uncheckedColor = MutedText,
                                        checkmarkColor = PrimaryText
                                    )
                                )
                            }
                        }
                    }
                }

                // Time & Date
                val timeString = expense?.timestamp?.let { 
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
                } ?: SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                
                Text(
                    text = timeString,
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditMode) {
                        Text(
                            text = "Delete",
                            color = NegativeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .bounceClick(onClick = onDelete)
                                .padding(8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row {
                        Text(
                            text = "Cancel",
                            color = PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .bounceClick(onClick = onDismiss)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Save",
                            color = PrimaryAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .bounceClick {
                                    val amt = amount.toDoubleOrNull()
                                    if (amt != null && remark.isNotBlank() && payerId != 0) {
                                        onSave(payerId, receiverId, remark, description, amt, splitIds.joinToString(","))
                                    }
                                }
                                .padding(8.dp)
                        )
}}}}}}
