package dev.rvbsm.fsit.config.serialization

import com.charleskorn.kaml.AmbiguousQuoteStyle
import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.SequenceStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> YamlBuilder.withDefault(noinline defaultProvider: () -> T) {
    serializersModule = serializersModule.overwriteWith(
        serializersModuleOf(
            DefaultedSerializer(
                serializersModule.getContextual(T::class) ?: serializersModule.serializer<T>(),
                defaultProvider,
            )
        )
    )
}

fun Yaml(from: Yaml = Yaml.default, builderAction: YamlBuilder.() -> Unit): Yaml {
    val builder = YamlBuilder(from)
    builder.builderAction()
    val conf = builder.build()
    return Yaml(builder.serializersModule, conf)
}

class YamlBuilder(yaml: Yaml) {
    private var configuration = yaml.configuration

    var encodeDefaults: Boolean = true
        set(value) {
            configuration = configuration.copy(encodeDefaults = value)
            field = value
        }
    var strictMode: Boolean = true
        set(value) {
            configuration = configuration.copy(strictMode = value)
            field = value
        }
    var extensionDefinitionPrefix: String? = null
        set(value) {
            configuration = configuration.copy(extensionDefinitionPrefix = value)
            field = value
        }
    var polymorphismStyle: PolymorphismStyle = PolymorphismStyle.Tag
        set(value) {
            configuration = configuration.copy(polymorphismStyle = value)
            field = value
        }
    var polymorphismPropertyName: String = "type"
        set(value) {
            configuration = configuration.copy(polymorphismPropertyName = value)
            field = value
        }
    var encodingIndentationSize: Int = 2
        set(value) {
            configuration = configuration.copy(encodingIndentationSize = value)
            field = value
        }
    var breakScalarsAt: Int = 80
        set(value) {
            configuration = configuration.copy(breakScalarsAt = value)
            field = value
        }
    var sequenceStyle: SequenceStyle = SequenceStyle.Block
        set(value) {
            configuration = configuration.copy(sequenceStyle = value)
            field = value
        }
    var singleLineStringStyle: SingleLineStringStyle = SingleLineStringStyle.DoubleQuoted
        set(value) {
            configuration = configuration.copy(singleLineStringStyle = value)
            field = value
        }
    var multiLineStringStyle: MultiLineStringStyle = singleLineStringStyle.multiLineStringStyle
        set(value) {
            configuration = configuration.copy(multiLineStringStyle = value)
            field = value
        }
    var ambiguousQuoteStyle: AmbiguousQuoteStyle = AmbiguousQuoteStyle.DoubleQuoted
        set(value) {
            configuration = configuration.copy(ambiguousQuoteStyle = value)
            field = value
        }
    var sequenceBlockIndent: Int = 0
        set(value) {
            configuration = configuration.copy(sequenceBlockIndent = value)
            field = value
        }
    var allowAnchorsAndAliases: Boolean = false
        set(value) {
            configuration = configuration.copy(allowAnchorsAndAliases = value)
            field = value
        }
    var yamlNamingStrategy: YamlNamingStrategy? = null
        set(value) {
            configuration = configuration.copy(yamlNamingStrategy = value)
            field = value
        }
    var codePointLimit: Int? = null
        set(value) {
            configuration = configuration.copy(codePointLimit = value)
            field = value
        }

    @ExperimentalSerializationApi
    var decodeEnumCaseInsensitive: Boolean = false
        set(value) {
            configuration = configuration.copy(decodeEnumCaseInsensitive = value)
            field = value
        }

    var serializersModule: SerializersModule = yaml.serializersModule

    fun build(): YamlConfiguration {
        return configuration
    }
}
