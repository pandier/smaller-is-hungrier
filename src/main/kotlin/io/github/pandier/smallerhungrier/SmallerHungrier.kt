package io.github.pandier.smallerhungrier

import com.google.inject.Inject
import org.apache.logging.log4j.Logger
import org.spongepowered.api.Server
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.DataTransactionResult
import org.spongepowered.api.data.Key
import org.spongepowered.api.data.Keys
import org.spongepowered.api.data.value.Value
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
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.io.IOException
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.function.Predicate

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
        .defaultOptions { options ->
            options.serializers { builder ->
                builder.register(Configuration.Source.Serializer())
            }
        }
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
     * Updates the player's scale attribute based on the specified [scale].
     * Uses [Configuration.scaleForPlayer] if scale is not specified.
     */
    private fun updateScale(player: Player, scale: Double = config.scaleForPlayer(player)) {
        val scaleAttribute = player.attribute(AttributeTypes.GENERIC_SCALE)
            .orElseThrow { IllegalStateException("Could not find 'generic.scale' attribute for player '${player.name()}'") }
        scaleAttribute.setBaseValue(scale)
    }

    @Listener
    private fun handleJoin(event: ServerSideConnectionEvent.Join) {
        updateScale(event.player())
    }

    @Listener
    private fun handlePlayerSpawn(event: RespawnPlayerEvent.Post) {
        updateScale(event.entity())
    }

    @Listener
    private fun handleFoodLevelChange(event: ChangeDataHolderEvent.ValueChange) {
        val player = event.targetHolder() as? ServerPlayer ?: return
        val scale = config.scaleForTransaction(event.endResult())
        if (scale != null) {
            updateScale(player, scale)
        }
    }

    @ConfigSerializable
    data class Configuration(
        @field:Comment(
            "Defines the source which will be used to calculate the scale.\n"
                    + "\n"
                    + "Possible values:\n"
                    + "  - food = uses the players food level with high at 20\n"
                    + "  - health = uses the players health with high at 20 (players will grow on excess health)"
        )
        val source: Source = Source.FOOD,

        @field:Comment(
            "Defines the range in which the resulted scale will be.\n"
                    + "\n"
                    + "The high property defines the scale when the player has 20 hunger/health\n"
                    + "and the low property defines the scale when the player reaches 0 hunger/health.\n"
                    + "\n"
                    + "You can also set the low value higher than the high value to make the player bigger when hungrier!"
        )
        val range: Range = Range()
    ) {
        enum class Source(val function: Function<*>) {
            FOOD(Function(Keys.FOOD_LEVEL) { it / 20.0 }),
            HEALTH(Function(Keys.HEALTH) { it / 20.0 });

            class Function<E>(
                private val key: Key<Value<E>>,
                private val mapper: (E) -> Double
            ) {
                fun invokeTransaction(transaction: DataTransactionResult): Double? {
                    val value = transaction.successfulValue(key).orElse(null) ?: return null
                    return mapper(value.get())
                }

                fun invokePlayer(player: Player): Double =
                    mapper.invoke(player.require(key))
            }

            class Serializer : ScalarSerializer<Source>(Source::class.java) {
                override fun deserialize(type: Type, obj: Any): Source {
                    val name = obj.toString().uppercase()
                    return Source.entries.find { it.name == name }
                        ?: throw SerializationException("Unknown source: $obj")
                }

                override fun serialize(obj: Source, typeSupported: Predicate<Class<*>>): Any {
                    return obj.name.lowercase()
                }
            }
        }

        @ConfigSerializable
        data class Range(
            val low: Double = 0.45,
            val high: Double = 1.0
        ) {
            fun map(value: Double): Double =
                low + (high - low) * value
        }

        /**
         * Calculates scale for the specified [player]
         * based on the user specified settings.
         */
        fun scaleForPlayer(player: Player): Double =
            source.function.invokePlayer(player).let(range::map)

        /**
         * Calculates scale for the specified [transaction]
         * based on the user specified settings.
         *
         * Returns null if the transaction doesn't contain
         * the required successful value.
         */
        fun scaleForTransaction(transaction: DataTransactionResult): Double? =
            source.function.invokeTransaction(transaction)?.let(range::map)
    }
}