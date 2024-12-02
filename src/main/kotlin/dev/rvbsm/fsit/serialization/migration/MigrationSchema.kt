package dev.rvbsm.fsit.serialization.migration

typealias Transformer = (String) -> String

data class MigrationSchema(val version: Int, val migrations: Collection<Migration>) {
    data class Migration(val origins: Set<Key>, val destination: Key) {
        data class Key(val path: String, val modifier: Transformer = { it }) {
            override fun hashCode() = path.hashCode()
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Key

                return path == other.path
            }
        }
    }
}

infix fun Iterable<MigrationSchema.Migration>.version(version: Int) = MigrationSchema(version, toSet())

infix fun Iterable<MigrationSchema.Migration.Key>.migrateTo(destination: MigrationSchema.Migration.Key) =
    MigrationSchema.Migration(toSet(), destination)

infix fun MigrationSchema.Migration.Key.migrateTo(destination: MigrationSchema.Migration.Key) =
    setOf(this) migrateTo destination

infix fun Iterable<MigrationSchema.Migration.Key>.migrateTo(destination: String) =
    this migrateTo destination.migrationKey()

infix fun MigrationSchema.Migration.Key.migrateTo(destination: String) = this migrateTo destination.migrationKey()
infix fun String.migrateTo(destination: String) = migrationKey() migrateTo destination

fun String.migrationKey(modifier: (String) -> String = { it }) = MigrationSchema.Migration.Key(this, modifier)
