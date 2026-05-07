package org.havenapp.neruppu.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002R\u0012\u0010\u0003\u001a\u00020\u0004X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lorg/havenapp/neruppu/data/local/NeruppuDatabase;", "Landroidx/room/RoomDatabase;", "()V", "eventDao", "Lorg/havenapp/neruppu/data/local/dao/EventDao;", "getEventDao", "()Lorg/havenapp/neruppu/data/local/dao/EventDao;", "data_debug"})
@androidx.room.Database(entities = {org.havenapp.neruppu.data.local.entity.EventEntity.class}, version = 1, exportSchema = false)
@androidx.room.TypeConverters(value = {org.havenapp.neruppu.data.local.Converters.class})
public abstract class NeruppuDatabase extends androidx.room.RoomDatabase {
    
    public NeruppuDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract org.havenapp.neruppu.data.local.dao.EventDao getEventDao();
}