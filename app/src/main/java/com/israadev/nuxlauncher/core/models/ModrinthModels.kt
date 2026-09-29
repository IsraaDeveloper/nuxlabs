package com.israadev.nuxlauncher.core.models

import com.google.gson.annotations.SerializedName

/**
 * Modrinth Search API Response (/v2/search)
 */
data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchHit> = emptyList(),
    val offset: Int = 0,
    val limit: Int = 20,
    @SerializedName("total_hits") val totalHits: Int = 0
)

/**
 * Single project hit in Modrinth search
 */
data class ModrinthSearchHit(
    @SerializedName("project_id") val projectId: String = "",
    @SerializedName("project_type") val projectType: String = "mod",
    val slug: String? = null,
    val author: String? = null,
    val title: String = "",
    val description: String? = null,
    val categories: List<String>? = null,
    @SerializedName("display_categories") val displayCategories: List<String>? = null,
    val versions: List<String>? = null,
    val downloads: Long = 0,
    @SerializedName("icon_url") val iconUrl: String? = null,
    val color: Long? = null
)

/**
 * Modrinth Project Details (/v2/project/{id})
 */
data class ModrinthProject(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val body: String? = null,
    val categories: List<String>? = null,
    val loaders: List<String>? = null,
    @SerializedName("game_versions") val gameVersions: List<String>? = null,
    val downloads: Long = 0,
    @SerializedName("icon_url") val iconUrl: String? = null,
    val gallery: List<ModrinthGalleryItem>? = null
)

data class ModrinthGalleryItem(
    val url: String = "",
    val title: String? = null,
    val description: String? = null
)

/**
 * Modrinth Version (/v2/project/{id}/version or /v2/version/{id})
 */
data class ModrinthVersion(
    val id: String = "",
    @SerializedName("project_id") val projectId: String = "",
    val name: String = "",
    @SerializedName("version_number") val versionNumber: String = "",
    @SerializedName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    @SerializedName("version_type") val versionType: String = "release", // release, beta, alpha
    val files: List<ModrinthVersionFile> = emptyList(),
    val dependencies: List<ModrinthDependency>? = null
)

/**
 * Downloadable file in Modrinth Version
 */
data class ModrinthVersionFile(
    val url: String = "",
    val filename: String = "",
    val primary: Boolean = false,
    val size: Long = 0
)

/**
 * Modrinth dependency item
 */
data class ModrinthDependency(
    @SerializedName("version_id") val versionId: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("dependency_type") val dependencyType: String = "required" // required, optional, incompatible, embedded
)

/**
 * Representation of an installed item in instance storage
 */
data class InstalledModItem(
    val id: String,
    val filename: String,
    val originalFilename: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val itemType: String
)

/**
 * Manifest tracking for installed modpack files to enable selective delete and toggle
 */
data class ModpackManifest(
    @SerializedName("modpack_name") val modpackName: String,
    @SerializedName("version") val version: String = "",
    @SerializedName("archive_filename") val archiveFilename: String,
    @SerializedName("installed_files") val installedFiles: List<String> = emptyList()
)

