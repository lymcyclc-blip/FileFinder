package com.lymcyc.filefinder.scanner

import android.os.Environment
import com.lymcyc.filefinder.data.FileDao
import com.lymcyc.filefinder.data.FileEntity
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import java.io.File
import java.util.ArrayDeque

class FileScanner(private val dao: FileDao) {

    private val skipPrefixes = listOf(
        "Android/data",
        "Android/obb",
        ".thumbnails",
        ".cache"
    )

    suspend fun scanAll(onProgress: (Int) -> Unit = {}) {
        dao.clear()
        val root = Environment.getExternalStorageDirectory() ?: return
        val rootPath = root.absolutePath
        val batch = ArrayList<FileEntity>(BATCH_SIZE)
        var count = 0
        val stack = ArrayDeque<File>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val dir = stack.pop()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                val name = f.name
                if (name.startsWith(".")) continue
                val rel = f.absolutePath.removePrefix(rootPath).trimStart('/')
                if (skipPrefixes.any { rel.startsWith(it) }) continue

                batch.add(toEntity(f))
                count++
                if (f.isDirectory) stack.push(f)
                if (batch.size >= BATCH_SIZE) {
                    dao.insertAll(batch)
                    batch.clear()
                    onProgress(count)
                }
            }
        }
        if (batch.isNotEmpty()) {
            dao.insertAll(batch)
            onProgress(count)
        }
    }

    companion object {
        private const val BATCH_SIZE = 500

        private val pinyinFormat: HanyuPinyinOutputFormat = HanyuPinyinOutputFormat().apply {
            caseType = HanyuPinyinCaseType.LOWERCASE
            toneType = HanyuPinyinToneType.WITHOUT_TONE
        }

        private fun isChinese(c: Char): Boolean = c.code in 0x4E00..0x9FFF

        private fun pinyinOf(c: Char): String? {
            return try {
                val arr = PinyinHelper.toHanyuPinyinStringArray(c, pinyinFormat)
                arr?.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }

        fun toEntity(f: File): FileEntity {
            val name = f.name
            val ext = if (f.isDirectory) "" else name.substringAfterLast('.', "").lowercase()
            return FileEntity(
                path = f.absolutePath,
                name = name,
                nameLower = name.lowercase(),
                pinyinFull = pinyinFull(name),
                pinyinHead = pinyinHead(name),
                size = if (f.isFile) f.length() else 0L,
                modified = f.lastModified(),
                isDir = f.isDirectory,
                ext = ext
            )
        }

        private fun pinyinFull(s: String): String {
            val sb = StringBuilder(s.length * 2)
            for (c in s) {
                if (isChinese(c)) {
                    val py = pinyinOf(c)
                    if (py != null) sb.append(py) else sb.append(c)
                } else {
                    sb.append(c.lowercaseChar())
                }
            }
            return sb.toString()
        }

        private fun pinyinHead(s: String): String {
            val sb = StringBuilder(s.length)
            for (c in s) {
                if (isChinese(c)) {
                    val py = pinyinOf(c)
                    if (!py.isNullOrEmpty()) sb.append(py.first())
                } else if (c.isLetterOrDigit()) {
                    sb.append(c.lowercaseChar())
                }
            }
            return sb.toString()
        }
    }
}
