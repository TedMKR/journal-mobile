package com.journal.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
    backgroundColor: Color = AppTheme.colors.surface,
    borderColor: Color = AppTheme.colors.outline,
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
    backgroundColor: Color = AppTheme.colors.surfaceVariant,
    contentColor: Color = AppTheme.colors.primary,
    labelColor: Color = AppTheme.colors.mutedText
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
    backgroundColor: Color = AppTheme.colors.headerBackground,
    contentColor: Color = AppTheme.colors.primary,
    barColor: Color = AppTheme.colors.barBackground
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
    color: Color = AppTheme.colors.primary,
    backgroundColor: Color = AppTheme.colors.headerBackground,
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
    containerColor: Color = AppTheme.colors.primary,
    cornerRadius: Int = 12,
    textColor: Color = AppTheme.colors.onPrimary,
    bold: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = textColor)
    ) {
        Text(
            text = text,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.primary,
    cornerRadius: Int = 12
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val borderColor = AppTheme.colors.outline
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.danger
) {
    val shape = RoundedCornerShape(12.dp)
    val dangerContainer = AppTheme.colors.dangerContainer
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        border = BorderStroke(1.dp, containerColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = dangerContainer,
            contentColor = containerColor
        )
    ) {
        Text(
            text = text,
            color = containerColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppTextActionButton(
    text: String,
    onClick: () -> Unit,
    color: Color = AppTheme.colors.primary
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppMenuDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AppTheme.colors.primary,
    secondaryColor: Color = AppTheme.colors.mutedText,
    borderColor: Color = AppTheme.colors.fieldBorder
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.second == selected }?.first ?: label
    var fieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val surfaceColor = AppTheme.colors.surface

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }
                .background(surfaceColor, RoundedCornerShape(12.dp))
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
                .background(surfaceColor)
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
    backgroundColor: Color = AppTheme.colors.dangerContainer,
    contentColor: Color = AppTheme.colors.danger
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
    backgroundColor: Color = AppTheme.colors.surfaceVariant,
    contentColor: Color = AppTheme.colors.primary,
    textColor: Color = AppTheme.colors.mutedText,
    cornerRadius: Int = 14,
    indicatorSize: Int = 20
) {
    val borderColor = AppTheme.colors.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = contentColor, modifier = Modifier.size(indicatorSize.dp))
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppStateCard(
    text: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colors.surfaceVariant,
    contentColor: Color = AppTheme.colors.primary,
    errorColor: Color = AppTheme.colors.danger
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
    backgroundColor: Color = AppTheme.colors.surfaceVariant,
    contentColor: Color = AppTheme.colors.primary,
    errorColor: Color = AppTheme.colors.danger
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
    selectedColor: Color = AppTheme.colors.primary,
    unselectedColor: Color = AppTheme.colors.headerBackground,
    selectedTextColor: Color = AppTheme.colors.onPrimary,
    unselectedTextColor: Color = AppTheme.colors.primary
) {
    Box(
        modifier = modifier
            .background(if (selected) selectedColor else unselectedColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) selectedTextColor else unselectedTextColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun AppFormField(
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = AppTheme.colors.primary,
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
    selectedColor: Color = AppTheme.colors.primary,
    unselectedColor: Color = AppTheme.colors.headerBackground,
    selectedTextColor: Color = AppTheme.colors.onPrimary,
    unselectedTextColor: Color = AppTheme.colors.primary
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
    contentColor: Color = AppTheme.colors.primary,
    secondaryColor: Color = AppTheme.colors.mutedText
) {
    val surfaceColor = AppTheme.colors.surface
    val buttonColor = AppTheme.colors.headerBackground
    val canGoPrev = page > 0
    val canGoNext = page + 1 < totalPages

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppPaginationArrowButton(
            text = "‹",
            enabled = canGoPrev,
            onClick = onPrev,
            backgroundColor = buttonColor,
            contentColor = contentColor,
            disabledColor = secondaryColor
        )
        Text(
            "${page + 1} / $totalPages",
            color = contentColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        AppPaginationArrowButton(
            text = "›",
            enabled = canGoNext,
            onClick = onNext,
            backgroundColor = buttonColor,
            contentColor = contentColor,
            disabledColor = secondaryColor
        )
    }
}

@Composable
private fun AppPaginationArrowButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    disabledColor: Color
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else disabledColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
