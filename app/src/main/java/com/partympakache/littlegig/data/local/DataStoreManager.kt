package com.partympakache.littlegig.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.partympakache.littlegig.data.model.AppPreferences

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")


//Use data store to store login state
class DataStoreManager(private val context: Context) {
    private val dataStore = context.dataStore
    private val viewedRecapsKey = stringSetPreferencesKey("viewed_recaps")


    suspend fun setRecapViewed(recapId: String, userId: String) {
        val key = "viewed_${recapId}_${userId}"
        dataStore.edit { preferences ->
            val currentSet = preferences[viewedRecapsKey]?.toMutableSet() ?: mutableSetOf() // Convert to MutableSet
            currentSet.add(key)
            preferences[viewedRecapsKey] = currentSet
        }
    }

    fun hasRecapBeenViewed(recapId: String, userId: String): Flow<Boolean> {
        val key = "viewed_${recapId}_${userId}"
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val viewedRecaps = preferences[viewedRecapsKey] ?: emptySet()
                viewedRecaps.contains(key)
            }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    //Make sure you use the same name
    companion object{
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login_state")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }
    //Get the Login State
    val getLoginStatus: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if(exception is IOException){
                emit(emptyPreferences())
            } else {
                throw exception
            }

        }
        .map { preferences ->
            val loginStatus = preferences[IS_LOGGED_IN_KEY] ?: false //Default false
            loginStatus
        }

    //Save Login State
    suspend fun saveLoginState(isLoggedIn: Boolean){
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = isLoggedIn
        }
    }
}