package dev.myengine.core

class StableHash(seed: Long = FNV_OFFSET_BASIS) {
    private var value: Long = seed

    fun add(text: String): StableHash {
        text.encodeToByteArray().forEach { addByte(it.toInt() and 0xff) }
        addByte(0)
        return this
    }

    fun add(number: Int): StableHash {
        add(number.toLong())
        return this
    }

    fun add(number: Long): StableHash {
        for (shift in 0 until 64 step 8) {
            addByte(((number ushr shift) and 0xff).toInt())
        }
        return this
    }

    fun add(flag: Boolean): StableHash {
        addByte(if (flag) 1 else 0)
        return this
    }

    fun digest(): String = value.toULong().toString(16).padStart(16, '0')

    private fun addByte(byte: Int) {
        value = value xor byte.toLong()
        value *= FNV_PRIME
    }

    private companion object {
        private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L
        private const val FNV_PRIME: Long = 1099511628211L
    }
}

interface HashableState {
    fun appendHash(hash: StableHash)
}

fun stableHashOf(block: StableHash.() -> Unit): String = StableHash().apply(block).digest()
