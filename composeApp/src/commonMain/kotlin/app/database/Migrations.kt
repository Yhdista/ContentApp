package app.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use

/** ♻️ Room owns the migration transaction. No destructive fallback; v6 stays intact on failure. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // v6 has no original prescription: the new table remains empty for migrated sessions.
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `workout_prescriptions` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `workoutExerciseId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `restSeconds` INTEGER NOT NULL, `exerciseOrder` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `setType` TEXT NOT NULL, `sourceRoutineExerciseId` TEXT NOT NULL, `sourceRoutineSetId` TEXT, PRIMARY KEY(`id`))""")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_prescriptions_sessionId` ON `workout_prescriptions` (`sessionId`)")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `restSeconds` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `orderReconstructed` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `routines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `revision` INTEGER NOT NULL, `archived` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `routine_exercises` (`id` TEXT NOT NULL, `routineId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `restSeconds` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `legacyExerciseName` TEXT, `legacyTargetSets` INTEGER, `legacyDefaultWeightKg` REAL, `legacyDefaultReps` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        connection.execSQL("""CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` ON `routine_exercises` (`routineId`)""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `routine_sets` (`id` TEXT NOT NULL, `routineExerciseId` TEXT NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `setType` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineExerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        connection.execSQL("""CREATE INDEX IF NOT EXISTS `index_routine_sets_routineExerciseId` ON `routine_sets` (`routineExerciseId`)""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `editor_drafts` (`kind` TEXT NOT NULL, `id` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`kind`, `id`))""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `training_plans` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `revision` INTEGER NOT NULL, `archived` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `training_plan_slots` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `label` TEXT NOT NULL, `routineId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `training_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        connection.execSQL("""CREATE INDEX IF NOT EXISTS `index_training_plan_slots_planId` ON `training_plan_slots` (`planId`)""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `training_plan_progress` (`planId` TEXT NOT NULL, `nextSlotId` TEXT, `progressRevision` INTEGER NOT NULL, `needsReview` INTEGER NOT NULL, PRIMARY KEY(`planId`), FOREIGN KEY(`planId`) REFERENCES `training_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        connection.execSQL("""CREATE TABLE IF NOT EXISTS `training_plan_selection` (`id` INTEGER NOT NULL, `planId` TEXT, PRIMARY KEY(`id`))""")
        connection.execSQL("ALTER TABLE exercises ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        listOf("sourceRoutineId TEXT", "sourceRoutineRevision INTEGER", "sourcePlanId TEXT", "sourcePlanRevision INTEGER",
            "sourceSlotId TEXT", "sourceProgressRevision INTEGER").forEach { connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN $it") }
        listOf("workoutExerciseId TEXT", "prescribedWeightKg REAL", "prescribedReps INTEGER", "prescribedSetType TEXT")
            .forEach { connection.execSQL("ALTER TABLE workout_sets ADD COLUMN $it") }
        connection.execSQL("""INSERT INTO exercises(id,name,muscleGroup,defaultRestSeconds,archived)
            SELECT t.exerciseId, (SELECT x.exerciseName FROM workout_template_exercises x WHERE x.exerciseId=t.exerciseId ORDER BY x.templateId,x.orderIndex LIMIT 1), 'CHEST',90,1
            FROM workout_template_exercises t WHERE NOT EXISTS (SELECT 1 FROM exercises e WHERE e.id=t.exerciseId) GROUP BY t.exerciseId""")
        connection.execSQL("""INSERT INTO exercises(id,name,muscleGroup,defaultRestSeconds,archived)
            SELECT DISTINCT s.exerciseId,'Neznámý cvik','CHEST',90,1 FROM workout_sets s WHERE NOT EXISTS (SELECT 1 FROM exercises e WHERE e.id=s.exerciseId)""")
        connection.execSQL("INSERT INTO routines(id,name,notes,revision,archived) SELECT id,name,description,1,0 FROM workout_templates")
        // Preserve even legacy children whose parent template is missing.
        connection.execSQL("""INSERT INTO routines(id,name,notes,revision,archived)
            SELECT DISTINCT templateId,'Obnovená rutina','',1,0 FROM workout_template_exercises t
            WHERE NOT EXISTS (SELECT 1 FROM routines r WHERE r.id=t.templateId)""")
        connection.prepare("""SELECT t.templateId,t.exerciseId,t.exerciseName,t.targetSets,t.defaultWeightKg,t.defaultReps,t.orderIndex,e.defaultRestSeconds
            FROM workout_template_exercises t JOIN exercises e ON e.id=t.exerciseId ORDER BY t.templateId,t.orderIndex,t.exerciseId""").use { row ->
            while(row.step()) {
                val templateId=row.getText(0); val exerciseId=row.getText(1); val order=row.getLong(6)
                val id="legacy-routine:${templateId.length}:$templateId:${exerciseId.length}:$exerciseId:$order"
                connection.prepare("INSERT INTO routine_exercises(id,routineId,exerciseId,restSeconds,orderIndex,legacyExerciseName,legacyTargetSets,legacyDefaultWeightKg,legacyDefaultReps) VALUES (?,?,?,?,?,?,?,?,?)").use {
                    it.bindText(1,id); it.bindText(2,templateId); it.bindText(3,exerciseId); it.bindLong(4,row.getLong(7)); it.bindLong(5,order)
                    it.bindText(6,row.getText(2)); it.bindLong(7,row.getLong(3)); it.bindDouble(8,row.getDouble(4)); it.bindLong(9,row.getLong(5)); it.step()
                }
                // Do not truncate legacy data to new validation limits. Invalid routines need repair before start.
                for(index in 0 until row.getLong(3)) {
                    connection.prepare("INSERT INTO routine_sets(id,routineExerciseId,weightKg,reps,setType,orderIndex) VALUES (?,?,?,?,'NORMAL',?)").use {
                        it.bindText(1,"$id:$index"); it.bindText(2,id); it.bindDouble(3,row.getDouble(4)); it.bindLong(4,row.getLong(5)); it.bindLong(5,index); it.step()
                    }
                }
            }
        }
        // v6 has no entry identity/order: deterministically group by session and catalogue ID and mark reconstruction.
        var previousSession: String?=null; var order=0L
        connection.prepare("""SELECT DISTINCT s.sessionId,s.exerciseId,e.name,e.muscleGroup,e.defaultRestSeconds FROM workout_sets s
            JOIN exercises e ON e.id=s.exerciseId ORDER BY s.sessionId,s.exerciseId""").use { row ->
            while(row.step()) {
                val sessionId=row.getText(0); val exerciseId=row.getText(1)
                if(previousSession!=sessionId) { previousSession=sessionId; order=0 }
                val id="legacy-workout:${sessionId.length}:$sessionId:${exerciseId.length}:$exerciseId"
                connection.prepare("INSERT INTO workout_exercises(id,sessionId,exerciseId,name,muscleGroup,restSeconds,orderIndex,orderReconstructed) VALUES (?,?,?,?,?,?,?,1)").use {
                    it.bindText(1,id); it.bindText(2,sessionId); it.bindText(3,exerciseId); it.bindText(4,row.getText(2)); it.bindText(5,row.getText(3)); it.bindLong(6,row.getLong(4)); it.bindLong(7,order++); it.step()
                }
                connection.prepare("UPDATE workout_sets SET workoutExerciseId=? WHERE sessionId=? AND exerciseId=?").use {
                    it.bindText(1,id); it.bindText(2,sessionId); it.bindText(3,exerciseId); it.step()
                }
            }
        }
        // No write to sync_mutations: preserve IDs, operation names and original JSON bytes exactly.
        connection.execSQL("DROP TABLE workout_template_exercises")
        connection.execSQL("DROP TABLE workout_templates")
    }
}
