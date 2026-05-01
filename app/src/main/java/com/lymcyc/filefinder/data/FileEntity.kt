package com.lymcyc.filefinder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [
        Index("nameLower"),
        Index("pinyinFull"),
        Index("pinyinHead"),
        Index("modified")
    ]
)
data class FileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val nameLower: String,
    val pinyinFull: String,
    val pinyinHead: String,
    val size: Long,
    val modified: Long,
    val isDir: Boolean,
    val ext: String
)
