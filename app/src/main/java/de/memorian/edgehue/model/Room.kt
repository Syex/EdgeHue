package de.memorian.edgehue.model

data class Room(
        val name: String,
        val enabled: Boolean,
        val lights: List<Light>
)

data class Light(
        val name: String,
        val enabled: Boolean,
        val reachable: Boolean
)