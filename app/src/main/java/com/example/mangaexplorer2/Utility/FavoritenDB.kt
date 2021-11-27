package com.example.mangaexplorer2.Utility

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.Models.NextChapterState

private var favoriteDb: FavoritenDB? = null

fun getFavoriteDbInstance(context: Context): FavoritenDB {
    if(favoriteDb == null){
        favoriteDb = FavoritenDB(context)
    }
    return favoriteDb!!
}

fun closeFavoriteDbInstance(): Unit {
    favoriteDb?.closeDB()
    favoriteDb = null
}


class FavoritenDB(context: Context) {
    val db: SQLiteDatabase? = DbHelper(
        context,
        "favorites",
        """
	"idFavorite"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"mangaTitle"	TEXT NOT NULL,
	"mangaSource"	TEXT NOT NULL,
	"coverImageUrl"	TEXT NOT NULL,
	"chapterMenuUrl"	TEXT NOT NULL,
	"currentPageUrl"	TEXT NOT NULL,
	"newChapterAvailable"	INTEGER NOT NULL DEFAULT 0
    """.trimIndent()
    )
        .writableDatabase

    fun saveReadingProgress(favoriteItem: FavoriteItem): Unit {
        if (db != null) {
            if (mangaIsFavorite(favoriteItem.mangaSource,favoriteItem.mangaTitle)) {
                db.execSQL(
                    """
                UPDATE favorites SET mangaTitle="${favoriteItem.mangaTitle}", mangaSource="${favoriteItem.mangaSource}", coverImageUrl="${favoriteItem.coverImageUrl}", chapterMenuUrl="${favoriteItem.chapterMenuUrl}", currentPageUrl="${favoriteItem.currentPageUrl}", newChapterAvailable=0 WHERE mangaTitle = "${favoriteItem.mangaTitle}" AND mangaSource = "${favoriteItem.mangaSource}"
         """.trimIndent()
                )
            } else {
                db.execSQL("""INSERT INTO favorites(mangaTitle, mangaSource, coverImageUrl, chapterMenuUrl,currentPageUrl, newChapterAvailable) VALUES("${favoriteItem.mangaTitle}", "${favoriteItem.mangaSource}", "${favoriteItem.coverImageUrl}", "${favoriteItem.chapterMenuUrl}","${favoriteItem.currentPageUrl}", 0)""".trimIndent())
            }
        }
    }

    fun closeDB(): Unit {
        db?.close()
    }

    fun removeReadingProgress(mangaSource: String, mangaTitle: String): Unit {
        if (db != null) {
            db.execSQL(
                """
                DELETE FROM favorites WHERE mangaTitle="$mangaTitle" AND mangaSource="$mangaSource"
         """.trimIndent()
            )
        }
    }

    fun setChapterAvailable(isAvailable: NextChapterState,mangaSource: String, mangaTitle: String): Unit {
        if (db != null) {
            val available = isAvailable.ordinal
            db.execSQL(
                """
                UPDATE favorites SET newChapterAvailable=$available WHERE mangaTitle="$mangaTitle" AND mangaSource="$mangaSource"
         """.trimIndent()
            )
        }
    }

    fun mangaIsFavorite(mangaSource: String, mangaTitle: String): Boolean {
        if (db != null) {
            val cursor = db.rawQuery(
                """
                SELECT idFavorite FROM favorites WHERE mangaTitle = "$mangaTitle" AND mangaSource = "$mangaSource"
            """.trimIndent(), null
            )
            return cursor.count >= 1
        }
        return false
    }

    private fun getNextChapterStateFromInt(chapterStateNumber: Int): NextChapterState{
        if(chapterStateNumber == 0){
            return NextChapterState.UNAVAILABLE
        }
        if(chapterStateNumber == 1){
            return NextChapterState.AVAILABLE;
        }
        return NextChapterState.ERROR;
    }

    fun getFavorites(): List<FavoriteItem> {
        if (db != null) {
            val cursor = db.rawQuery(
                """
                SELECT * FROM favorites
            """.trimIndent(), null
            )

            val favoriteItems = mutableListOf<FavoriteItem>()
            if (cursor.moveToFirst()) {
                do {
                    favoriteItems += FavoriteItem(
                        mangaTitle = cursor.getString(cursor.getColumnIndex("mangaTitle")),
                        mangaSource = cursor.getString(cursor.getColumnIndex("mangaSource")),
                        coverImageUrl = cursor.getString(cursor.getColumnIndex("coverImageUrl")),
                        chapterMenuUrl = cursor.getString(cursor.getColumnIndex("chapterMenuUrl")),
                        currentPageUrl = cursor.getString(cursor.getColumnIndex("currentPageUrl")),
                        hasNewChapter = getNextChapterStateFromInt(cursor.getInt(cursor.getColumnIndex("newChapterAvailable")))
                    )
                } while (cursor.moveToNext())
            }
            return favoriteItems.toList()
        }
        return emptyList()
    }
}