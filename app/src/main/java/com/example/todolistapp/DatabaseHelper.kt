package com.example.todolistapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "tasks.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_COMPLETED = "completed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT NOT NULL, " +
                "$COLUMN_COMPLETED INTEGER NOT NULL)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addTask(task: Task): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, task.name)
            put(COLUMN_COMPLETED, if (task.isCompleted) 1 else 0)
        }

        val newTaskId = db.insert(TABLE_NAME, null, values)
        db.close()
        return newTaskId
    }

    fun getAllTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val nameIndex = cursor.getColumnIndex(COLUMN_NAME)
                val completedIndex = cursor.getColumnIndex(COLUMN_COMPLETED)

                if (idIndex >= 0 && nameIndex >= 0 && completedIndex >= 0) {
                    val id = cursor.getInt(idIndex)
                    val name = cursor.getString(nameIndex)
                    val isCompleted = cursor.getInt(completedIndex) == 1

                    tasks.add(Task(id, name, isCompleted))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return tasks
    }

    fun updateTask(task: Task) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, task.name)
            put(COLUMN_COMPLETED, if (task.isCompleted) 1 else 0)
        }

        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
        db.close()
    }


    fun deleteTask(taskId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
        db.close()
    }
}
