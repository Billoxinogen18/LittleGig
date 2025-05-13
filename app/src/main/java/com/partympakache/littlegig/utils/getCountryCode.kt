package com.partympakache.littlegig.utils

import android.telephony.PhoneNumberUtils
import android.util.Log
import java.util.*

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException

//Get Country Code
fun getCountryCode(countryCode: String): String {
    val locale = Locale("", countryCode)
    return locale.country
}

//Get Country Name
fun getCountryName(countryCode: String): String {
    val locale = Locale("", countryCode)
    return locale.displayCountry
}

//Get Country Flag
fun getCountryFlag(countryCode: String): String {
    val flagOffset = 0x1F1E6
    val asciiOffset = 0x41
    val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
    val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
    return (String(Character.toChars(firstChar)) + String(Character.toChars(secondChar)))
}

// Add to com.partympakache.littlegig.utils
fun formatPhoneNumber(phoneNumber: String, countryCode: String): String? {
    // Remove any non-digit characters
    var digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

    // If the phone number already starts with the country code, don't add it again
    if (!digitsOnly.startsWith(countryCode)) {
        // Check if minimum valid length for a phone number (typically 7+ digits)
        if (digitsOnly.length < 7) {
            return null // Too short to be valid
        }

        // Add the country code if not already present
        return "+$countryCode$digitsOnly"
    } else {
        // Already has country code
        return "+$digitsOnly"
    }
}
//Gets the country code
fun getCountryISOFromCode(numericCode: String): String {
    val regex = """^\d+$""".toRegex() //Regex to check if is digit
    if (!numericCode.matches(regex)) { //Check if matches
        return "" // Return empty string if invalid input
    }
    val matchingLocale = Locale.getISOCountries().firstOrNull { isoCountry -> //Find the country
        //Use a try-catch
        try {
            val locale = Locale("", isoCountry)
            val code = locale.country //Get code
            PhoneNumberUtils.formatNumberToE164("1", code)?.substring(1)?.startsWith(numericCode) == true //Check if it starts with, make sure to return bool
        } catch (e: Exception){
            false //If exception, false
        }

    }
    return matchingLocale ?: "" //Return
}


fun formatPhoneNumberWithLibrary(phoneNumber: String, countryIsoCode: String): String? {
    try {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val parsedNumber = phoneUtil.parse(phoneNumber, countryIsoCode)

        if (phoneUtil.isValidNumber(parsedNumber)) {
            return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        }
    } catch (e: NumberParseException) {
        Log.e("PhoneFormat", "Error parsing number", e)
    }
    return null
}