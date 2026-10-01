package com.israadev.nuxlauncher.core.renderer.v2

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Data model untuk Renderer Plugin V2 (kompatibel dengan format fclPlugin_V2 Zalith)
 */
data class RendererV2Config(
    val displayName: String,
    val rendererId: String,
    val rendererGLPath: String,
    val rendererEGLPath: String,
    val dlopenLibPaths: List<String> = emptyList(),
    val env: List<EnvConfig> = emptyList(),
    val minMCVer: String? = null,
    val maxMCVer: String? = null
)

sealed interface EnvConfig {
    val key: String

    data class NormalEnv(
        override val key: String,
        val value: String
    ) : EnvConfig

    data class SelectableEnv(
        override val key: String,
        val title: String? = null,
        val check: Boolean? = true,
        val defaultValue: String,
        val values: List<String>
    ) : EnvConfig

    data class CustomizableEnv(
        override val key: String,
        val title: String? = null,
        val defaultValue: String? = null
    ) : EnvConfig

    data class ToggleableEnv(
        override val key: String,
        val value: String,
        val title: String? = null,
        val toggle: Boolean = false
    ) : EnvConfig
}

/**
 * Unit pengaturan UI untuk masing-masing tipe environment variable
 */
sealed class EnvSettingUnit(
    val key: String,
    val summary: String?,
    protected val prefs: SharedPreferences,
    protected val storageKey: String,
    val defaultValue: String
) {
    var state by mutableStateOf(defaultValue)

    open fun init() {
        state = prefs.getString(storageKey, defaultValue) ?: defaultValue
    }

    open fun save(value: String) {
        state = value
        prefs.edit().putString(storageKey, value).apply()
    }

    open fun reset() {
        save(defaultValue)
    }

    class Selectable(
        key: String,
        val rawEnv: EnvConfig.SelectableEnv,
        val values: List<String>,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, rawEnv.defaultValue) {
        private val checkKey = "${storageKey}:check"
        var isEnabled by mutableStateOf(rawEnv.check != false)

        override fun init() {
            super.init()
            val pluginDefault = rawEnv.check != false
            isEnabled = if (rawEnv.check == null) {
                true
            } else if (prefs.contains(checkKey)) {
                prefs.getBoolean(checkKey, pluginDefault)
            } else {
                pluginDefault
            }

            if (state !in values) {
                save(rawEnv.defaultValue)
            }
        }

        fun saveCheck(enabled: Boolean) {
            isEnabled = enabled
            prefs.edit().putBoolean(checkKey, enabled).apply()
        }

        override fun reset() {
            super.reset()
            saveCheck(rawEnv.check != false)
        }
    }

    class Customizable(
        key: String,
        val rawEnv: EnvConfig.CustomizableEnv,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, rawEnv.defaultValue ?: "")

    class Toggleable(
        key: String,
        val rawEnv: EnvConfig.ToggleableEnv,
        val envValue: String,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, if (rawEnv.toggle) envValue else "") {
        val isEnabled: Boolean get() = state.isNotEmpty()

        fun setToggle(enabled: Boolean) {
            save(if (enabled) envValue else "")
        }

        override fun reset() {
            setToggle(rawEnv.toggle)
        }
    }
}

/**
 * Data lengkap representasi Renderer V2
 */
class RendererV2Data(
    val packageName: String,
    val nativePath: String,
    val summary: String,
    val config: RendererV2Config,
    context: Context
) {
    private val prefs = context.getSharedPreferences("NuxRendererV2EnvConfig", Context.MODE_PRIVATE)
    val units: List<EnvSettingUnit>

    init {
        val list = mutableListOf<EnvSettingUnit>()
        val prefix = "$packageName:"

        for (env in config.env) {
            val storageKey = "$prefix${env.key}"
            when (env) {
                is EnvConfig.NormalEnv -> {}
                is EnvConfig.SelectableEnv -> {
                    val allValues = buildList {
                        add(env.defaultValue)
                        for (v in env.values) {
                            if (v != env.defaultValue) add(v)
                        }
                    }
                    val unit = EnvSettingUnit.Selectable(
                        key = env.key,
                        rawEnv = env,
                        values = allValues,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
                is EnvConfig.CustomizableEnv -> {
                    val unit = EnvSettingUnit.Customizable(
                        key = env.key,
                        rawEnv = env,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
                is EnvConfig.ToggleableEnv -> {
                    val unit = EnvSettingUnit.Toggleable(
                        key = env.key,
                        rawEnv = env,
                        envValue = env.value,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
            }
        }
        units = list
    }

    /**
     * Menghasilkan map environment variables yang aktif untuk diinjeksi ke proses game
     */
    fun getEffectiveEnv(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        for (env in config.env) {
            when (env) {
                is EnvConfig.NormalEnv -> {
                    result[env.key] = env.value
                }
                is EnvConfig.SelectableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Selectable
                    if (u != null && u.isEnabled && u.state.isNotBlank()) {
                        result[env.key] = u.state
                    }
                }
                is EnvConfig.CustomizableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Customizable
                    if (u != null && u.state.isNotBlank()) {
                        result[env.key] = u.state
                    }
                }
                is EnvConfig.ToggleableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Toggleable
                    if (u != null && u.isEnabled) {
                        result[env.key] = u.envValue
                    }
                }
            }
        }
        return result
    }

    fun resetAll() {
        units.forEach { it.reset() }
    }
}
