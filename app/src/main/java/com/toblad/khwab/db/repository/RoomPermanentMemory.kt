package com.toblad.khwab.db.repository

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.model.MemoryAudit
import com.toblad.khwab.core.memory.model.MemoryCategory
import com.toblad.khwab.core.memory.model.MemoryConfidence
import com.toblad.khwab.core.memory.model.MemoryId
import com.toblad.khwab.core.memory.model.MemoryMetadata
import com.toblad.khwab.core.memory.model.MemoryOperation
import com.toblad.khwab.core.memory.model.MemorySource
import com.toblad.khwab.core.memory.model.MemoryType
import com.toblad.khwab.core.memory.model.MemoryVersion
import com.toblad.khwab.core.memory.permanent.PermanentMemory
import com.toblad.khwab.db.dao.PermanentMemoryDao
import com.toblad.khwab.db.entity.PermanentMemoryAuditEntity
import com.toblad.khwab.db.entity.PermanentMemoryEntity
import com.toblad.khwab.db.entity.PermanentMemoryVersionEntity
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Room-backed implementation of [PermanentMemory].
 *
 * Mirrors the behaviour of [com.toblad.khwab.core.memory.permanent.internal.RamPermanentMemory]
 * with full version history and audit trail, backed by SQLite via Room.
 *
 * Note: [PermanentMemory] is a synchronous interface used from core.
 * Room suspend functions are called via [runBlocking] here. This is safe
 * because [PermanentMemory] operations are always invoked from a coroutine
 * context in [com.toblad.khwab.core.execution.CoreStepExecutor] via
 * the integration layer. A future refactor may make the interface suspend.
 */
