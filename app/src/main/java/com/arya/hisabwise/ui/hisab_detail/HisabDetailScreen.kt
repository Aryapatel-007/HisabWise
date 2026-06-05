package com.arya.hisabwise.ui.hisab_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.arya.hisabwise.ui.theme.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun HisabDetailScreen(
    onNavigateToAddEntry: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEditHisab: (Int) -> Unit,
    viewModel: HisabDetailViewModel = hiltViewModel()
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
                    AddEntryButton(
                        totalEntries = state.totalEntries,
                        onClick = { onNavigateToAddEntry(state.hisab!!.id) }
                    )
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
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }
                
                IconButton(
                    onClick = { state.hisab?.id?.let { onNavigateToEditHisab(it) } },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Edit Hisab",
                        tint = PrimaryText
                    )
                }

                Text(
                    text = "Hisab for ${state.hisab?.name ?: "..."}",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // MAIN SCROLLABLE CONTENT
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // TOTAL SPENT / BUDGET BOX
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceColor, RoundedCornerShape(28.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL SPENT", color = MutedText, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.2f", state.totalSpent),
                                color = NegativeColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = BorderColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL LEFT", color = MutedText, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            val totalLeftStr = state.hisab?.budgetPerPerson?.let { budget ->
                                val totalBudget = budget * state.members.size
                                val totalLeft = totalBudget - state.totalSpent
                                String.format("%.2f", totalLeft)
                            } ?: "N/A"
                            
                            val totalLeftVal = totalLeftStr.toDoubleOrNull() ?: 0.0
                            val leftColor = when {
                                totalLeftVal > 0 -> PositiveColor
                                totalLeftVal < 0 -> NegativeColor
                                else -> PrimaryText
                            }
                            
                            Text(
                                text = totalLeftStr,
                                color = leftColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // INDIVIDUAL SPENDING BOX
                if (state.memberBalances.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(28.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("NAME", color = MutedText, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                                Text("TOTAL SPENT", color = MutedText, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = BorderColor)
                            
                            state.memberBalances.forEachIndexed { index, mb ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mb.name,
                                        color = PrimaryText,
                                        fontSize = 15.sp
                                    )
                                    
                                    val color = when {
                                        mb.netBalance > 0 -> NegativeColor
                                        mb.netBalance < 0 -> PositiveColor
                                        else -> MutedText
                                    }
                                    val prefix = when {
                                        mb.netBalance > 0 -> "-"
                                        mb.netBalance < 0 -> "+"
                                        else -> ""
                                    }
                                    
                                    Text(
                                        text = "$prefix${String.format("%.2f", kotlin.math.abs(mb.netBalance))}",
                                        color = color,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (index < state.memberBalances.size - 1) {
                                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 20.dp))
                                }
                            }
                        }
                    }
                }

                // FINAL HISAB BOX
                if (state.finalSettlements.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor, RoundedCornerShape(28.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "FINAL HISAB",
                                    color = PrimaryAccent,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(color = BorderColor)

                            state.finalSettlements.forEachIndexed { index, settlement ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = settlement.debtorName,
                                        color = PrimaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Box(
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                color = MutedText,
                                                thickness = 1.dp
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Pays",
                                                tint = MutedText,
                                                modifier = Modifier.size(16.dp).offset(x = (-2).dp)
                                            )
                                        }
                                        Text(
                                            text = kotlin.math.round(settlement.amount).toInt().toString(),
                                            color = PositiveColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(SurfaceColor)
                                                .padding(horizontal = 8.dp)
                                        )
                                    }

                                    Text(
                                        text = settlement.creditorName,
                                        color = PrimaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End
                                    )
                                    
                                    Checkbox(
                                        checked = settlement.isCompleted,
                                        onCheckedChange = { viewModel.toggleSettlement(settlement) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = PrimaryAccent,
                                            uncheckedColor = MutedText,
                                            checkmarkColor = PrimaryText
                                        ),
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                                if (index < state.finalSettlements.size - 1) {
                                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AddEntryButton(totalEntries: Int, onClick: () -> Unit) {
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
                text = "Show all $totalEntries entries",
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Add Entry",
                    tint = PrimaryText,
                    modifier = Modifier.size(18.dp)
                )
}}}}
