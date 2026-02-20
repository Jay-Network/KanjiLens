package com.jworks.kanjilens.data.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val handle: String? = null
)

sealed class AuthState {
    data object Loading : AuthState()
    data object SignedOut : AuthState()
    data class SignedIn(val user: AuthUser, val isAnonymous: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isAdminOrDeveloper = MutableStateFlow(false)
    val isAdminOrDeveloper: StateFlow<Boolean> = _isAdminOrDeveloper.asStateFlow()

    private var googleSignInClient: GoogleSignInClient? = null
    private var sessionPrefs: SharedPreferences? = null

    companion object {
        private const val PREFS_NAME = "kanjilens_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_IS_ANONYMOUS = "is_anonymous"
        private const val KEY_HANDLE = "handle"
        private const val KEY_TOTAL_SCANS = "total_scan_count"
        private const val KEY_HANDLE_PROMPT_DISMISSED = "handle_prompt_dismissed"
        const val HANDLE_PROMPT_SCAN_THRESHOLD = 3
    }

    fun initPrefs(context: Context) {
        if (sessionPrefs == null) {
            sessionPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun initGoogleSignIn(context: Context) {
        initPrefs(context)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    private fun saveSession(user: AuthUser, isAnonymous: Boolean = false) {
        sessionPrefs?.edit()?.apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_AVATAR_URL, user.avatarUrl)
            putBoolean(KEY_IS_ANONYMOUS, isAnonymous)
            if (user.handle != null) putString(KEY_HANDLE, user.handle)
            apply()
        }
    }

    private data class SavedSession(val user: AuthUser, val isAnonymous: Boolean)

    private fun loadSavedSession(): SavedSession? {
        val prefs = sessionPrefs ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return SavedSession(
            user = AuthUser(
                id = userId,
                email = prefs.getString(KEY_EMAIL, null),
                displayName = prefs.getString(KEY_DISPLAY_NAME, null),
                avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
                handle = prefs.getString(KEY_HANDLE, null)
            ),
            isAnonymous = prefs.getBoolean(KEY_IS_ANONYMOUS, false)
        )
    }

    private fun clearSavedSession() {
        sessionPrefs?.edit()?.clear()?.apply()
    }

    fun getSignInIntent(): Intent? {
        return googleSignInClient?.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                supabaseClient.auth.signInWith(IDToken) {
                    provider = Google
                    this.idToken = idToken
                }
                // User is now linked — no longer anonymous
                sessionPrefs?.edit()?.putBoolean(KEY_IS_ANONYMOUS, false)?.apply()
                refreshAuthState()
                val state = _authState.value
                if (state is AuthState.SignedIn) {
                    saveSession(state.user, isAnonymous = false)
                }
            } else {
                _authState.value = AuthState.Error("No ID token received from Google")
            }
        } catch (e: ApiException) {
            _authState.value = AuthState.Error("Google sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Authentication failed: ${e.message}")
        }
    }

