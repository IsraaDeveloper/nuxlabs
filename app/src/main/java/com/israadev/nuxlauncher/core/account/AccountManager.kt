package com.israadev.nuxlauncher.core.account

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.models.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

object AccountManager {
    private val gson = Gson()

    // 1. LAUNCHER AUTH SESSION (Akun NUX Launcher, Terpisah dari Akun Game Minecraft)
    private val _launcherUser = MutableStateFlow<AuthUser?>(null)
    val launcherUser: StateFlow<AuthUser?> = _launcherUser.asStateFlow()

    // 2. IN-GAME MINECRAFT ACCOUNTS (Profil Player Game di AccountsScreen)
    private val _accounts = MutableStateFlow<List<UserAccount>>(emptyList())
    val accounts: StateFlow<List<UserAccount>> = _accounts.asStateFlow()

    private val _currentAccount = MutableStateFlow<UserAccount?>(null)
    val currentAccount: StateFlow<UserAccount?> = _currentAccount.asStateFlow()

    private fun getSessionFile(context: Context): File {
        return File(context.filesDir, "launcher_session.json")
    }

    private fun getAccountsFile(context: Context): File {
        return File(context.filesDir, "accounts.json")
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        // --- A. Load Launcher Auth Session ---
        val sessionFile = getSessionFile(context)
        if (sessionFile.exists()) {
            try {
                val json = sessionFile.readText()
                val user = gson.fromJson(json, AuthUser::class.java)
                _launcherUser.value = user
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- B. Load Minecraft Game Accounts ---
        val accountsFile = getAccountsFile(context)
        if (accountsFile.exists()) {
            try {
                val json = accountsFile.readText()
                val type = object : TypeToken<List<UserAccount>>() {}.type
                val rawList: List<UserAccount> = gson.fromJson(json, type) ?: emptyList()
                val list: List<UserAccount> = rawList.map { acc ->
                    acc.copy(
                        id = if (acc.id.isNullOrBlank()) java.util.UUID.randomUUID().toString() else acc.id,
                        username = if (acc.username.isNullOrBlank()) "Player" else acc.username,
                        uuid = if (acc.uuid.isNullOrBlank()) java.util.UUID.randomUUID().toString() else acc.uuid,
                        accessToken = acc.safeAccessToken,
                        refreshToken = acc.safeRefreshToken,
                        email = acc.safeEmail,
                        tier = acc.safeTier,
                        accountType = acc.safeAccountType,
                        skinModel = acc.safeSkinModel
                    )
                }

                // Jika launcher user belum ter-load tapi ada akun launcher lawas tercampur di accounts.json, migrasikan ke session terpisah
                if (_launcherUser.value == null) {
                    val legacyAuthAcc = list.firstOrNull { it.safeEmail.isNotBlank() && it.isActivated }
                    if (legacyAuthAcc != null) {
                        val authUser = AuthUser(
                            uid = legacyAuthAcc.uuid.ifBlank { legacyAuthAcc.id },
                            email = legacyAuthAcc.safeEmail,
                            username = legacyAuthAcc.username,
                            photoURL = legacyAuthAcc.photoUrl ?: "",
                            isActivated = legacyAuthAcc.isActivated,
                            tier = legacyAuthAcc.safeTier
                        )
                        _launcherUser.value = authUser
                        saveSession(context, authUser)
                    }
                }

                // Bersihkan akun launcher dari daftar profil game Minecraft (pisahkan 100%)
                val cleanGameList = list.filterNot {
                    it.username.equals("NuxPlayer", ignoreCase = true) ||
                    (it.safeEmail.isNotBlank() && it.safeEmail == _launcherUser.value?.email) ||
                    (it.uuid.isNotBlank() && it.uuid == _launcherUser.value?.uid)
                }

                _accounts.value = cleanGameList
                _currentAccount.value = cleanGameList.firstOrNull()
                saveGameAccounts(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // LAUNCHER ACCOUNT OPERATIONS
    // ==========================================

    fun getActiveUser(): AuthUser? = _launcherUser.value

    fun isSessionActivated(): Boolean {
        val user = _launcherUser.value ?: return false
        return user.isActivated
    }

    fun saveAuthUser(context: Context, user: AuthUser) {
        _launcherUser.value = user
        saveSession(context, user)
    }

    fun updateProfile(context: Context, newUsername: String? = null, newPhotoUrl: String? = null) {
        val current = _launcherUser.value ?: return
        val updated = current.copy(
            username = if (!newUsername.isNullOrBlank()) newUsername.trim() else current.username,
            photoURL = if (newPhotoUrl != null) newPhotoUrl.trim() else current.photoURL
        )
        _launcherUser.value = updated
        saveSession(context, updated)
    }

    fun setActivation(context: Context, isActivated: Boolean, tier: String) {
        val current = _launcherUser.value ?: return
        val updated = current.copy(isActivated = isActivated, tier = tier)
        _launcherUser.value = updated
        saveSession(context, updated)
    }

    fun updateTokens(idToken: String, refreshToken: String) {
        val current = _launcherUser.value ?: return
        val updated = current.copy(
            idToken = idToken,
            refreshToken = refreshToken.ifBlank { current.refreshToken }
        )
        _launcherUser.value = updated
        appContext?.let { saveSession(it, updated) }
    }

    fun logout(context: Context) {
        _launcherUser.value = null
        try {
            val file = getSessionFile(context)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSession(context: Context, user: AuthUser) {
        try {
            val file = getSessionFile(context)
            file.writeText(gson.toJson(user))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // MINECRAFT GAME ACCOUNTS OPERATIONS
    // ==========================================

    fun addAccount(context: Context, account: UserAccount) {
        val list = _accounts.value.toMutableList()
        list.removeAll { it.id == account.id || (it.username.equals(account.username, ignoreCase = true) && it.safeAccountType == account.safeAccountType) }
        list.add(0, account)
        _accounts.value = list
        _currentAccount.value = account
        saveGameAccounts(context)
    }

    fun updateAccount(context: Context, account: UserAccount) {
        val list = _accounts.value.toMutableList()
        val index = list.indexOfFirst {
            it.id == account.id || (it.username.equals(account.username, ignoreCase = true) && it.safeAccountType == account.safeAccountType)
        }
        if (index != -1) {
            list[index] = account
        } else {
            list.add(0, account)
        }
        _accounts.value = list
        val isCurrentMatch = _currentAccount.value?.id == account.id ||
            (_currentAccount.value?.username?.equals(account.username, ignoreCase = true) == true &&
             _currentAccount.value?.safeAccountType == account.safeAccountType)
        if (isCurrentMatch || _currentAccount.value == null) {
            _currentAccount.value = account
        }
        saveGameAccounts(context)
    }

    fun selectAccount(account: UserAccount) {
        _currentAccount.value = account
    }

    fun deleteAccount(context: Context, account: UserAccount) {
        val list = _accounts.value.toMutableList()
        list.removeAll { it.id == account.id || it.username.equals(account.username, ignoreCase = true) }
        _accounts.value = list
        if (_currentAccount.value?.id == account.id || _currentAccount.value?.username.equals(account.username, ignoreCase = true)) {
            _currentAccount.value = list.firstOrNull()
        }
        saveGameAccounts(context)
    }

    fun removeAccount(context: Context, account: UserAccount) {
        deleteAccount(context, account)
    }

    private fun saveGameAccounts(context: Context) {
        try {
            val file = getAccountsFile(context)
            file.writeText(gson.toJson(_accounts.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
