package com.adsamcik.signalcollector.import

import android.content.Context
import android.os.Build
import com.adsamcik.signalcollector.common.database.AppDatabase
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser

class DataImport {
	private val registeredImports: Collection<IImport>

	val supportedExtensions: Collection<String> get() = registeredImports.flatMap { it.supportedExtensions }

	init {
		val list = mutableListOf<IImport>()
		if (Build.VERSION.SDK_INT >= 26) {
			list.add(GpxImport())
		}

		this.registeredImports = list
	}

	fun showImportDialog(context: Context) {
		MaterialDialog(context).show {
			val filter: FileFilter = { file -> registeredImports.any { it.supportedExtensions.contains(file.extension) } }

			fileChooser(filter = filter, waitForPositiveButton = true, allowFolderCreation = false) { _, file ->
				val import = registeredImports.find { it.supportedExtensions.contains(file.extension) }
						?: throw IllegalArgumentException("This is not a valid import file")

				val database = AppDatabase.getDatabase(context)
				import.import(database, file)
			}
		}
	}
}