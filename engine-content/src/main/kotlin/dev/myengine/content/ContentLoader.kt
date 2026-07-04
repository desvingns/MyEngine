package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
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
        val strings = readProperties(root.resolve("strings.properties"), errors)?.entries
            ?.associate { it.key.toString() to it.value.toString() }
            ?: emptyMap()

        if (manifest != null && manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors += ContentValidationError("manifest.properties", manifest.id, "schemaVersion", "Unsupported schema version ${manifest.schemaVersion}.")
        }

        validateReferences(towers, enemies, recipes, waves, resources, errors)
        validateLocalization(resources, strings, errors)

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
            range = fields.requiredPositiveInt(file, id, "range", errors) ?: return null,
            damage = fields.requiredPositiveInt(file, id, "damage", errors) ?: return null,
            cooldownTicks = fields.requiredPositiveInt(file, id, "cooldownTicks", errors) ?: return null,
            costResource = fields.required(file, id, "costResource", errors) ?: return null,
            costAmount = fields.requiredNonNegativeInt(file, id, "costAmount", errors) ?: return null,
        )

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

    private fun validateReferences(
        towers: Map<String, TowerContent>,
        enemies: Map<String, EnemyContent>,
        recipes: Map<String, RecipeContent>,
        waves: Map<String, WaveContent>,
        resources: Map<String, ResourceContent>,
        errors: MutableList<ContentValidationError>,
    ) {
        towers.values.forEach { if (!resources.containsKey(it.costResource)) errors += ContentValidationError("towers.properties", it.id, "costResource", "Unknown resource '${it.costResource}'.") }
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

    private fun validateLocalization(resources: Map<String, ResourceContent>, strings: Map<String, String>, errors: MutableList<ContentValidationError>) {
        resources.values.forEach {
            if (!strings.containsKey(it.displayKey)) {
                errors += ContentValidationError("strings.properties", it.id, "displayKey", "Missing localization key '${it.displayKey}'.")
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
}
