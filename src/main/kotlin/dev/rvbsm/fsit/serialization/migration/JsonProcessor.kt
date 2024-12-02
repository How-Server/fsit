package dev.rvbsm.fsit.serialization.migration

import dev.rvbsm.fsit.serialization.preferContextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf

class JsonProcessor(override val target: JsonObject) : MigrationProcessor<JsonElement> {
    override fun JsonElement.toMutable(): MutableNode = when (this) {
        is JsonNull -> MutableNodeNull
        is JsonPrimitive -> MutableNodeLiteral(content, isString)
        is JsonArray -> MutableNodeList(mapTo(mutableListOf()) { it.toMutable() })
        is JsonObject -> MutableNodeMap(mapValuesTo(mutableMapOf()) { (_, element) -> element.toMutable() })
    }

    override fun MutableNode.toImmutable(): JsonElement = when (this) {
        is MutableNodeNull -> JsonNull
        is MutableNodeLiteral -> {
            if (isString) JsonPrimitive(content)
            else if (content.toBooleanStrictOrNull() != null) JsonPrimitive(content.toBooleanStrict())
            else if (content.toLongOrNull() != null) JsonPrimitive(content.toLong())
            else if (content.toDoubleOrNull() != null) JsonPrimitive(content.toDouble())
            else JsonPrimitive(content)
        }

        is MutableNodeList -> JsonArray(map { it.toImmutable() })
        is MutableNodeMap -> JsonObject(mapValues { (_, node) -> node.toImmutable() })
    }
}

class JsonMigrationSerializer<T : Any>(
    tSerializer: KSerializer<T>,
    schemas: Iterable<MigrationSchema>,
    val parseVersion: JsonObject.() -> Int,
) : JsonTransformingSerializer<T>(tSerializer) {

    private val sortedSchemas = schemas.sortedByVersion()

    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        !is JsonObject -> element
        else -> JsonProcessor(element) + sortedSchemas.forVersion(element.parseVersion())
    }
}

val JsonObject.version get() = get("version")?.runCatching { jsonPrimitive.intOrNull }?.getOrNull() ?: 0

inline fun <reified T : Any> JsonBuilder.withMigration(
    schemas: Iterable<MigrationSchema>,
    noinline versionProvider: JsonObject.() -> Int = JsonObject::version,
) {
    serializersModule = serializersModule.overwriteWith(
        serializersModuleOf(JsonMigrationSerializer(serializersModule.preferContextual<T>(), schemas, versionProvider))
    )
}
