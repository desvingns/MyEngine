package dev.myengine.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.math.BigDecimal
import java.util.ArrayDeque
import java.util.Properties

data class ContentValidationError(
    val file: String,
    val id: String,
    val field: String,
    val message: String,
) {
    override fun toString(): String = "$file:$id:$field: $message"
}

data class ContentLoadResult(
    val registry: ContentRegistry?,
    val errors: List<ContentValidationError>,
) {
    val isValid: Boolean = errors.isEmpty() && registry != null
}

object ContentPackLoader {
    const val SUPPORTED_SCHEMA_VERSION: Int = 1

    fun load(root: Path): ContentLoadResult {
        val errors = mutableListOf<ContentValidationError>()
        val manifestProps = readProperties(root.resolve("manifest.properties"), errors)
        val manifest = manifestProps?.let { parseManifest(it, errors) }

        val tiles = parseDefinitions(root, "tiles.properties", errors, ::parseTile)
        val resources = parseDefinitions(root, "resources.properties", errors, ::parseResource)
        val towers = parseDefinitions(root, "towers.properties", errors, ::parseTower)
        val enemies = parseDefinitions(root, "enemies.properties", errors, ::parseEnemy)
        val recipes = parseDefinitions(root, "recipes.properties", errors, ::parseRecipe)
        val waves = parseDefinitions(root, "waves.properties", errors, ::parseWave)
        val incidents = parseDefinitions(root, "incidents.properties", errors, ::parseIncident)
        val difficulties = parseOptionalDefinitions(root, "difficulties.properties", errors, ::parseDifficulty)
        val strings = readProperties(root.resolve("strings.properties"), errors)?.entries
            ?.associate { it.key.toString() to it.value.toString() }
            ?: emptyMap()
        val maps = parseMaps(root, tiles, resources, errors)

        if (manifest != null && manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors += ContentValidationError("manifest.properties", manifest.id, "schemaVersion", "Unsupported schema version ${manifest.schemaVersion}.")
        }

        validateReferences(towers, enemies, recipes, waves, resources, errors)
        validateTerminalRules(maps, waves, errors)
        validateLocalization(resources, towers, strings, errors)

        if (errors.isNotEmpty() || manifest == null) {
            return ContentLoadResult(null, errors)
        }

        return ContentLoadResult(
            registry = ContentRegistry(
                manifest = manifest,
                tiles = tiles,
                resources = resources,
                towers = towers,
                enemies = enemies,
                recipes = recipes,
                waves = waves,
                incidents = incidents,
                strings = strings,
                difficulties = difficulties,
                maps = maps,
            ),
            errors = emptyList(),
        )
    }

    private fun parseManifest(props: Properties, errors: MutableList<ContentValidationError>): ContentPackManifest? {
        fun required(field: String): String? = props.getProperty(field)
            ?: run {
                errors += ContentValidationError("manifest.properties", "pack", field, "Required field is missing.")
                null
            }
        val id = required("id") ?: return null
        return ContentPackManifest(
            id = id,
            version = required("version") ?: return null,
            schemaVersion = required("schemaVersion")?.toIntOrNull()
                ?: return errors.addAndNull("manifest.properties", id, "schemaVersion", "Expected integer."),
            engineMin = required("engineMin") ?: return null,
            engineMax = required("engineMax") ?: return null,
            locales = props.getProperty("locales", "en").splitCsv(),
            dependencies = props.getProperty("dependencies", "").splitCsv(),
        )
    }

    private fun <T : ContentDefinition> parseDefinitions(
        root: Path,
        fileName: String,
        errors: MutableList<ContentValidationError>,
        parser: (String, Map<String, String>, MutableList<ContentValidationError>, String) -> T?,
    ): Map<String, T> {
        val props = readProperties(root.resolve(fileName), errors) ?: return emptyMap()
        val byId = props.entries
            .map { it.key.toString() to it.value.toString() }
            .groupBy(
                keySelector = { it.first.substringBefore('.') },
                valueTransform = { it.first.substringAfter('.', "") to it.second },
            )
        val parsed = linkedMapOf<String, T>()
        byId.toSortedMap().forEach { (id, pairs) ->
            if (id.isBlank()) {
                errors += ContentValidationError(fileName, id, "id", "Definition id is blank.")
                return@forEach
            }
            val fields = pairs.toMap()
            val definition = parser(id, fields, errors, fileName) ?: return@forEach
            if (parsed.containsKey(id)) {
                errors += ContentValidationError(fileName, id, "id", "Duplicate definition id.")
            } else {
                parsed[id] = definition
            }
        }
        return parsed
    }

