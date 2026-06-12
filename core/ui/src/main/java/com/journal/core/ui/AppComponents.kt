package com.journal.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = AppHeaderBackground,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius.dp))
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
fun AppStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppBackground,
    contentColor: Color = AppPrimary,
    labelColor: Color = AppMutedText
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = contentColor)
        Text(label, fontSize = 14.sp, color = labelColor)
    }
}

@Composable
fun AppStatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    contentColor: Color = AppPrimary,
    barColor: Color = contentColor
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(barColor, RoundedCornerShape(4.dp))
        )
        Text(
            title,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            color = contentColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppPrimary,
    backgroundColor: Color = AppHeaderBackground,
    horizontalPadding: Int = 10,
    verticalPadding: Int = 6
) {
    Text(
        text = text,
        color = color,
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun AppAdminBadge(
    text: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = background
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppPrimary,
    cornerRadius: Int = 12,
    textColor: Color = Color.White,
    bold: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = textColor)
    ) {
        Text(text, color = textColor, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    contentColor: Color = AppPrimary,
    cornerRadius: Int = 12
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(1.dp, AppHeaderBackground, RoundedCornerShape(cornerRadius.dp)),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppDanger
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(1.dp, containerColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppDangerLight,
            contentColor = containerColor
        )
    ) {
        Text(text, color = containerColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppTextActionButton(
    text: String,
    onClick: () -> Unit,
    color: Color = AppPrimary
) {
    TextButton(onClick = onClick) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppMenuDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AppPrimary,
    secondaryColor: Color = AppMutedText,
    borderColor: Color = AppFieldBorder
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.second == selected }?.first ?: label
    var fieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, if (expanded) contentColor else borderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayLabel,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_bottom),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(fieldWidth)
                .background(Color.White)
        ) {
            options.forEach { (optLabel, optValue) ->
                DropdownMenuItem(
                    text = { Text(optLabel, color = contentColor) },
                    onClick = {
                        onSelected(optValue)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AppSelectCard(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    AppDropdown(
        label = label,
        options = options,
        selected = selected,
        onSelected = onSelected,
        modifier = modifier
    )
}

@Composable
fun AppErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppDangerLight,
    contentColor: Color = AppDanger
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(14.dp),
        color = contentColor,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun AppLoadingCard(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppBackground,
    contentColor: Color = AppPrimary,
    textColor: Color = AppMutedText,
    cornerRadius: Int = 14,
    indicatorSize: Int = 20
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .border(1.dp, AppHeaderBackground, RoundedCornerShape(cornerRadius.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = contentColor, modifier = Modifier.size(indicatorSize.dp))
        Text(text, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppStateCard(
    text: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppBackground,
    contentColor: Color = AppPrimary,
    errorColor: Color = AppDanger
) {
    Text(
        text = text,
        color = if (isError) errorColor else contentColor,
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun AppMessageCards(
    error: String?,
    success: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppBackground,
    contentColor: Color = AppPrimary,
    errorColor: Color = AppDanger
) {
    Column(modifier = modifier) {
        error?.let {
            AppStateCard(
                text = it,
                isError = true,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                errorColor = errorColor
            )
        }
        success?.let {
            AppStateCard(
                text = it,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                errorColor = errorColor
            )
        }
    }
}

@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = AppPrimary,
    unselectedColor: Color = AppHeaderBackground,
    selectedTextColor: Color = Color.White,
    unselectedTextColor: Color = AppPrimary
) {
    Box(
        modifier = modifier
            .background(if (selected) selectedColor else unselectedColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) selectedTextColor else unselectedTextColor)
    }
}

@Composable
fun AppFormField(
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = AppPrimary,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
fun AppStatusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = AppPrimary,
    unselectedColor: Color = AppHeaderBackground,
    selectedTextColor: Color = Color.White,
    unselectedTextColor: Color = AppPrimary
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(if (selected) selectedColor else unselectedColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) selectedTextColor else unselectedTextColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppPaginationRow(
    page: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AppPrimary,
    secondaryColor: Color = AppMutedText
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AppHeaderBackground, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev, enabled = page > 0) {
            Text("Назад", color = contentColor, fontWeight = FontWeight.SemiBold)
        }
        Text("${page + 1} / $totalPages", color = secondaryColor, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onNext, enabled = page + 1 < totalPages) {
            Text("Вперед", color = contentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