    suspend fun refreshAuthState() {
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val user = session.user
                if (user != null) {
                    val isAnon = sessionPrefs?.getBoolean(KEY_IS_ANONYMOUS, false) ?: false
                    // Privacy: only store email, never Google display name or avatar
                    val savedHandle = sessionPrefs?.getString(KEY_HANDLE, null)
                    val authUser = AuthUser(
                        id = user.id,
                        email = user.email,
                        displayName = null,
                        avatarUrl = null,
                        handle = savedHandle
                    )
                    _authState.value = AuthState.SignedIn(authUser, isAnonymous = isAnon)
                    saveSession(authUser, isAnonymous = isAnon)
                    if (!isAnon) fetchUserRole(user.id)
                    return
                }
            }
            // Supabase session expired/missing — try saved session
            val saved = loadSavedSession()
            if (saved != null) {
                android.util.Log.d("AuthRepository", "Restored session from SharedPreferences: ${saved.user.email}")
                _authState.value = AuthState.SignedIn(saved.user, isAnonymous = saved.isAnonymous)
                if (!saved.isAnonymous) fetchUserRole(saved.user.id)
                return
            }
            _authState.value = AuthState.SignedOut
            _isAdminOrDeveloper.value = false
        } catch (e: Exception) {
            // On error, still try saved session
            val saved = loadSavedSession()
            if (saved != null) {
                _authState.value = AuthState.SignedIn(saved.user, isAnonymous = saved.isAnonymous)
                return
            }
            _authState.value = AuthState.SignedOut
            _isAdminOrDeveloper.value = false
        }
    }

    private suspend fun fetchUserRole(userId: String) {
        try {
            android.util.Log.d("AuthRepository", "fetchUserRole: querying for userId=$userId")
            val result = supabaseClient.postgrest["user_roles"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
            val data = result.data
            android.util.Log.d("AuthRepository", "fetchUserRole: response=$data")
            val items = kotlinx.serialization.json.Json.parseToJsonElement(data)
            if (items is kotlinx.serialization.json.JsonArray && items.isNotEmpty()) {
                val role = items[0].jsonObject["role"]?.jsonPrimitive?.content
                android.util.Log.d("AuthRepository", "fetchUserRole: role=$role")
                _isAdminOrDeveloper.value = role == "admin" || role == "developer"
            } else {
                android.util.Log.d("AuthRepository", "fetchUserRole: no rows found")
                _isAdminOrDeveloper.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "fetchUserRole: error", e)
            _isAdminOrDeveloper.value = false
        }
    }

    /** Create a local anonymous session (no network needed) */
    fun createAnonymousSession() {
        val existingId = sessionPrefs?.getString(KEY_USER_ID, null)
        val isExistingAnon = sessionPrefs?.getBoolean(KEY_IS_ANONYMOUS, false) ?: false
        val existingHandle = sessionPrefs?.getString(KEY_HANDLE, null)
        // Reuse existing anonymous UUID if available, otherwise generate new
        val userId = if (existingId != null && isExistingAnon) existingId
                     else java.util.UUID.randomUUID().toString()
        val authUser = AuthUser(
            id = userId,
            email = null,
            displayName = null,
            avatarUrl = null,
            handle = existingHandle
        )
        _authState.value = AuthState.SignedIn(authUser, isAnonymous = true)
        saveSession(authUser, isAnonymous = true)
    }

    /** Ensure a session exists. Creates anonymous session if none found. */
    suspend fun ensureSession(context: Context) {
        initPrefs(context)
        refreshAuthState()
        if (_authState.value is AuthState.SignedOut || _authState.value is AuthState.Error) {
            createAnonymousSession()
        }
    }

    suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
            googleSignInClient?.signOut()
        } catch (_: Exception) { }
        clearSavedSession()
        _isAdminOrDeveloper.value = false
        // Create a fresh anonymous session so user stays on camera
        createAnonymousSession()
    }

    fun getCurrentUserId(): String? {
        val state = _authState.value
        return if (state is AuthState.SignedIn) state.user.id else null
    }

    fun getAccessToken(): String? {
        return try {
            supabaseClient.auth.currentAccessTokenOrNull()
        } catch (_: Exception) {
            null
        }
    }

    // --- Handle system ---

    fun setHandle(handle: String) {
        sessionPrefs?.edit()?.putString(KEY_HANDLE, handle)?.apply()
        // Update current auth state with new handle
        val state = _authState.value
        if (state is AuthState.SignedIn) {
            val updated = state.user.copy(handle = handle)
            _authState.value = AuthState.SignedIn(updated, isAnonymous = state.isAnonymous)
        }
    }

    fun getHandle(): String? = sessionPrefs?.getString(KEY_HANDLE, null)

    // --- Scan counter for handle prompt ---

    fun incrementTotalScans() {
        val prefs = sessionPrefs ?: return
        val current = prefs.getInt(KEY_TOTAL_SCANS, 0)
        prefs.edit().putInt(KEY_TOTAL_SCANS, current + 1).apply()
    }

    fun getTotalScans(): Int = sessionPrefs?.getInt(KEY_TOTAL_SCANS, 0) ?: 0

    fun shouldShowHandlePrompt(): Boolean {
        val hasHandle = getHandle() != null
        val dismissed = sessionPrefs?.getBoolean(KEY_HANDLE_PROMPT_DISMISSED, false) ?: false
        val totalScans = getTotalScans()
        return !hasHandle && !dismissed && totalScans >= HANDLE_PROMPT_SCAN_THRESHOLD
    }

    fun dismissHandlePrompt() {
        sessionPrefs?.edit()?.putBoolean(KEY_HANDLE_PROMPT_DISMISSED, true)?.apply()
    }
}
