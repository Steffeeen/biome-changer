package de.steffeeen.biomechanger

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin

lateinit var plugin: BiomeChanger

class BiomeChanger : JavaPlugin(), Listener {

    init {
        plugin = this
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        getCommand("biomechanger")?.setExecutor { player, _, _, _ ->
            if (player !is Player) {
                player.sendMessage("Only players can use this command")
                return@setExecutor true
            }

            if (!player.isOp) {
                player.sendMessage(Component.text("You have to be op to use this command").color(NamedTextColor.RED))
                return@setExecutor true
            }

            player.inventory.addItem(createBiomeChangerToolItem())

            return@setExecutor true
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.item == null) {
            return
        }
        val biomeChangerTool = event.item!!.toBiomeChangerTool() ?: return

        val action = event.action
        val isSneaking = event.player.isSneaking

        when {
            action == Action.RIGHT_CLICK_BLOCK && isSneaking -> handleBiomeSelection(biomeChangerTool, event)
            action == Action.RIGHT_CLICK_BLOCK -> handleBiomeChanging(biomeChangerTool, event)
            action == Action.RIGHT_CLICK_AIR && isSneaking -> handleSizeChanging(biomeChangerTool, event)
        }
    }

    private fun handleBiomeChanging(
        biomeChangerTool: BiomeChangerTool, event: PlayerInteractEvent,
    ) {
        when (val result = biomeChangerTool.changeBiomeStartingFrom(event.clickedBlock!!)) {
            is BiomeChangerTool.Result.Error -> event.player.sendActionBar(
                Component.text(result.message).color(NamedTextColor.RED)
            )

            is BiomeChangerTool.Result.SuccessWithData<List<Location>> -> result.data.forEach {
                it.world.spawnParticle(
                    Particle.FLAME, it.clone().add(0.5, 1.5, 0.5), 1, 0.0, 0.0, 0.0, 0.0
                )
            }
        }
    }

    private fun handleBiomeSelection(
        biomeChangerTool: BiomeChangerTool, event: PlayerInteractEvent,
    ) {
        val block = event.clickedBlock!!
        when (val result = biomeChangerTool.changeSelectedBiomeTo(block.biome)) {
            is BiomeChangerTool.Result.Error -> event.player.sendActionBar(
                Component.text(result.message).color(NamedTextColor.RED)
            )

            is BiomeChangerTool.Result.Success -> event.player.sendActionBar(
                Component.text("Selected ${block.biome.toDisplayString()}").color(NamedTextColor.GREEN)
            )
        }
    }

    private fun handleSizeChanging(biomeChangerTool: BiomeChangerTool, event: PlayerInteractEvent) {
        val newSize = biomeChangerTool.changeSize()
        event.player.sendActionBar(Component.text("Changed size to ${newSize.toString().lowercase()}"))
    }
}

