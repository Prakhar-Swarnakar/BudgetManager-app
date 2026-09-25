package com.budgetmanager.app.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

sealed interface BackupValidationResult {
    data class Valid(val bundle: BackupBundle) : BackupValidationResult
    data class Invalid(val reason: String) : BackupValidationResult
}

/**
 * Turns a [BackupBundle] into a file's contents and back, with strict validation on the way in -
 * a malformed file, one missing a required field, and one from a newer app version are all
 * rejected without touching any data (13-development-best-practices.md: "Backup import validates
 * the file before touching data... replaces data only after confirmation, and only if the whole
 * file is valid").
 */
object BackupSerializer {
    private const val MALFORMED_MESSAGE = "Could not read that file. Pick a backup made by this app."

    private val json = Json { prettyPrint = true }

    fun serialize(bundle: BackupBundle): String = json.encodeToString(BackupBundle.serializer(), bundle)

    /**
     * Checks the format version before attempting a full decode, so a backup from a newer app
     * version gets its own clear message instead of failing as if it were just malformed - a
     * newer version may have fields this decoder doesn't know how to read at all.
     */
    fun validate(rawJson: String): BackupValidationResult {
        val formatVersion = try {
            json.parseToJsonElement(rawJson).jsonObject["formatVersion"]?.jsonPrimitive?.intOrNull
        } catch (e: SerializationException) {
            return BackupValidationResult.Invalid(MALFORMED_MESSAGE)
        } catch (e: IllegalArgumentException) {
            return BackupValidationResult.Invalid(MALFORMED_MESSAGE)
        }

        if (formatVersion == null) return BackupValidationResult.Invalid(MALFORMED_MESSAGE)
        if (formatVersion > BackupBundle.CURRENT_FORMAT_VERSION) {
            return BackupValidationResult.Invalid(
                "This backup was made with a newer version of the app. Update the app before importing it."
            )
        }

        return try {
            BackupValidationResult.Valid(json.decodeFromString(BackupBundle.serializer(), rawJson))
        } catch (e: SerializationException) {
            BackupValidationResult.Invalid(MALFORMED_MESSAGE)
        } catch (e: IllegalArgumentException) {
            BackupValidationResult.Invalid(MALFORMED_MESSAGE)
        }
    }
}
