package com.example.data.model

enum class VoiceCommand {
    TAKE_PHOTO,
    START_VIDEO,
    STOP_VIDEO,
    SWITCH_CAMERA,
    FLASH_ON,
    FLASH_OFF,
    ZOOM_IN,
    ZOOM_OUT,
    OPEN_GALLERY,
    NEXT,
    PREVIOUS,
    SELECT,
    DELETE,
    CONFIRM,
    CANCEL,
    HELP,
    UNKNOWN;

    companion object {
        fun fromSpokenText(spokenText: String): VoiceCommand {
            val normalized = spokenText.lowercase().trim()
            return when {
                normalized.contains("confirm") || normalized == "yes" || normalized == "ok" -> CONFIRM
                normalized.contains("cancel") || normalized == "no" -> CANCEL
                normalized.contains("help") -> HELP
                normalized.contains("next") -> NEXT
                normalized.contains("prev") || normalized == "back" -> PREVIOUS
                normalized.contains("delete") || normalized.contains("trash") || normalized.contains("remove") -> DELETE
                normalized.contains("gallery") -> OPEN_GALLERY
                normalized.contains("start video") || normalized.contains("record") -> START_VIDEO
                normalized.contains("stop") -> STOP_VIDEO
                normalized.contains("switch") || normalized.contains("flip") -> SWITCH_CAMERA
                normalized.contains("flash on") -> FLASH_ON
                normalized.contains("flash off") -> FLASH_OFF
                normalized.contains("zoom in") -> ZOOM_IN
                normalized.contains("zoom out") -> ZOOM_OUT
                normalized.contains("select") -> SELECT
                normalized.contains("photo") || normalized.contains("shoot") || normalized.contains("capture") || normalized.contains("cheese") || normalized.contains("picture") -> TAKE_PHOTO
                else -> UNKNOWN
            }
        }
    }
}
