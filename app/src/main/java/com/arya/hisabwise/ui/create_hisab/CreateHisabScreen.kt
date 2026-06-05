package com.arya.hisabwise.ui.create_hisab

import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.arya.hisabwise.ui.theme.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun CreateHisabScreen(
    viewModel: CreateHisabViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHisabDetail: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onNavigateToHisabDetail()
        }
    }

    val nameFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.memberVerificationError) {
        state.memberVerificationError?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(CreateHisabEvent.ClearVerificationError)
        }
    }

    GlowingBackground {
        Scaffold(
            containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SaveButton(
                    isEnabled = state.hisabName.isNotBlank(),
                    onClick = { viewModel.onEvent(CreateHisabEvent.SaveHisab) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. HEADER & HISAB NAME
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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
                        
                        Text(
                            text = if (state.editHisabId != null) "Edit Hisab" else "Hisab for",
                            color = PrimaryText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var isHisabNameFocused by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(SurfaceColor, CircleShape)
                            .clip(CircleShape)
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = state.hisabName,
                            onValueChange = { viewModel.onEvent(CreateHisabEvent.HisabNameChanged(it)) },
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(PrimaryAccent),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isHisabNameFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                if (state.hisabName.isEmpty() && !isHisabNameFocused) {
                                    Text(
                                        text = "e.g. Goa Trip",
                                        color = MutedText,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // 2. MEMBERS SECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Members",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (state.members.isEmpty()) {
                        Text(
                            text = "No members added",
                            color = MutedText,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.members.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(SurfaceColor)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = member.name,
                                            color = PrimaryText,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(SurfaceVariantColor)
                                            .padding(start = 16.dp, end = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = member.phoneNumber,
                                                color = MutedText,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                            IconButton(
                                                onClick = { viewModel.onEvent(CreateHisabEvent.RemoveMember(member)) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete member",
                                                    tint = NegativeColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ADD MEMBER BOX
            item {
                var isMemberNameFocused by remember { mutableStateOf(false) }
                var isMemberPhoneFocused by remember { mutableStateOf(false) }

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
                    ) {
                        BasicTextField(
                            value = state.newMemberName,
                            onValueChange = { viewModel.onEvent(CreateHisabEvent.NewMemberNameChanged(it)) },
                            textStyle = TextStyle(
                                color = PrimaryText, 
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(PrimaryAccent),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp)
                                .focusRequester(nameFocusRequester)
                                .onFocusChanged { isMemberNameFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.newMemberName.isEmpty() && !isMemberNameFocused) {
                                        Text(
                                            text = "Name",
                                            color = MutedText,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderColor)
                        )
                        BasicTextField(
                            value = state.newMemberPhone,
                            onValueChange = { viewModel.onEvent(CreateHisabEvent.NewMemberPhoneChanged(it)) },
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(PrimaryAccent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp)
                                .onFocusChanged { isMemberPhoneFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.newMemberPhone.isEmpty() && !isMemberPhoneFocused) {
                                        Text(
                                            text = "Number",
                                            color = MutedText,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = BorderColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(enabled = !state.isVerifyingMember) {
                                viewModel.onEvent(CreateHisabEvent.AddMember)
                                nameFocusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isVerifyingMember) {
                            CircularProgressIndicator(
                                color = PrimaryAccent,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "+ Add Member",
                                color = PrimaryAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 4. BUDGET BOX
            item {
                var isBudgetFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor, RoundedCornerShape(28.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(28.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Per person budget",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    HorizontalDivider(color = BorderColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = state.budgetPerPerson,
                            onValueChange = { viewModel.onEvent(CreateHisabEvent.BudgetChanged(it)) },
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(PrimaryAccent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isBudgetFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.budgetPerPerson.isEmpty() && !isBudgetFocused) {
                                        Text(
                                            text = "0.00",
                                            color = MutedText,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// 5. SAVE BUTTON
}

@Composable
private fun SaveButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isEnabled) PrimaryAccent else BorderColor
    val textColor = if (isEnabled) PrimaryText else MutedText
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor, CircleShape)
            .clip(CircleShape)
            .bounceClick(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Save",
                color = textColor,
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
                    contentDescription = "Save",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
}}}}
