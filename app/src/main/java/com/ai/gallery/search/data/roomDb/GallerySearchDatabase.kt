package com.ai.gallery.search.data.roomDb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.ai.gallery.search.data.roomDb.daos.GalleryImageDao
import com.ai.gallery.search.domain.models.GalleryImage
import java.util.Date

@Database(
    entities = [GalleryImage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GallerySearchDatabase : RoomDatabase() {
    abstract fun galleryImageDao(): GalleryImageDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}