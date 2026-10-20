package com.carled.app.data.local

/** Mirrors FE Character/UserEntitlement IDs. Replace this adapter when BE exposes ownership APIs. */
data class OwnedCharacter(val characterId: String, val name: String, val asset: String,
    val animationType: String, val entitlementId: String, val sourceType: String = "PAYMENT")

interface CharacterInventoryRepository { fun listOwned(userId: String): List<OwnedCharacter> }
class MockCharacterInventory : CharacterInventoryRepository {
    override fun listOwned(userId: String): List<OwnedCharacter> = if (userId.isBlank()) emptyList() else listOf(
        OwnedCharacter("character-cat-hero", "Mirage Cat Hero", "characters/mirage-cat.png", "LOOP", "mock-cat-entitlement"),
        OwnedCharacter("character-space-robot", "Space Robot", "characters/space-robot.png", "LOOP", "mock-robot-entitlement"),
        OwnedCharacter("character-dragon", "Mini Dragon", "characters/mini-dragon.png", "SPRITE", "mock-dragon-entitlement")
    )
}