class RoomPermanentMemory(
    private val dao: PermanentMemoryDao
) : PermanentMemory {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    override fun create(memory: Memory): Memory = runBlocking {
        val existing = dao.findBySubject(memory.subject)
        if (existing != null) {
            // Subject already known — update
            val updated = memory.copy(
                id = MemoryId(existing.id),
                value = memory.value
            )
            update(updated)
        } else {
            val now = System.currentTimeMillis()
            val entity = memory.toEntity()
            dao.insert(entity)
            dao.insertVersion(
                PermanentMemoryVersionEntity(
                    id = UUID.randomUUID().toString(),
                    memoryId = entity.id,
                    version = 1,
                    value = memory.value,
                    updatedAt = now
                )
            )
            dao.insertAudit(
                PermanentMemoryAuditEntity(
                    id = UUID.randomUUID().toString(),
                    memoryId = entity.id,
                    operation = MemoryOperation.CREATE.name,
                    timestamp = now,
                    source = memory.metadata.source.name
                )
            )
            memory.withVersionsAndAudit(
                versions = listOf(MemoryVersion(1, memory.value, now)),
                audit = listOf(MemoryAudit(MemoryOperation.CREATE, now, memory.metadata.source))
            )
        }
    }

    override fun read(id: MemoryId): Memory? = runBlocking {
        dao.findById(id.value)?.toDomain(dao)
    }

    override fun update(memory: Memory): Memory = runBlocking {
        val existing = dao.findById(memory.id.value)
            ?: run {
                dao.insert(memory.toEntity())
                return@runBlocking memory
            }
        val now = System.currentTimeMillis()
        val nextVersion = existing.currentVersion + 1
        val updated = existing.copy(
            value = memory.value,
            updatedAt = now,
            currentVersion = nextVersion
        )
        dao.update(updated)
        dao.insertVersion(
            PermanentMemoryVersionEntity(
                id = UUID.randomUUID().toString(),
                memoryId = existing.id,
                version = nextVersion,
                value = memory.value,
                updatedAt = now
            )
        )
        dao.insertAudit(
            PermanentMemoryAuditEntity(
                id = UUID.randomUUID().toString(),
                memoryId = existing.id,
                operation = MemoryOperation.UPDATE.name,
                timestamp = now,
                source = memory.metadata.source.name
            )
        )
        updated.toDomain(dao)
    }

    override fun delete(id: MemoryId): Boolean = runBlocking {
        val existing = dao.findById(id.value) ?: return@runBlocking false
        val now = System.currentTimeMillis()
        dao.update(existing.copy(isDeleted = true, deletedAt = now, updatedAt = now))
        dao.insertAudit(
            PermanentMemoryAuditEntity(
                id = UUID.randomUUID().toString(),
                memoryId = existing.id,
                operation = MemoryOperation.DELETE.name,
                timestamp = now,
                source = MemorySource.USER.name
            )
        )
        true
    }

    override fun restore(id: MemoryId): Boolean = runBlocking {
        val existing = dao.findById(id.value) ?: return@runBlocking false
        val now = System.currentTimeMillis()
        dao.update(existing.copy(isDeleted = false, deletedAt = null, updatedAt = now))
        dao.insertAudit(
            PermanentMemoryAuditEntity(
                id = UUID.randomUUID().toString(),
                memoryId = existing.id,
                operation = MemoryOperation.RESTORE.name,
                timestamp = now,
                source = MemorySource.USER.name
            )
        )
        true
    }

    override fun history(id: MemoryId): List<MemoryVersion> = runBlocking {
        dao.findVersions(id.value).map { MemoryVersion(it.version, it.value, it.updatedAt) }
    }

    override fun audit(id: MemoryId): List<MemoryAudit> = runBlocking {
        dao.findAudit(id.value).map {
            MemoryAudit(
                operation = MemoryOperation.valueOf(it.operation),
                timestamp = it.timestamp,
                source = MemorySource.valueOf(it.source)
            )
        }
    }

    override fun explain(id: MemoryId): String {
        val m = read(id) ?: return "No memory found."
        return buildString {
            append("Subject: ${m.subject}\n")
            append("Current value: ${m.value}\n")
            append("Version: ${m.metadata.currentVersion}\n")
            m.versions.forEach { v ->
                append("  v${v.version}: ${v.value} (${dateFmt.format(Date(v.updatedAt))})\n")
            }
        }
    }

    override fun findDuplicate(subject: String): Memory? = runBlocking {
        dao.findBySubject(subject)?.toDomain(dao)
    }

    override fun search(query: String): List<Memory> = runBlocking {
        dao.search(query).map { it.toDomain(dao) }
    }

    override fun all(): List<Memory> = runBlocking {
        dao.findAll().map { it.toDomain(dao) }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun Memory.toEntity(): PermanentMemoryEntity {
        val now = System.currentTimeMillis()
        return PermanentMemoryEntity(
            id = id.value,
            subject = subject,
            value = value,
            category = metadata.category.name,
            confidence = metadata.confidence.name,
            source = metadata.source.name,
            createdAt = metadata.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = metadata.updatedAt.takeIf { it > 0 } ?: now,
            currentVersion = metadata.currentVersion,
            isDeleted = metadata.isDeleted,
            deletedAt = metadata.deletedAt
        )
    }

    private suspend fun PermanentMemoryEntity.toDomain(dao: PermanentMemoryDao): Memory {
        val versions = dao.findVersions(id).map { MemoryVersion(it.version, it.value, it.updatedAt) }
        val audit = dao.findAudit(id).map {
            MemoryAudit(
                operation = MemoryOperation.valueOf(it.operation),
                timestamp = it.timestamp,
                source = MemorySource.valueOf(it.source)
            )
        }
        return Memory(
            id = MemoryId(id),
            subject = subject,
            value = value,
            type = MemoryType.PERMANENT,
            metadata = MemoryMetadata(
                category = runCatching { MemoryCategory.valueOf(category) }.getOrDefault(MemoryCategory.OTHER),
                confidence = runCatching { MemoryConfidence.valueOf(confidence) }.getOrDefault(MemoryConfidence.EXPLICIT),
                source = runCatching { MemorySource.valueOf(source) }.getOrDefault(MemorySource.USER),
                createdAt = createdAt,
                updatedAt = updatedAt,
                currentVersion = currentVersion,
                isDeleted = isDeleted,
                deletedAt = deletedAt
            ),
            versions = versions,
            auditTrail = audit
        )
    }

    private fun Memory.withVersionsAndAudit(
        versions: List<MemoryVersion>,
        audit: List<MemoryAudit>
    ): Memory = copy(versions = versions, auditTrail = audit)
}