    private fun <T : ContentDefinition> parseOptionalDefinitions(
        root: Path,
        fileName: String,
        errors: MutableList<ContentValidationError>,
        parser: (String, Map<String, String>, MutableList<ContentValidationError>, String) -> T?,
    ): Map<String, T> =
        if (Files.exists(root.resolve(fileName))) {
            parseDefinitions(root, fileName, errors, parser)
        } else {
            emptyMap()
        }

    private fun parseTile(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): TileContent? =
        TileContent(
            id = id,
            buildable = fields.requiredBool(file, id, "buildable", errors) ?: return null,
            blocksMovement = fields.requiredBool(file, id, "blocksMovement", errors) ?: return null,
            isCore = fields["isCore"]?.toBooleanStrictOrNull() ?: false,
        )

    private fun parseResource(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): ResourceContent? =
        ResourceContent(
            id = id,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
        )

    private fun parseTower(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): TowerContent? =
        TowerContent(
            id = id,
            displayKey = fields.required(file, id, "displayKey", errors) ?: return null,
            range = fields.requiredPositiveInt(file, id, "range", errors) ?: return null,
            damage = fields.requiredPositiveInt(file, id, "damage", errors) ?: return null,
            cooldownTicks = fields.requiredPositiveInt(file, id, "cooldownTicks", errors) ?: return null,
            costResource = fields.required(file, id, "costResource", errors) ?: return null,
            costAmount = fields.requiredNonNegativeInt(file, id, "costAmount", errors) ?: return null,
            upgradeTiers = parseTowerUpgradeTiers(id, fields, errors, file),
        )

    private fun parseTowerUpgradeTiers(
        id: String,
        fields: Map<String, String>,
        errors: MutableList<ContentValidationError>,
        file: String,
    ): Map<String, TowerUpgradeTier> {
        val groups = linkedMapOf<Pair<String, Int>, MutableMap<String, String>>()
        fields.filterKeys { it.startsWith("upgrade.") }.forEach { (field, value) ->
            val parts = field.split('.')
            if (parts.size != 4) {
                errors += ContentValidationError(file, id, field, "Expected upgrade.<branch>.<tier>.<field>.")
                return@forEach
            }
            val branch = parts[1]
            val tier = parts[2].toIntOrNull()
            val upgradeField = parts[3]
            if (!branch.matches(TowerUpgradeTier.BRANCH_ID_REGEX)) {
                errors += ContentValidationError(file, id, field, "Upgrade branch must match ${TowerUpgradeTier.BRANCH_ID_REGEX.pattern}.")
                return@forEach
            }
            if (tier == null || tier <= 0) {
                errors += ContentValidationError(file, id, field, "Upgrade tier must be a positive integer.")
                return@forEach
            }
            groups.getOrPut(branch to tier) { linkedMapOf() }[upgradeField] = value
        }

        return groups.entries
            .sortedWith(compareBy<Map.Entry<Pair<String, Int>, MutableMap<String, String>>> { it.key.first }.thenBy { it.key.second })
            .mapNotNull { (key, upgradeFields) ->
                val (branch, tier) = key
                val prefix = "upgrade.$branch.$tier"
                val scopedFields = upgradeFields.mapKeys { "$prefix.${it.key}" }
                TowerUpgradeTier(
                    branch = branch,
                    tier = tier,
                    displayKey = scopedFields.required(file, id, "$prefix.displayKey", errors) ?: return@mapNotNull null,
                    range = scopedFields.requiredPositiveInt(file, id, "$prefix.range", errors) ?: return@mapNotNull null,
                    damage = scopedFields.requiredPositiveInt(file, id, "$prefix.damage", errors) ?: return@mapNotNull null,
                    cooldownTicks = scopedFields.requiredPositiveInt(file, id, "$prefix.cooldownTicks", errors) ?: return@mapNotNull null,
                    costResource = scopedFields.required(file, id, "$prefix.costResource", errors) ?: return@mapNotNull null,
                    costAmount = scopedFields.requiredNonNegativeInt(file, id, "$prefix.costAmount", errors) ?: return@mapNotNull null,
                )
            }
            .associateBy { it.key }
    }

