package org.havenapp.neruppu.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u0006H\u0096@\u00a2\u0006\u0002\u0010\u0007J\u0014\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\tH\u0016J\u0016\u0010\f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lorg/havenapp/neruppu/data/repository/SensorRepositoryImpl;", "Lorg/havenapp/neruppu/domain/repository/SensorRepository;", "eventDao", "Lorg/havenapp/neruppu/data/local/dao/EventDao;", "(Lorg/havenapp/neruppu/data/local/dao/EventDao;)V", "clearEvents", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEvents", "Lkotlinx/coroutines/flow/Flow;", "", "Lorg/havenapp/neruppu/domain/model/Event;", "saveEvent", "event", "(Lorg/havenapp/neruppu/domain/model/Event;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "data_debug"})
public final class SensorRepositoryImpl implements org.havenapp.neruppu.domain.repository.SensorRepository {
    @org.jetbrains.annotations.NotNull()
    private final org.havenapp.neruppu.data.local.dao.EventDao eventDao = null;
    
    public SensorRepositoryImpl(@org.jetbrains.annotations.NotNull()
    org.havenapp.neruppu.data.local.dao.EventDao eventDao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<org.havenapp.neruppu.domain.model.Event>> getEvents() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object saveEvent(@org.jetbrains.annotations.NotNull()
    org.havenapp.neruppu.domain.model.Event event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object clearEvents(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}