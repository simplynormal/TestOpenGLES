package com.hcmut.test.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {WayEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "turn-by-turn-db";
    public abstract WayDao wayDao();
}