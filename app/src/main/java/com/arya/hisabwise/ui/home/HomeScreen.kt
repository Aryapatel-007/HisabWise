package com.arya.hisabwise.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.arya.hisabwise.data.local.HisabEntity

@Composable
fun HomeScreen(
    onNavigateToAddHisab: () -> Unit,
    onNavigateToHisabDetail: (Int) -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val hisabs by viewModel.hisabs.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userFirstName by viewModel.userFirstName.collectAsState()
    val workspaceMode by viewModel.workspaceMode.collectAsState()
    var hisabToDelete by remember { mutableStateOf<HisabEntity?>(null) }

    var showLogoutPopup by remember { mutableStateOf(false) }

    GlowingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
        // TOP STICKY HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HisabWise",
                    color = PrimaryText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                if (isLoggedIn && userFirstName.isNotBlank()) {
                    Text(
                        text = "Hello, $userFirstName!",
                        color = MutedText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // WORKSPACE TOGGLE
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceColor)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (workspaceMode == "local") PrimaryAccent else Color.Transparent)
                        .bounceClick { viewModel.onToggleWorkspace(false, onNavigateToAuth) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Local",
                        color = if (workspaceMode == "local") PrimaryText else MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (workspaceMode == "global") PrimaryAccent else Color.Transparent)
                        .bounceClick { viewModel.onToggleWorkspace(true, onNavigateToOnboarding) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Global",
                        color = if (workspaceMode == "global") PrimaryText else MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = PrimaryText,
                modifier = Modifier
                    .size(24.dp)
                    .bounceClick { showLogoutPopup = true }
            )
        }

        // ADD HISAB BOX
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(PrimaryAccent, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .bounceClick { onNavigateToAddHisab() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Hisab",
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryText.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Hisab",
                            tint = PrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // BEAUTIFUL LINE SEPARATOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .height(1.dp)
                .background(BorderColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // HISAB LIST (LazyColumn)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(hisabs, key = { it.id }) { hisab ->
                HisabItem(
                    modifier = Modifier.animateItem(),
                    hisab = hisab,
                    onClick = { onNavigateToHisabDetail(hisab.id) },
                    onRename = { newName -> viewModel.renameHisab(hisab.id, newName) },
                    onDuplicate = { viewModel.duplicateHisab(hisab) },
                    onDelete = { hisabToDelete = hisab }
                )
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    hisabToDelete?.let { hisab ->
        Dialog(
            onDismissRequest = { hisabToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceVariantColor
            ) {
                Column {
                    Text(
                        text = "Are you sure?",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick { hisabToDelete = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = PrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderColor)
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick {
                                    viewModel.deleteHisab(hisab)
                                    hisabToDelete = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                color = NegativeColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // LOGOUT CONFIRMATION DIALOG
    if (showLogoutPopup) {
        Dialog(
            onDismissRequest = { showLogoutPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceVariantColor
            ) {
                Column {
                    Text(
                        text = "Log out?",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick { showLogoutPopup = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = PrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderColor)
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick {
                                    showLogoutPopup = false
                                    viewModel.logout {
                                        onNavigateToAuth()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log out",
                                color = NegativeColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun HisabItem(
    modifier: Modifier = Modifier,
    hisab: HisabEntity,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(hisab.name) }
    var isRenameFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceVariantColor)
                .bounceClick { if (!isRenaming) onClick() }
        ) {
            // Left Half
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isRenaming) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isRenameFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(PrimaryAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isRenaming = false
                                onRename(renameText)
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (renameText.isEmpty() && !isRenameFocused) {
                                    Text(
                                        text = "Hisab Name",
                                        color = MutedText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(
                        text = hisab.name,
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }

            // Right Half
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    val amountColor = when {
                        hisab.netAmount > 0 -> PositiveColor
                        hisab.netAmount < 0 -> NegativeColor
                        else -> MutedText
                    }
                    Text(
                        text = String.format("%.2f", hisab.netAmount),
                        color = amountColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = PrimaryText
                            )
                        }

                        // OPTIONS MENU
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(
                                surface = SurfaceVariantColor,
                                onSurface = PrimaryText
                            ),
                            shapes = MaterialTheme.shapes.copy(
                                extraSmall = RoundedCornerShape(12.dp)
                            )
                        ) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(SurfaceVariantColor)
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Text("Rename", color = PrimaryText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        expanded = false
                                        isRenaming = true
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                                DropdownMenuItem(
                                    text = { 
                                        Text("Duplicate", color = PrimaryText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        expanded = false
                                        onDuplicate()
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                                DropdownMenuItem(
                                    text = { 
                                        Text("Delete", color = NegativeColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        expanded = false
                                        onDelete()
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
}}}}}}}}
