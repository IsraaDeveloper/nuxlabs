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
        if (isFabric && fabricProfile != null) {
            fabricProfile.libraries.forEach { lib ->
                if (lib.name.contains("org.lwjgl")) return@forEach
                val relPath = com.israadev.nuxlauncher.core.fabric.FabricService.artifactToPath(lib.name)
                if (relPath != null) {
                    val file = File(libDir, relPath)
                    if (file.exists()) {
                        classpathEntries.add(file.absolutePath)
                    }
                }
            }
        }

        // 3. Mojang libraries (excluding desktop org.lwjgl)
        versionDetail?.libraries?.forEach { lib ->
            val name = lib.name ?: ""
            if (name.contains("org.lwjgl")) return@forEach

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

        val classpath = classpathEntries.joinToString(File.pathSeparator)
        val assetsDir = InstanceManager.getAssetsDir(context)

        val userType = if (account.safeAccountType == "microsoft") "msa" else "mojang"
        val accessToken = if (account.safeAccessToken.isNotBlank()) account.safeAccessToken else "0"

        val isElyBy = account.safeAccountType == "elyby"

        var authlibJar: File? = null
        var authlibUrl: String? = null

        if (isElyBy) {
            authlibJar = LwjglManager.prepareAuthLibInjector(context)
            authlibUrl = account.authServerUrl ?: "https://authserver.ely.by/api/authlib-injector"
        }

        val intent = Intent(context, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_INSTANCE_NAME, instance.name)
            putExtra(GameActivity.EXTRA_MC_VERSION, instance.mcVersion)
            putExtra(GameActivity.EXTRA_USERNAME, account.username)
            putExtra(GameActivity.EXTRA_UUID, account.uuid)
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
