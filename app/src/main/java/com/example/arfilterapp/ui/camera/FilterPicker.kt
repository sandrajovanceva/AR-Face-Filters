package com.example.arfilterapp.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arfilterapp.filters.FilterType
import com.example.arfilterapp.ui.theme.ARFilterAppTheme

@Composable
fun FilterPicker(
    filters: List<FilterType>,
    selectedFilter: FilterType,
    onSelectFilter: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedFilter) {
        val index = filters.indexOf(selectedFilter)
        if (index >= 0) {
            listState.animateScrollToItem(index = index)
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters, key = { it.name }) { filter ->
            FilterPickerItem(
                filter = filter,
                selected = filter == selectedFilter,
                onClick = { onSelectFilter(filter) }
            )
        }
    }
}

@Composable
private fun FilterPickerItem(
    filter: FilterType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0x77000000),
        label = "filter-bg"
    )
    val fg = if (selected) Color.Black else Color.White
    val borderColor = if (selected) Color(0xFFFFD600) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = filter.emoji, fontSize = 16.sp)
        Text(
            text = filter.displayName,
            color = fg,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222222, widthDp = 360, heightDp = 80)
@Composable
private fun FilterPickerPreview() {
    ARFilterAppTheme {
        FilterPicker(
            filters = FilterType.entries,
            selectedFilter = FilterType.GLASSES,
            onSelectFilter = {}
        )
    }
}