    private fun parseEnemy(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): EnemyContent? =
        EnemyContent(
            id = id,
            health = fields.requiredPositiveInt(file, id, "health", errors) ?: return null,
            speedTilesPerTick = fields.requiredPositiveInt(file, id, "speedTilesPerTick", errors) ?: return null,
            rewardResource = fields.required(file, id, "rewardResource", errors) ?: return null,
            rewardAmount = fields.requiredNonNegativeInt(file, id, "rewardAmount", errors) ?: return null,
            coreDamage = fields.requiredPositiveInt(file, id, "coreDamage", errors) ?: return null,
        )

    private fun parseRecipe(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): RecipeContent? =
        RecipeContent(
            id = id,
            inputResource = fields["inputResource"]?.takeIf { it.isNotBlank() },
            inputAmount = fields["inputAmount"]?.toIntOrNull() ?: 0,
            outputResource = fields.required(file, id, "outputResource", errors) ?: return null,
            outputAmount = fields.requiredPositiveInt(file, id, "outputAmount", errors) ?: return null,
            durationTicks = fields.requiredPositiveInt(file, id, "durationTicks", errors) ?: return null,
        )

    private fun parseWave(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): WaveContent? {
        val spawns = fields.required(file, id, "spawns", errors)
            ?.splitCsv()
            ?.mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2 || parts[1].toIntOrNull() == null) {
                    errors += ContentValidationError(file, id, "spawns", "Expected enemyId:count list.")
                    null
                } else {
                    WaveSpawn(parts[0], parts[1].toInt())
                }
            }
            ?: return null
        return WaveContent(
            id = id,
            startTick = fields.requiredNonNegativeLong(file, id, "startTick", errors) ?: return null,
            spawns = spawns,
        )
    }

    private fun parseIncident(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): IncidentContent? =
        IncidentContent(
            id = id,
            minThreat = fields.requiredNonNegativeInt(file, id, "minThreat", errors) ?: return null,
            maxThreat = fields.requiredNonNegativeInt(file, id, "maxThreat", errors) ?: return null,
            weight = fields.requiredPositiveInt(file, id, "weight", errors) ?: return null,
        )

    private fun parseDifficulty(id: String, fields: Map<String, String>, errors: MutableList<ContentValidationError>, file: String): DifficultyContent? =
        DifficultyContent(
            id = id,
            healthMult = fields.requiredPositiveDecimal(file, id, "healthMult", errors) ?: return null,
            countMult = fields.requiredPositiveDecimal(file, id, "countMult", errors) ?: return null,
            rewardMult = fields.requiredPositiveDecimal(file, id, "rewardMult", errors) ?: return null,
            goldRateMult = fields.requiredPositiveDecimal(file, id, "goldRateMult", errors) ?: return null,
        )

    private fun parseMaps(
        root: Path,
        tiles: Map<String, TileContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ): Map<String, MapContent> {
        val file = "maps.json"
        val path = root.resolve(file)
        if (!Files.exists(path)) return emptyMap()

        val document = try {
            Files.newBufferedReader(path).use { reader -> Json.parseToJsonElement(reader.readText()) as? JsonObject }
        } catch (error: Exception) {
            errors += ContentValidationError(file, "file", "json", "Invalid JSON: ${error.message ?: error::class.simpleName}.")
            return emptyMap()
        }
        if (document == null) {
            errors += ContentValidationError(file, "file", "json", "Expected a top-level JSON object.")
            return emptyMap()
        }
        val definitions = document["maps"] as? JsonArray
        if (definitions == null) {
            errors += ContentValidationError(file, "file", "maps", "Expected a 'maps' array.")
            return emptyMap()
        }

        val parsed = linkedMapOf<String, MapContent>()
        definitions.forEachIndexed { index, value ->
            val objectValue = value as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, "maps[$index]", "entry", "Expected a map object.")
                return@forEachIndexed
            }
            val map = parseMap(objectValue, file, "maps[$index]", errors) ?: return@forEachIndexed
            if (parsed.put(map.id, map) != null) {
                errors += ContentValidationError(file, map.id, "id", "Duplicate map id.")
            }
        }
        parsed.values.forEach { validateMap(it, tiles, resources, errors) }
        return parsed
    }

    private fun parseMap(
        value: JsonObject,
        file: String,
        fallbackId: String,
        errors: MutableList<ContentValidationError>,
    ): MapContent? {
        val id = value.requiredString("id", file, fallbackId, "id", errors) ?: return null
        val width = value.requiredInt("width", file, id, "width", errors) ?: 0
        val height = value.requiredInt("height", file, id, "height", errors) ?: 0
        val terrainRows = value.requiredStringArray("terrainRows", file, id, errors)
        val terrainMapping = parseTerrainMapping(value["terrainMapping"], file, id, errors)
        val spawns = parseSpawns(value["spawns"], file, id, errors)
        val core = parseCoordinate(value["core"], file, id, "core", errors) ?: MapCoordinate(-1, -1)
        val terminalRules = parseTerminalRules(value["terminalRules"], file, id, errors)
        return MapContent(id, width, height, terrainRows, terrainMapping, spawns, core, terminalRules)
    }

    private fun parseTerminalRules(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): MapTerminalRules {
        if (value == null) return MapTerminalRules()
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, "terminalRules", "Expected an object with optional winCondition and leakBudget.")
            return MapTerminalRules()
        }

        val winCondition = when (val raw = objectValue["winCondition"]) {
            null -> MapWinCondition.FINITE_WAVES
            is JsonPrimitive -> when (raw.contentOrNull) {
                "finite_waves" -> MapWinCondition.FINITE_WAVES
                "no_win", "endless" -> MapWinCondition.NO_WIN
                else -> {
                    errors += ContentValidationError(file, id, "terminalRules.winCondition", "Expected 'finite_waves', 'no_win', or 'endless'.")
                    MapWinCondition.FINITE_WAVES
                }
            }
            else -> {
                errors += ContentValidationError(file, id, "terminalRules.winCondition", "Expected a string.")
                MapWinCondition.FINITE_WAVES
            }
        }

        val leakBudget = when (val raw = objectValue["leakBudget"]) {
            null -> null
            is JsonPrimitive -> raw.intOrNull?.also { budget ->
                if (budget <= 0) {
                    errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Leak budget must be positive when declared.")
                }
            }?.takeIf { it > 0 } ?: run {
                if (raw.intOrNull == null) {
                    errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Expected a positive integer when declared.")
                }
                null
            }
            else -> {
                errors += ContentValidationError(file, id, "terminalRules.leakBudget", "Expected a positive integer when declared.")
                null
            }
        }
        return MapTerminalRules(winCondition, leakBudget)
    }

    private fun parseTerrainMapping(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): Map<Char, MapTerrainSymbol> {
        val mapping = value as? JsonObject
        if (mapping == null) {
            errors += ContentValidationError(file, id, "terrainMapping", "Expected an object that maps one-character symbols to terrain.")
            return emptyMap()
        }
        return buildMap {
            mapping.forEach { (symbol, definition) ->
                if (symbol.length != 1) {
                    errors += ContentValidationError(file, id, "terrainMapping.$symbol", "Terrain symbols must be exactly one character.")
                    return@forEach
                }
                val objectValue = definition as? JsonObject
                if (objectValue == null) {
                    errors += ContentValidationError(file, id, "terrainMapping.$symbol", "Expected a terrain mapping object.")
                    return@forEach
                }
                val terrainId = objectValue.requiredString("tile", file, id, "terrainMapping.$symbol.tile", errors) ?: return@forEach
                val resourceNode = parseResourceNode(objectValue["resource"], file, id, "terrainMapping.$symbol.resource", errors)
                put(symbol.single(), MapTerrainSymbol(terrainId, resourceNode))
            }
        }
    }

    private fun parseResourceNode(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): MapResourceNode? {
        if (value == null) return null
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, field, "Expected a resource object with id and amount.")
            return null
        }
        val resourceId = objectValue.requiredString("id", file, id, "$field.id", errors) ?: return null
        val amount = objectValue.requiredInt("amount", file, id, "$field.amount", errors) ?: return null
        return MapResourceNode(resourceId, amount)
    }

    private fun parseSpawns(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): Map<String, MapSpawn> {
        val array = value as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, "spawns", "Expected an array of named spawn objects.")
            return emptyMap()
        }
        val spawns = linkedMapOf<String, MapSpawn>()
        array.forEachIndexed { index, item ->
            val objectValue = item as? JsonObject
            if (objectValue == null) {
                errors += ContentValidationError(file, id, "spawns[$index]", "Expected a spawn object.")
                return@forEachIndexed
            }
            val spawnId = objectValue.requiredString("id", file, id, "spawns[$index].id", errors) ?: return@forEachIndexed
            val coordinate = parseCoordinate(objectValue, file, id, "spawns[$index]", errors) ?: return@forEachIndexed
            if (spawns.put(spawnId, MapSpawn(spawnId, coordinate)) != null) {
                errors += ContentValidationError(file, id, "spawns[$index].id", "Duplicate named spawn '$spawnId'.")
            }
        }
        return spawns
    }

    private fun parseCoordinate(
        value: kotlinx.serialization.json.JsonElement?,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): MapCoordinate? {
        val objectValue = value as? JsonObject
        if (objectValue == null) {
            errors += ContentValidationError(file, id, field, "Expected an object with integer x and y coordinates.")
            return null
        }
        val x = objectValue.requiredInt("x", file, id, "$field.x", errors) ?: return null
        val y = objectValue.requiredInt("y", file, id, "$field.y", errors) ?: return null
        return MapCoordinate(x, y)
    }

    private fun validateMap(
        map: MapContent,
        tiles: Map<String, TileContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        val file = "maps.json"
        if (map.width <= 0) errors += ContentValidationError(file, map.id, "width", "Map width must be positive.")
        if (map.height <= 0) errors += ContentValidationError(file, map.id, "height", "Map height must be positive.")
        if (map.terrainRows.size != map.height) {
            errors += ContentValidationError(file, map.id, "terrainRows", "Expected ${map.height} rows, found ${map.terrainRows.size}.")
        }
        map.terrainMapping.toSortedMap().forEach { (symbol, mapping) ->
            if (!tiles.containsKey(mapping.terrainId)) {
                errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.tile", "Unknown tile '${mapping.terrainId}'.")
            }
            mapping.resourceNode?.let { resource ->
                if (!resources.containsKey(resource.resourceId)) {
                    errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.resource.id", "Unknown resource '${resource.resourceId}'.")
                }
                if (resource.amount < 0) {
                    errors += ContentValidationError(file, map.id, "terrainMapping.$symbol.resource.amount", "Resource amount must be non-negative.")
                }
            }
        }

        val coreTiles = mutableListOf<MapCoordinate>()
        map.terrainRows.forEachIndexed { y, row ->
            if (row.length != map.width) {
                errors += ContentValidationError(file, map.id, "terrainRows[$y]", "Expected row width ${map.width}, found ${row.length}.")
            }
            row.forEachIndexed { x, symbol ->
                val mapping = map.terrainMapping[symbol]
                if (mapping == null) {
                    errors += ContentValidationError(file, map.id, "terrainRows[$y][$x]", "Unknown terrain symbol '$symbol'; add it to terrainMapping.")
                } else if (tiles[mapping.terrainId]?.isCore == true) {
                    coreTiles += MapCoordinate(x, y)
                }
            }
        }
        if (coreTiles.size != 1) {
            errors += ContentValidationError(file, map.id, "terrainRows", "Expected exactly one core tile, found ${coreTiles.size}.")
        }
        if (!map.inBounds(map.core)) {
            errors += ContentValidationError(file, map.id, "core", "Core coordinate (${map.core.x},${map.core.y}) is outside ${map.width}x${map.height}.")
        } else if (coreTiles.singleOrNull() != map.core) {
            errors += ContentValidationError(file, map.id, "core", "Core coordinate must point at the single core terrain tile.")
        }
        if (map.spawns.isEmpty()) {
            errors += ContentValidationError(file, map.id, "spawns", "At least one named spawn is required.")
        }

        val gridIsUsable = map.width > 0 && map.height > 0 &&
            map.terrainRows.size == map.height &&
            map.terrainRows.all { it.length == map.width } &&
            map.terrainRows.all { row -> row.all(map.terrainMapping::containsKey) } &&
            map.terrainMapping.values.all { tiles.containsKey(it.terrainId) }
        map.spawns.values.sortedBy { it.id }.forEach { spawn ->
            if (!map.inBounds(spawn.position)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "Spawn coordinate (${spawn.position.x},${spawn.position.y}) is outside ${map.width}x${map.height}.")
            } else if (gridIsUsable && !isWalkable(map, spawn.position, tiles)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "Spawn must be on walkable terrain.")
            } else if (gridIsUsable && coreTiles.singleOrNull() == map.core && !hasWalkablePath(map, spawn.position, map.core, tiles)) {
                errors += ContentValidationError(file, map.id, "spawns.${spawn.id}", "No walkable path from spawn '${spawn.id}' to core (${map.core.x},${map.core.y}).")
            }
        }
    }

    private fun hasWalkablePath(
        map: MapContent,
        start: MapCoordinate,
        target: MapCoordinate,
        tiles: Map<String, TileContent>,
    ): Boolean {
        if (!isWalkable(map, start, tiles) || !isWalkable(map, target, tiles)) return false
        val pending = ArrayDeque<MapCoordinate>()
        val visited = hashSetOf(start)
        pending += start
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current == target) return true
            listOf(
                MapCoordinate(current.x + 1, current.y),
                MapCoordinate(current.x, current.y + 1),
                MapCoordinate(current.x - 1, current.y),
                MapCoordinate(current.x, current.y - 1),
            ).filter { next -> map.inBounds(next) }.forEach { next ->
                if (visited.add(next) && isWalkable(map, next, tiles)) pending += next
            }
        }
        return false
    }

    private fun isWalkable(map: MapContent, position: MapCoordinate, tiles: Map<String, TileContent>): Boolean {
        val mapping = map.terrainMapping[map.symbolAt(position)] ?: return false
        return tiles[mapping.terrainId]?.blocksMovement == false
    }

    private fun MapContent.inBounds(position: MapCoordinate): Boolean =
        position.x in 0 until width && position.y in 0 until height

    private fun JsonObject.requiredString(
        key: String,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): String? {
        val value = this[key] as? JsonPrimitive
        val text = value?.contentOrNull?.takeIf { it.isNotBlank() }
        return text ?: errors.addAndNull(file, id, field, "Expected a non-blank string.")
    }

    private fun JsonObject.requiredInt(
        key: String,
        file: String,
        id: String,
        field: String,
        errors: MutableList<ContentValidationError>,
    ): Int? {
        val value = this[key] as? JsonPrimitive
        val number = value?.intOrNull
        return number ?: errors.addAndNull(file, id, field, "Expected an integer.")
    }

    private fun JsonObject.requiredStringArray(
        key: String,
        file: String,
        id: String,
        errors: MutableList<ContentValidationError>,
    ): List<String> {
        val array = this[key] as? JsonArray
        if (array == null) {
            errors += ContentValidationError(file, id, key, "Expected an array of row strings.")
            return emptyList()
        }
        return array.mapIndexedNotNull { index, value ->
            val text = (value as? JsonPrimitive)?.contentOrNull
            if (text == null) {
                errors += ContentValidationError(file, id, "$key[$index]", "Expected a row string.")
            }
            text
        }
    }

    private fun validateReferences(
        towers: Map<String, TowerContent>,
        enemies: Map<String, EnemyContent>,
        recipes: Map<String, RecipeContent>,
        waves: Map<String, WaveContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        towers.values.forEach { tower ->
            if (!resources.containsKey(tower.costResource)) errors += ContentValidationError("towers.properties", tower.id, "costResource", "Unknown resource '${tower.costResource}'.")
            tower.upgradeTiers.values.forEach { tier ->
                if (!resources.containsKey(tier.costResource)) errors += ContentValidationError("towers.properties", tower.id, "upgrade.${tier.branch}.${tier.tier}.costResource", "Unknown resource '${tier.costResource}'.")
            }
        }
        enemies.values.forEach { if (!resources.containsKey(it.rewardResource)) errors += ContentValidationError("enemies.properties", it.id, "rewardResource", "Unknown resource '${it.rewardResource}'.") }
        recipes.values.forEach {
            if (it.inputResource != null && !resources.containsKey(it.inputResource)) errors += ContentValidationError("recipes.properties", it.id, "inputResource", "Unknown resource '${it.inputResource}'.")
            if (!resources.containsKey(it.outputResource)) errors += ContentValidationError("recipes.properties", it.id, "outputResource", "Unknown resource '${it.outputResource}'.")
        }
        waves.values.forEach { wave ->
            wave.spawns.forEach { spawn ->
                if (!enemies.containsKey(spawn.enemyId)) errors += ContentValidationError("waves.properties", wave.id, "spawns", "Unknown enemy '${spawn.enemyId}'.")
                if (spawn.count <= 0) errors += ContentValidationError("waves.properties", wave.id, "spawns", "Spawn count must be positive.")
            }
        }
    }

    private fun validateTerminalRules(
        maps: Map<String, MapContent>,
        waves: Map<String, WaveContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        if (waves.isNotEmpty()) return
        maps.values
            .filter { it.terminalRules.winCondition == MapWinCondition.FINITE_WAVES }
            .sortedBy { it.id }
            .forEach { map ->
                errors += ContentValidationError(
                    file = "maps.json",
                    id = map.id,
                    field = "terminalRules.winCondition",
                    message = "finite_waves requires at least one declared wave; use no_win or endless for an endless map.",
                )
            }
    }

    private fun validateLocalization(
        resources: Map<String, ResourceContent>,
        towers: Map<String, TowerContent>,
        strings: Map<String, String>,
        errors: MutableList<ContentValidationError>,
    ) {
        resources.values.forEach {
            if (!strings.containsKey(it.displayKey)) {
                errors += ContentValidationError("strings.properties", it.id, "displayKey", "Missing localization key '${it.displayKey}'.")
            }
        }
        towers.values.forEach { tower ->
            if (!strings.containsKey(tower.displayKey)) {
                errors += ContentValidationError("strings.properties", tower.id, "displayKey", "Missing localization key '${tower.displayKey}'.")
            }
            tower.upgradeTiers.values.forEach { tier ->
                if (!strings.containsKey(tier.displayKey)) {
                    errors += ContentValidationError(
                        "strings.properties",
                        tower.id,
                        "upgrade.${tier.branch}.${tier.tier}.displayKey",
                        "Missing localization key '${tier.displayKey}'.",
                    )
                }
            }
        }
        HudStringKeys.required.forEach { key ->
            if (!strings.containsKey(key)) {
                errors += ContentValidationError("strings.properties", "hud", key, "Missing required HUD localization key '$key'.")
            }
        }
    }

    private fun readProperties(path: Path, errors: MutableList<ContentValidationError>): Properties? {
        if (!Files.exists(path)) {
            errors += ContentValidationError(path.fileName.toString(), "file", "path", "File is missing.")
            return null
        }
        return Properties().also { props ->
            Files.newInputStream(path).use(props::load)
        }
    }

    private fun MutableList<ContentValidationError>.addAndNull(file: String, id: String, field: String, message: String): Nothing? {
        add(ContentValidationError(file, id, field, message))
        return null
    }

    private fun String.splitCsv(): List<String> =
        split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun Map<String, String>.required(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): String? =
        this[field]?.takeIf { it.isNotBlank() } ?: errors.addAndNull(file, id, field, "Required field is missing.")

    private fun Map<String, String>.requiredBool(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Boolean? =
        required(file, id, field, errors)?.toBooleanStrictOrNull() ?: errors.addAndNull(file, id, field, "Expected boolean.")

    private fun Map<String, String>.requiredPositiveInt(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Int? {
        val value = required(file, id, field, errors)?.toIntOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value > 0) value else errors.addAndNull(file, id, field, "Expected positive integer.")
    }

    private fun Map<String, String>.requiredNonNegativeInt(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Int? {
        val value = required(file, id, field, errors)?.toIntOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value >= 0) value else errors.addAndNull(file, id, field, "Expected non-negative integer.")
    }

    private fun Map<String, String>.requiredNonNegativeLong(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): Long? {
        val value = required(file, id, field, errors)?.toLongOrNull() ?: return errors.addAndNull(file, id, field, "Expected integer.")
        return if (value >= 0) value else errors.addAndNull(file, id, field, "Expected non-negative integer.")
    }

    private fun Map<String, String>.requiredPositiveDecimal(file: String, id: String, field: String, errors: MutableList<ContentValidationError>): BigDecimal? {
        val raw = required(file, id, field, errors) ?: return null
        val value = try {
            BigDecimal(raw.trim())
        } catch (_: NumberFormatException) {
            return errors.addAndNull(file, id, field, "Expected decimal multiplier.")
        }
        return if (value.signum() > 0) value else errors.addAndNull(file, id, field, "Expected positive decimal multiplier.")
    }
}
