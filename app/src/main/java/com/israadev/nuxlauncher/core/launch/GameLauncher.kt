package com.israadev.nuxlauncher.core.launch

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.israadev.nuxlauncher.GameActivity
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.models.VersionDetail
import java.io.File

object GameLauncher {
    private val gson = Gson()

    fun launch(context: Context, instance: Instance, account: UserAccount) {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val versionDir = File(InstanceManager.getVersionsDir(context), instance.mcVersion)
        val versionJson = File(versionDir, "${instance.mcVersion}.json")

        var mainClass = "net.minecraft.client.main.Main"
        var assetIndexId = instance.mcVersion
        var mojangJavaMajor: Int? = null
        var versionDetail: VersionDetail? = null

        if (versionJson.exists()) {
            try {
                val detail = gson.fromJson(versionJson.readText(), VersionDetail::class.java)
                versionDetail = detail
                detail.mainClass?.let { mainClass = it }
                detail.assetIndex?.id?.let { assetIndexId = it }
                mojangJavaMajor = detail.javaVersion?.majorVersion
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val isFabric = instance.loader.equals("fabric", ignoreCase = true)
        var fabricProfile: com.israadev.nuxlauncher.core.models.FabricProfile? = null

        if (isFabric) {
            val fabricJsonFile = File(versionDir, "fabric-${instance.loaderVersion}.json")
            if (fabricJsonFile.exists()) {
                try {
                    fabricProfile = gson.fromJson(fabricJsonFile.readText(), com.israadev.nuxlauncher.core.models.FabricProfile::class.java)
                } catch (_: Exception) {}
            }
            if (fabricProfile == null) {
                val files = versionDir.listFiles { f -> f.name.startsWith("fabric-") && f.name.endsWith(".json") }
                if (!files.isNullOrEmpty()) {
                    try {
                        fabricProfile = gson.fromJson(files.first().readText(), com.israadev.nuxlauncher.core.models.FabricProfile::class.java)
                    } catch (_: Exception) {}
                }
            }

            if (fabricProfile == null) {
                android.widget.Toast.makeText(
                    context,
                    "Profil Fabric (${instance.loaderVersion}) belum terpasang. Silakan tekan tombol UNDUH pada instance!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

            fabricProfile.mainClass?.let { mainClass = it } ?: run {
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient"
            }
        }

        val runtimeName = if (instance.javaRuntime != "auto") instance.javaRuntime else {
            com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.getRecommendedRuntime(instance.mcVersion, mojangJavaMajor)
        }
        val runtimeHome = com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.getRuntimeHome(context, runtimeName)

        // Prepare Android-native LWJGL components
        val versionJsonFile = File(versionDir, "${instance.mcVersion}.json")
        val lwjglVersion = LwjglManager.getLwjglVersion(instance.mcVersion, versionJsonFile)
        val lwjglNativesDir = LwjglManager.getNativesDir(context, lwjglVersion)
        val lwjglJars = LwjglManager.getLwjglJars(context, lwjglVersion)
        LwjglManager.prepareLauncherComponents(context)

        // Build classpath: Android LWJGL jars first, Fabric libraries, Mojang non-LWJGL libraries, then client.jar
        val libDir = InstanceManager.getLibrariesDir(context)
        val classpathEntries = mutableListOf<String>()

        // 1. Android LWJGL Jars
        lwjglJars.forEach { jar ->
            classpathEntries.add(jar.absolutePath)
        }

        // Add MioLaunchWrapper.jar if available
        val wrapperJar = LwjglManager.prepareLaunchWrapper(context)
        val useWrapper = wrapperJar != null && wrapperJar.exists()
        if (useWrapper) {
            classpathEntries.add(wrapperJar.absolutePath)
        }

        // 2. Fabric Libraries
        val loaderArtifactKeys = mutableSetOf<String>()
        if (isFabric && fabricProfile != null) {
            fabricProfile.libraries.forEach { lib ->
                if (lib.name.contains("org.lwjgl")) return@forEach
                val parts = lib.name.split(":")
                if (parts.size >= 2) {
                    loaderArtifactKeys.add("${parts[0]}:${parts[1]}")
                }
                val relPath = com.israadev.nuxlauncher.core.fabric.FabricService.artifactToPath(lib.name)
                if (relPath != null) {
                    val file = File(libDir, relPath)
                    if (file.exists()) {
                        classpathEntries.add(file.absolutePath)
                    }
                }
            }
        }

        // 3. Mojang libraries (excluding desktop org.lwjgl and libraries already provided/overridden by mod loader)
        versionDetail?.libraries?.forEach { lib ->
            val name = lib.name ?: ""
            if (name.contains("org.lwjgl")) return@forEach

            val parts = name.split(":")
            if (parts.size >= 2) {
                val groupArtifact = "${parts[0]}:${parts[1]}"
                if (loaderArtifactKeys.contains(groupArtifact)) return@forEach
            }

            // Exclude vanilla ASM when running under Fabric (Fabric strictly provides & requires its own ASM)
            if (isFabric && (name.startsWith("org.ow2.asm:") || lib.downloads?.artifact?.path?.contains("org/ow2/asm") == true)) {
                return@forEach
            }

            val path = lib.downloads?.artifact?.path
            if (path != null) {
                val file = File(libDir, path)
                if (file.exists()) {
                    classpathEntries.add(file.absolutePath)
                }
            }
        }

        // 4. Minecraft Client JAR
        val clientJar = File(versionDir, "${instance.mcVersion}.jar")
        if (clientJar.exists()) {
            classpathEntries.add(clientJar.absolutePath)
        }

        val classpath = classpathEntries.distinct().joinToString(File.pathSeparator)
        val assetsDir = InstanceManager.getAssetsDir(context)

        val userType = if (account.safeAccountType == "microsoft") "msa" else "mojang"
        val accessToken = if (account.safeAccessToken.isNotBlank()) account.safeAccessToken else "0"

        val isElyBy = account.safeAccountType == "elyby"
        val isOffline = account.safeAccountType == "offline"

        var authlibJar: File? = null
        var authlibUrl: String? = null
        var playerUuid = account.uuid

        if (isElyBy) {
            authlibJar = LwjglManager.prepareAuthLibInjector(context)
            authlibUrl = account.authServerUrl ?: "https://authserver.ely.by/api/authlib-injector"

            // Pastikan UUID adalah 32-karakter valid yang dikenali server Ely.by
            if (playerUuid.length != 32 || playerUuid.toLongOrNull() != null) {
                val realUuid = com.israadev.nuxlauncher.core.account.elyby.ElyByAuthService.fetchRealPlayerUuid(account.username)
                if (realUuid.isNotBlank()) {
                    playerUuid = realUuid
                    val updated = account.copy(uuid = realUuid)
                    com.israadev.nuxlauncher.core.account.AccountManager.updateAccount(context, updated)
                }
            }
        } else if (isOffline) {
            val skinFile = account.getSkinFile()
            val capeFile = account.getCapeFile()
            val hasCustomSkinOrCape = (skinFile != null && skinFile.exists()) || (capeFile != null && capeFile.exists())

            if (hasCustomSkinOrCape) {
                // Hentikan server sebelumnya jika masih aktif
                com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer.activeInstance?.stop()

                val offlineServer = com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer(0)
                offlineServer.addCharacter(
                    username = account.username,
                    uuid = playerUuid,
                    skinFile = skinFile,
                    capeFile = capeFile,
                    isSlim = account.safeSkinModel == "slim"
                )
                val port = offlineServer.start()
                if (port > 0) {
                    authlibJar = LwjglManager.prepareAuthLibInjector(context)
                    authlibUrl = "http://127.0.0.1:$port"
                    android.util.Log.i("GameLauncher", "Mengaktifkan Offline Yggdrasil server pada port $port untuk ${account.username}")
                }
            }
        }

        val installedMods = com.israadev.nuxlauncher.core.crash.CrashManager.getInstalledMods(gameDir.absolutePath)

        val intent = Intent(context, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_INSTANCE_NAME, instance.name)
            putExtra(GameActivity.EXTRA_MC_VERSION, instance.mcVersion)
            putExtra(GameActivity.EXTRA_LOADER, instance.loader)
            putExtra(GameActivity.EXTRA_LOADER_VERSION, instance.loaderVersion)
            putStringArrayListExtra(GameActivity.EXTRA_INSTALLED_MODS, ArrayList(installedMods))
            putExtra(GameActivity.EXTRA_USERNAME, account.username)
            putExtra(GameActivity.EXTRA_UUID, playerUuid)
            putExtra(GameActivity.EXTRA_ACCESS_TOKEN, accessToken)
            putExtra(GameActivity.EXTRA_USER_TYPE, userType)
            if (authlibJar != null && authlibUrl != null) {
                putExtra(GameActivity.EXTRA_AUTHLIB_INJECTOR_PATH, authlibJar.absolutePath)
                putExtra(GameActivity.EXTRA_AUTHLIB_URL, authlibUrl)
            }
            putExtra(GameActivity.EXTRA_GAME_DIR, gameDir.absolutePath)
            putExtra(GameActivity.EXTRA_ASSETS_DIR, assetsDir.absolutePath)
            putExtra(GameActivity.EXTRA_ASSET_INDEX, assetIndexId)
            putExtra(GameActivity.EXTRA_MAIN_CLASS, mainClass)
            putExtra(GameActivity.EXTRA_CLASSPATH, classpath)
            putExtra(GameActivity.EXTRA_RUNTIME_NAME, runtimeName)
            putExtra(GameActivity.EXTRA_RUNTIME_HOME, runtimeHome.absolutePath)
            putExtra(GameActivity.EXTRA_LWJGL_NATIVES_DIR, lwjglNativesDir.absolutePath)
            putExtra(GameActivity.EXTRA_USE_WRAPPER, useWrapper)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        com.israadev.nuxlauncher.core.social.NuxSocialManager.setInGame(true)
        context.startActivity(intent)
    }
}
