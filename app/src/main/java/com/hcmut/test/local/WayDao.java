package com.hcmut.test.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WayDao {
    @Query("SELECT * FROM wayentity WHERE tileIds LIKE :tileId")
    WayEntity[] getWaysByTileId(int tileId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<WayEntity> wayEntities);
}
