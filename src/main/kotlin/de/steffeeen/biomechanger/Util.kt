package de.steffeeen.biomechanger

import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import kotlin.reflect.KProperty


val BIOME_TYPE = enumPersistentDataType<Biome>()

val SIZE_TYPE = enumPersistentDataType<Size>()

inline fun <reified T : Enum<T>> enumPersistentDataType(): EnumPersistentDataType<T> = EnumPersistentDataType(T::class.java)

class EnumPersistentDataType<T>(private val clazz: Class<T>) : PersistentDataType<String, T> where T : Enum<T> {
    override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): T {
        return java.lang.Enum.valueOf(clazz, primitive)
    }

    override fun toPrimitive(complex: T, context: PersistentDataAdapterContext): String {
        return complex.name
    }

    override fun getPrimitiveType(): Class<String> {
        return String::class.java
    }

    override fun getComplexType(): Class<T> {
        return clazz
    }
}

inline fun <reified T : Enum<T>> Enum<T>.cycle(): T {
    val index = (ordinal + 1) % enumValues<T>().size
    return enumValues<T>()[index]
}

interface IPersistentDataDelegate<T, Z> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Z?
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Z?)
}

class PersistentDataDelegate<T, Z> private constructor(
    private val delegate: IPersistentDataDelegate<T, Z>,
) : IPersistentDataDelegate<T, Z> by delegate {
    constructor(
        key: NamespacedKey,
        type: PersistentDataType<T, Z>,
        item: ItemStack,
        onChange: (old: Z?, new: Z?) -> Unit = { _, _ -> },
    ) : this(
        NullablePersistentDataDelegate(key, type, item, onChange)
    )

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Z {
        return delegate.getValue(thisRef, property)!!
    }
}

class NullablePersistentDataDelegate<T, Z>(
    private val key: NamespacedKey,
    private val type: PersistentDataType<T, Z>,
    private val item: ItemStack,
    private val onChange: (old: Z?, new: Z?) -> Unit = { _, _ -> },
) : IPersistentDataDelegate<T, Z> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Z? {
        return item.itemMeta.persistentDataContainer[key, type]
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Z?) {
        val oldValue = item.itemMeta.persistentDataContainer[key, type]
        if (value == null) {
            item.editMeta { it.persistentDataContainer.remove(key) }
        } else {
            item.editMeta { it.persistentDataContainer.set(key, type, value) }
        }
        onChange(oldValue, value)
    }
}