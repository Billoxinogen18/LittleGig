package com.partympakache.littlegig.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.partympakache.littlegig.data.model.CountryData
import com.partympakache.littlegig.utils.getCountryCode
import com.partympakache.littlegig.utils.getCountryFlag
import com.partympakache.littlegig.utils.getCountryName
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePicker(
    selectedCountry: CountryData,
    onCountrySelected: (CountryData) -> Unit,
    defaultSelectedCountry: CountryData,
    pickedCountry: (CountryData) -> Unit
) {

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
     var selectedCountryRemember by remember { mutableStateOf(defaultSelectedCountry) }

    val countryList = remember {
        Locale.getISOCountries().map { countryCode ->
            CountryData(
                countryCode = getCountryCode(countryCode),
                countryName = getCountryName(countryCode),
                countryFlag = getCountryFlag(countryCode)
            )
        }.sortedBy { it.countryName }
    }


    Column {

        // Use a Row to arrange icon and text horizontally.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .clickable { showDialog = true } // Show dialog on click
        ) {
            // Display the flag using an emoji (consider a library for better flag support)
            Text(
                text = selectedCountryRemember.countryFlag.ifEmpty {
                   //If empty, get the phone's country, and show the details.
                    val locale =  ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    locale?.let {
                        selectedCountryRemember = CountryData(
                            countryCode = getCountryCode(it.country),
                            countryName = getCountryName(it.country),
                            countryFlag = getCountryFlag(it.country)
                        )
                    }
                    selectedCountryRemember.countryFlag

                },
                modifier = Modifier.padding(end = 8.dp)
            )
            // Display Country Code
            Text(
                text = "+${selectedCountryRemember.countryCode.ifEmpty {
                    val locale =  ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    locale?.let {
                        selectedCountryRemember = CountryData(
                            countryCode = getCountryCode(it.country),
                            countryName = getCountryName(it.country),
                            countryFlag = getCountryFlag(it.country)
                        )
                    }
                    selectedCountryRemember.countryCode
                }}",
                style = MaterialTheme.typography.bodyMedium
            )

            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }

        // Country Selection Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Select Country") },
                text = {
                    LazyColumn {
                        items(countryList) { country ->
                            CountryItem(
                                country = country,
                                onCountrySelected = {
                                    selectedCountryRemember = it // Select Country on selection
                                    pickedCountry(it)
                                    onCountrySelected(it)
                                    showDialog = false // Close on selection
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun CountryItem(country: CountryData, onCountrySelected: (CountryData) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCountrySelected(country) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = country.countryFlag, modifier = Modifier.padding(end = 16.dp))
        Text(text = country.countryName, style = MaterialTheme.typography.bodyMedium)
    }
}