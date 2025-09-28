package de.steffeeen.biomechanger

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private val BIOME_CHANGER_TOOL_KEY = NamespacedKey(plugin, "biomeChangerTool")
private val SELECTED_BIOME_KEY = NamespacedKey(plugin, "selectedBiome")
private val USES_REMAINING_KEY = NamespacedKey(plugin, "usesRemaining")
private val SIZE_KEY = NamespacedKey(plugin, "size")

private const val DEFAULT_USES = 2500

private val ILLEGAL_BIOMES = listOf(
    Biome.NETHER_WASTES,
    Biome.BASALT_DELTAS,
    Biome.CRIMSON_FOREST,
    Biome.WARPED_FOREST,
    Biome.SOUL_SAND_VALLEY,
    Biome.THE_VOID,
    Biome.THE_END,
    Biome.SMALL_END_ISLANDS,
    Biome.END_BARRENS,
    Biome.END_MIDLANDS,
    Biome.END_HIGHLANDS,
    Biome.DEEP_DARK,
)

enum class Size(val radius: Int) {
    // ~5x5
    SMALL(2),

    // ~11x11
    @Suppress("unused")
    LARGE(5)
}

fun ItemStack.toBiomeChangerTool(): BiomeChangerTool? {
    if (itemMeta.persistentDataContainer.getOrDefault(BIOME_CHANGER_TOOL_KEY, PersistentDataType.BYTE, 0) <= 0) {
        return null
    }
    return try {
        BiomeChangerTool(this)
    } catch (e: IllegalArgumentException) {
        plugin.logger.warning("Found broken BiomeChangerTool")
        lore()?.let {
            it.add(Component.text("BROKEN BIOME CHANGER").color(TextColor.fromHexString("#ff0000")))
            it.add(Component.text("Missing ${BIOME_CHANGER_TOOL_KEY.namespace}:${BIOME_CHANGER_TOOL_KEY.key} key"))
        }
        null
    }
}

fun createBiomeChangerToolItem(): ItemStack {
    val item = ItemStack(Material.BLAZE_ROD)
    item.editMeta {
        it.persistentDataContainer.set(BIOME_CHANGER_TOOL_KEY, PersistentDataType.BYTE, 1)
        it.persistentDataContainer.set(USES_REMAINING_KEY, PersistentDataType.INTEGER, DEFAULT_USES)
        it.persistentDataContainer.set(SIZE_KEY, SIZE_TYPE, Size.SMALL)
        it.displayName(Component.text("Biome Changer").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        it.setCustomModelData(100)
    }
    item.lore(loreForBiomeChanger(null, DEFAULT_USES, Size.SMALL))

    return item
}

class BiomeChangerTool(private val item: ItemStack) {

    private var selectedBiome: Biome? by NullablePersistentDataDelegate(
        SELECTED_BIOME_KEY,
        BiomePersistentDataType,
        item,
    ) { _, new -> item.lore(loreForBiomeChanger(new, usesRemaining, size)) }

    private var usesRemaining: Int by PersistentDataDelegate(
        USES_REMAINING_KEY,
        PersistentDataType.INTEGER,
        item,
    ) { _, new -> item.lore(loreForBiomeChanger(selectedBiome, new!!, size)) }

    private var size: Size by PersistentDataDelegate(
        SIZE_KEY,
        SIZE_TYPE,
        item,
    ) { _, new -> item.lore(loreForBiomeChanger(selectedBiome, usesRemaining, new!!)) }

    private val costs: Int
        get() = if (size == Size.SMALL) 1 else 3

    init {
        require(
            item.itemMeta.persistentDataContainer.getOrDefault(BIOME_CHANGER_TOOL_KEY, PersistentDataType.BYTE, 0) > 0
        ) { "BIOME_CHANGER_TOOL_KEY missing" }
        require(item.itemMeta.persistentDataContainer.has(USES_REMAINING_KEY)) { "USES_REMAINING_KEY missing" }
        require(item.itemMeta.persistentDataContainer.has(SIZE_KEY)) { "SIZE_KEY missing" }
    }

    fun changeBiomeStartingFrom(block: Block, event: PlayerInteractEvent): ResultWithData<List<Location>> {
        if (selectedBiome == null) {
            return Result.Error("You have to selected a biome first")
        }

        if (usesRemaining < costs) {
            return Result.Error("You don't have enough uses left")
        }

        if (block.world.environment != World.Environment.NORMAL) {
            return Result.Error("You can only change the biome in the overworld")
        }

        val columnsToChange = columnsToChange(block.location, size.radius)
        val biomeToChangeTo: Biome = selectedBiome!!
        columnsToChange.forEach { changeBiomeForColumn(it, biomeToChangeTo) }

        usesRemaining -= costs

        if (usesRemaining <= 0) {
            val hand = event.hand!!
            event.player.inventory.setItem(hand, ItemStack(Material.AIR))
        }

        return Result.SuccessWithData(columnsToChange)
    }

    fun changeSize(): Size {
        size = size.cycle()
        return size
    }

    fun changeSelectedBiomeTo(biome: Biome): Result {
        if (biome in ILLEGAL_BIOMES) {
            return Result.Error("This biome can't be selected")
        }

        selectedBiome = biome
        return Result.Success
    }

    sealed interface ResultWithData<out T>
    sealed interface Result {
        object Success : Result
        data class SuccessWithData<out T>(val data: T) : ResultWithData<T>
        data class Error(val message: String) : Result, ResultWithData<Nothing>
    }
}

private fun columnsToChange(center: Location, radius: Int): List<Location> {
    val locations = mutableListOf<Location>()
    for (x in (center.blockX - radius)..(center.blockX + radius)) {
        for (z in (center.blockZ - radius)..(center.blockZ + radius)) {
            locations += Location(center.world, x.toDouble(), center.y, z.toDouble())
        }
    }

    return locations
}

private fun changeBiomeForColumn(location: Location, biomeToChangeTo: Biome) {
    val biome = location.block.biome
    val world = location.world
    val minY = world.minHeight
    val maxY = world.maxHeight

    val x = location.blockX
    val z = location.blockZ

    for (y in location.y.toInt() until maxY) {
        if (world.getBiome(x, y, z) == biome) {
            world.setBiome(x, y, z, biomeToChangeTo)
        }
    }

    for (y in location.y.toInt() downTo minY) {
        if (world.getBiome(x, y, z) == biome) {
            world.setBiome(x, y, z, biomeToChangeTo)
        }
    }
}

private fun loreForBiomeChanger(selectedBiome: Biome?, usesRemaining: Int, size: Size): List<Component> {
    return mutableListOf(
        Component.text("Biome: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            .append(biomeComponent(selectedBiome).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)),
        Component.text("Uses: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            .append(Component.text("$usesRemaining").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)),
        Component.text("Size: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(size.toString().lowercase()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)),
    )
}

private fun biomeComponent(selectedBiome: Biome?): Component =
    when (selectedBiome) {
        null -> Component.text("none")
        else -> selectedBiome.getNameAsComponent()
    }

fun Biome.getNameAsComponent(): Component {
    return Component.translatable(this.translationKey())
}

