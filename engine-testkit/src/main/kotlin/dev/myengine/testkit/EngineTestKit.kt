package dev.myengine.testkit

object EngineTestKit {
    fun assertStableHash(first: String, second: String) {
        require(first == second) { "Expected stable replay hash, got '$first' and '$second'." }
    }
}

