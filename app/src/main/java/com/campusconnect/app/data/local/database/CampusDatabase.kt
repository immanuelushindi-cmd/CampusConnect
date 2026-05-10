package com.campusconnect.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campusconnect.app.data.local.dao.EventDao
import com.campusconnect.app.data.local.dao.NoticeDao
import com.campusconnect.app.data.local.dao.TimetableDao
import com.campusconnect.app.data.local.entity.EventEntity
import com.campusconnect.app.data.local.entity.NoticeEntity
import com.campusconnect.app.data.local.entity.TimetableEntity

@Database(
    entities = [NoticeEntity::class, TimetableEntity::class, EventEntity::class],
    version = 3,           // FIX: bumped from 2 → 3 to add targeting columns
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun noticeDao(): NoticeDao
    abstract fun timetableDao(): TimetableDao
    abstract fun eventDao(): EventDao

    companion object {
        const val DATABASE_NAME = "campus_connect_v2.db"

        // ── Migration 1 → 2 ───────────────────────────────────────────────────
        // notices : timestampMs  → createdAt
        // events  : hasRsvp      → isAttending  /  endTimestampMs + category removed
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE notices_new (
                        id             TEXT    NOT NULL PRIMARY KEY,
                        title          TEXT    NOT NULL DEFAULT '',
                        content        TEXT    NOT NULL DEFAULT '',
                        category       TEXT    NOT NULL DEFAULT 'GENERAL',
                        priority       TEXT    NOT NULL DEFAULT 'MEDIUM',
                        author         TEXT    NOT NULL DEFAULT '',
                        authorEmail    TEXT    NOT NULL DEFAULT '',
                        authorPhotoUrl TEXT,
                        createdAt      INTEGER NOT NULL DEFAULT 0,
                        imageUrl       TEXT,
                        tags           TEXT    NOT NULL DEFAULT '',
                        isFavorite     INTEGER NOT NULL DEFAULT 0,
                        viewCount      INTEGER NOT NULL DEFAULT 0,
                        isActive       INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO notices_new
                    SELECT id, title, content, category, priority, author, authorEmail,
                           authorPhotoUrl, timestampMs, imageUrl, tags,
                           isFavorite, viewCount, isActive
                    FROM notices
                """.trimIndent())
                db.execSQL("DROP TABLE notices")
                db.execSQL("ALTER TABLE notices_new RENAME TO notices")

                db.execSQL("""
                    CREATE TABLE events_new (
                        id               TEXT    NOT NULL PRIMARY KEY,
                        title            TEXT    NOT NULL DEFAULT '',
                        description      TEXT    NOT NULL DEFAULT '',
                        location         TEXT    NOT NULL DEFAULT '',
                        latitude         REAL,
                        longitude        REAL,
                        startTimestampMs INTEGER NOT NULL DEFAULT 0,
                        organizer        TEXT    NOT NULL DEFAULT '',
                        imageUrl         TEXT,
                        maxAttendees     INTEGER NOT NULL DEFAULT 0,
                        rsvpCount        INTEGER NOT NULL DEFAULT 0,
                        isAttending      INTEGER NOT NULL DEFAULT 0,
                        isOnline         INTEGER NOT NULL DEFAULT 0,
                        meetLink         TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO events_new
                    SELECT id, title, description, location, latitude, longitude,
                           startTimestampMs, organizer, imageUrl,
                           maxAttendees, rsvpCount, hasRsvp, isOnline, meetLink
                    FROM events
                """.trimIndent())
                db.execSQL("DROP TABLE events")
                db.execSQL("ALTER TABLE events_new RENAME TO events")
            }
        }

        // ── Migration 2 → 3 ───────────────────────────────────────────────────
        // FIX: Add targetDepartment and targetCourse columns to notices and events.
        // These columns were present in domain models and the Firestore mapper but
        // missing from the Room entities, meaning targeting was silently lost on
        // any cached data. SQLite ADD COLUMN is safe here (nullable columns with
        // no DEFAULT constraint are stored as NULL for existing rows, which is
        // exactly the right default — no targeting restriction).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // notices
                db.execSQL("ALTER TABLE notices ADD COLUMN targetDepartment TEXT")
                db.execSQL("ALTER TABLE notices ADD COLUMN targetCourse TEXT")
                // events
                db.execSQL("ALTER TABLE events ADD COLUMN targetDepartment TEXT")
                db.execSQL("ALTER TABLE events ADD COLUMN targetCourse TEXT")
                // timetable — targetDepartment was in the entity but never in the
                // physical table; semester column also missing on older installs
                db.execSQL("ALTER TABLE timetable ADD COLUMN targetDepartment TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE timetable ADD COLUMN semester TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}