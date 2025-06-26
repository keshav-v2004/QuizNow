import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.quiznow.authentication.AuthResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")



// UserManager class to handle user authentication

class UserManager(private val context: Context) {
    private val auth = Firebase.auth
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    private val IS_LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")

    // Google Sign-In client
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("785650806325-uktisnilm3gsuha0gsognfi5pkmt8gbj.apps.googleusercontent.com") // Replace with your web client ID from Firebase console
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Check if user is currently authenticated
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Get current user info
    fun getCurrentUser() = auth.currentUser

    // Sign up with email and password
    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                saveUserInfo(user.uid, email, true)
                AuthResult.Success(user.uid)
            } else {
                AuthResult.Error("Failed to create account")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    // Sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                saveUserInfo(user.uid, email, true)
                AuthResult.Success(user.uid)
            } else {
                AuthResult.Error("Sign in failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    // Sign in with Google
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                saveUserInfo(user.uid, user.email ?: "", true)
                AuthResult.Success(user.uid)
            } else {
                AuthResult.Error("Google sign in failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign in failed")
        }
    }

    // Get Google Sign-In Intent
    fun getGoogleSignInIntent() = googleSignInClient.signInIntent

    // Handle Google Sign-In result
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            signInWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            AuthResult.Error("Google sign in failed: ${e.message}")
        }
    }

    // Reset password
    suspend fun resetPassword(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success("Password reset email sent")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send reset email")
        }
    }

    suspend fun getCurrentUserId(): String? {
        // Only return user ID if user is authenticated with Firebase
        return auth.currentUser?.let { user ->
            saveUserInfo(user.uid, user.email ?: "", true)
            user.uid
        }
    }

    private suspend fun saveUserInfo(userId: String, email: String, isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[IS_LOGGED_IN_KEY] = isLoggedIn.toString()
        }
    }

    private suspend fun getUserIdFromLocal(): String {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY] ?: ""
        }.first()
    }

    suspend fun signOut(): AuthResult {
        return try {
            auth.signOut()
            googleSignInClient.signOut().await()
            context.dataStore.edit { it.clear() }
            AuthResult.Success("Signed out successfully")
        } catch (e: Exception) {
            AuthResult.Error("Sign out failed")
        }
    }
}