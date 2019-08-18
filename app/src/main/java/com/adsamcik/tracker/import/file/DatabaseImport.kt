package com.adsamcik.tracker.import.file

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adsamcik.tracker.common.database.AppDatabase
import com.adsamcik.tracker.common.exception.NotFoundException
import com.adsamcik.tracker.common.extension.sortByVertexes
import com.adsamcik.tracker.common.graph.Edge
import com.adsamcik.tracker.common.graph.Graph
import com.adsamcik.tracker.common.graph.Vertex
import com.adsamcik.tracker.common.graph.topSort
import io.requery.android.database.sqlite.SQLiteDatabase
import java.io.File

//todo UNIQUE constraint can fail the import -> using autoincrement could break foreign keys
class DatabaseImport : FileImport {
	override val supportedExtensions: Collection<String> = listOf("db")

	override fun import(database: AppDatabase, file: File) {
		val fromDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
		importDatabase(fromDatabase, database.openHelper.writableDatabase)
	}

	private fun addColumn(columnDefinition: String, requiredColumns: MutableList<ImportColumn>) {
		val isNotNull = columnDefinition.contains("NOT NULL")
		val columnName = columnDefinition.substringAfter('`').substringBefore('`')
		val isUnique = columnDefinition.contains("PRIMARY KEY") ||
				columnDefinition.contains("UNIQUE")
		val column = ImportColumn(columnName, isNotNull, isUnique, null)
		requiredColumns.add(column)
	}

	private fun addForeignKey(columnDefinition: String, requiredColumns: MutableList<ImportColumn>) {
		val thisTableColumn = columnDefinition
				.substringAfter("FOREIGN KEY(`")
				.substringBefore("`")

		val targetTableName = columnDefinition
				.substringAfter("REFERENCES `")
				.substringBefore("`")

		val column = requiredColumns.find { it.columnName == thisTableColumn } ?: throw NotFoundException(
				"Expected column with name $thisTableColumn but had only ${requiredColumns.joinToString()}")

		column.foreignKeyTable = ImportTable(targetTableName, isImported = false, columns = emptyList())
	}

	private fun addIfColumnIsRequired(sql: String, requiredColumns: MutableList<ImportColumn>) {
		sql.substringAfter('(').split(',').forEach { split ->
			val columnDefinition = split.trim()
			if (columnDefinition.startsWith('`')) {
				addColumn(columnDefinition, requiredColumns)
			} else if (columnDefinition.startsWith("FOREIGN KEY")) {
				addForeignKey(columnDefinition, requiredColumns)
			}
		}
	}

	private fun SupportSQLiteDatabase.getColumns(tableName: String): List<ImportColumn> {
		query("SELECT name, sql FROM sqlite_master WHERE type='table' and name == ? ORDER BY name",
				arrayOf(tableName)).use {
			return if (it.moveToNext()) {
				val requiredColumns = mutableListOf<ImportColumn>()
				val sql = it.getString(1)

				addIfColumnIsRequired(sql, requiredColumns)

				requiredColumns
			} else {
				throw NotFoundException("Could not find table with name $tableName")
			}
		}
	}

	private fun SupportSQLiteDatabase.getAllTables(): List<String> {
		query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use {
			val list = mutableListOf<String>()
			while (it.moveToNext()) {
				list.add(it.getString(0))
			}

			return list
		}
	}

	private fun getMatchingColumns(fromDatabase: SupportSQLiteDatabase,
	                               toDatabase: SupportSQLiteDatabase,
	                               table: String): List<ImportColumn> {
		val fromColumns = fromDatabase.getColumns(table)
		val toColumns = toDatabase.getColumns(table)
		return fromColumns.toList().filter { toColumns.contains(it) }
	}

	private fun getMatchingTables(fromDatabase: SupportSQLiteDatabase,
	                              toDatabase: SupportSQLiteDatabase): List<String> {
		val fromTables = fromDatabase.getAllTables()
		val toTables = toDatabase.getAllTables()
		return fromTables.toList().filter { toTables.contains(it) }
	}

	private fun importRow(to: SupportSQLiteDatabase, row: Cursor, columnsJoined: String, tableName: String) {
		val values = mutableListOf<String>()

		for (i in 0 until row.columnCount) {
			values.add(row.getString(i))
		}

		val valuesQuery = values.joinToString(separator = ",", transform = { "'$it'" })
		to.execSQL("INSERT INTO $tableName ($columnsJoined) VALUES ($valuesQuery)")
	}

	private fun importTable(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase, tableName: String) {
		val matchingColumns = getMatchingColumns(from, to, tableName)

		from.query("SELECT ${matchingColumns.joinToString(separator = ",")} FROM $tableName").use {
			val columnsJoined = it.columnNames.joinToString(separator = ",")
			while (it.moveToNext()) {
				importRow(to, it, columnsJoined, tableName)
			}
		}

	}

	private fun List<Pair<ImportTable, ImportTable>>.sortByTopology(): List<Pair<ImportTable, ImportTable>> {
		val vertexList = MutableList(size) { Vertex(it) }
		val edgeList = map { pair ->
			pair.first.columns
					.mapNotNull { column -> column.foreignKeyTable }
					.map { table -> indexOfFirst { it.first == table } }
		}
				.withIndex()
				.flatMap { indexedList ->
					indexedList.value.map { Edge(Vertex(indexedList.index), Vertex(it)) }
				}

		val topSorted = Graph(vertexList, edgeList).topSort()

		return sortByVertexes(topSorted)
	}

	private fun importDatabase(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase) {
		getMatchingTables(from, to)
				.map { tableName ->
					val fromColumns = from.getColumns(tableName)
					val toColumns = to.getColumns(tableName)

					ImportTable(tableName, false, fromColumns) to ImportTable(tableName, false, toColumns)
				}
				.sortByTopology()
				.forEach { pair ->
					val hasRequiredColumns =
							pair.first.columns.all {
								if (it.isNotNull) {
									pair.second.columns.contains(it)
								} else {
									true
								}
							}

					if (hasRequiredColumns) {
						importTable(from, to, pair.first.tableName)
					}
				}
	}
}

internal data class ImportTable(
		val tableName: String,
		var isImported: Boolean,
		val columns: List<ImportColumn>
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ImportTable

		if (tableName != other.tableName) return false

		return true
	}

	override fun hashCode(): Int {
		return tableName.hashCode()
	}
}

internal data class ImportColumn(
		val columnName: String,
		val isNotNull: Boolean,
		val isUnique: Boolean,
		var foreignKeyTable: ImportTable? = null
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ImportColumn

		if (columnName != other.columnName) return false

		return true
	}

	override fun hashCode(): Int {
		return columnName.hashCode()
	}
}
