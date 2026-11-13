package com.example.wakemeup.ui.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wakemeup.model.Destination
import java.util.Locale

@Composable
fun LocationSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    searchResults: List<Destination>,
    isSearching: Boolean,
    onDestinationSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            placeholder = {
                Text(
                    text = "Search destination address or place...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchSubmit()
            }),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        )

        AnimatedVisibility(
            visible = searchResults.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minOf(searchResults.size * 68, 240).dp)
                ) {
                    items(searchResults) { destination ->
                        SearchResultItem(
                            destination = destination,
                            onClick = {
                                keyboardController?.hide()
                                onDestinationSelect(destination)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    destination: Destination,
    onClick: () -> Unit
) {
    val poiIcon = getPoiIcon(destination.category, destination.type)
    val categoryHint = getPoiCategoryHint(destination.category, destination.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = poiIcon,
                    contentDescription = categoryHint ?: "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!categoryHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val subtitleText = if (!destination.formattedDistance.isNullOrBlank()) {
                "${destination.formattedDistance} • ${destination.address}"
            } else {
                destination.address
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getPoiIcon(category: String?, type: String?): ImageVector {
    val cat = category?.lowercase(Locale.ROOT) ?: ""
    val t = type?.lowercase(Locale.ROOT) ?: ""

    return when {
        t in listOf("fuel", "charging_station", "gas_station", "petrol_pump", "petrol") || cat in listOf("fuel", "gas_station") -> Icons.Rounded.LocalGasStation
        t in listOf("hotel", "motel", "hostel", "guest_house", "lodging", "resort") || cat == "tourism" -> Icons.Rounded.Hotel
        t in listOf("restaurant", "cafe", "fast_food", "food", "bakery", "bar", "pub", "food_court") -> Icons.Rounded.Restaurant
        t in listOf("hospital", "clinic", "doctors", "pharmacy", "dentist") || cat == "health" -> Icons.Rounded.LocalHospital
        t in listOf("supermarket", "convenience", "bank", "store", "mall", "department_store", "shop", "clothes", "shoes") || cat in listOf("shop", "office", "commercial") -> Icons.Rounded.Storefront
        t in listOf("park", "museum", "monument", "attraction", "theme_park", "viewpoint", "zoo", "castle") || cat in listOf("historic", "leisure") -> Icons.Rounded.Museum
        t in listOf("bus_stop", "station", "subway", "train_station", "tram_stop", "bus_station", "transit_station") -> Icons.Rounded.DirectionsBus
        t == "airport" -> Icons.Rounded.Flight
        else -> Icons.Rounded.LocationOn
    }
}

private fun getPoiCategoryHint(category: String?, type: String?): String? {
    val cat = category?.lowercase(Locale.ROOT)
    val t = type?.lowercase(Locale.ROOT)

    return when {
        t in listOf("fuel", "charging_station", "gas_station", "petrol_pump", "petrol") || cat in listOf("fuel", "gas_station") -> "Petrol Pump"
        t in listOf("hotel", "motel", "hostel", "guest_house", "lodging", "resort") -> "Hotel"
        t in listOf("restaurant", "cafe", "fast_food", "food", "bakery", "bar", "pub", "food_court") -> "Restaurant"
        t in listOf("hospital", "clinic", "doctors", "pharmacy", "dentist") -> "Hospital"
        t == "bank" -> "Bank"
        t == "supermarket" -> "Supermarket"
        t == "bus_stop" -> "Bus Stop"
        t in listOf("station", "train_station", "subway") -> "Station"
        t == "airport" -> "Airport"
        t == "park" -> "Park"
        t == "museum" -> "Museum"
        t != null && t.isNotBlank() && t != "yes" && t != "unclassified" -> {
            t.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        cat != null && cat.isNotBlank() && cat != "yes" && cat != "unclassified" && cat != "building" && cat != "highway" -> {
            cat.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        else -> null
    }
}
