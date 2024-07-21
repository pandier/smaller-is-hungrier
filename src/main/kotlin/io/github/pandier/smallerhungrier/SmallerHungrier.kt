package io.github.pandier.smallerhungrier

import com.google.inject.Inject
import org.apache.logging.log4j.Logger
import org.spongepowered.api.Server
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.Keys
import org.spongepowered.api.entity.attribute.type.AttributeTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.data.ChangeDataHolderEvent
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent
import org.spongepowered.api.event.lifecycle.StartingEngineEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.io.IOException
import java.nio.file.Path

@Suppress("unused")
@Plugin("smallerhungrier")
class SmallerHungrier @Inject constructor(
    private val logger: Logger,
    private val pluginContainer: PluginContainer,
    @DefaultConfig(sharedRoot = true)
    private val configPath: Path
) {
    private val configLoader = HoconConfigurationLoader.builder()
        .path(configPath)
        .build()
    private lateinit var config: Configuration

    @Listener
    private fun handleStarting(event: StartingEngineEvent<Server>) {
        // Load the configuration and save the defaults
        try {
            config = configLoader.load().get(Configuration::class.java)!!
            configLoader.save(configLoader.createNode().set(config))
        } catch (ex: IOException) {
            logger.error("Failed to load configuration, loading defaults instead", ex)
            config = Configuration()
        }
    }

    /**
     * Calculates the scale based on the food level of the specified [player].
     * The range in which the scale will be mapped can be configured
     * in the plugin's [Configuration].
     */
    private fun calculateScale(player: Player): Double =
        calculateScale(player.foodLevel().get())

    /**
     * Calculates the scale based on the specified [foodLevel].
     * The range in which the scale will be mapped can be configured
     * in the plugin's [Configuration].
     */
    private fun calculateScale(foodLevel: Int): Double =
        config.range.map(foodLevel / 20.0)

    /**
     * Updates the player's scale attribute based on the specified [scale].
     * Uses [calculateScale] if scale is not specified.
     */
    private fun updateScale(player: Player, scale: Double = calculateScale(player)) {
        val scaleAttribute = player.attribute(AttributeTypes.GENERIC_SCALE)
            .orElseThrow { IllegalStateException("Could not find 'generic.scale' attribute for player '${player.name()}'") }
        scaleAttribute.setBaseValue(scale)
    }

    @Listener
    fun handleJoin(event: ServerSideConnectionEvent.Join) {
        updateScale(event.player())
    }

    @Listener
    fun handlePlayerSpawn(event: RespawnPlayerEvent.Post) {
        updateScale(event.entity())
    }

    @Listener
    fun handleFoodLevelChange(event: ChangeDataHolderEvent.ValueChange) {
        val player = event.targetHolder() as? ServerPlayer ?: return
        val value = event.endResult().successfulValue(Keys.FOOD_LEVEL).orElse(null) ?: return
        updateScale(player, calculateScale(value.get()))
    }

    @ConfigSerializable
    data class Configuration(
        @field:Comment(
            "Defines the range in which the resulted scale will be.\n"
                    + "\n"
                    + "The high property defines the scale when the player has 20 hunger\n"
                    + "and the low property defines the scale when the player reaches 0 hunger.\n"
                    + "\n"
                    + "You can also set the low value higher than the high value to make the player bigger when hungrier!"
        )
        val range: Range = Range()
    ) {
        @ConfigSerializable
        data class Range(
            val low: Double = 0.45,
            val high: Double = 1.0
        ) {
            fun map(value: Double): Double =
                low + (high - low) * value
        }
    }
}