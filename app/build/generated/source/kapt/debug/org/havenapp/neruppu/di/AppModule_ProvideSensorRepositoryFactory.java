package org.havenapp.neruppu.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.havenapp.neruppu.data.local.dao.EventDao;
import org.havenapp.neruppu.domain.repository.SensorRepository;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AppModule_ProvideSensorRepositoryFactory implements Factory<SensorRepository> {
  private final Provider<EventDao> eventDaoProvider;

  public AppModule_ProvideSensorRepositoryFactory(Provider<EventDao> eventDaoProvider) {
    this.eventDaoProvider = eventDaoProvider;
  }

  @Override
  public SensorRepository get() {
    return provideSensorRepository(eventDaoProvider.get());
  }

  public static AppModule_ProvideSensorRepositoryFactory create(
      Provider<EventDao> eventDaoProvider) {
    return new AppModule_ProvideSensorRepositoryFactory(eventDaoProvider);
  }

  public static SensorRepository provideSensorRepository(EventDao eventDao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSensorRepository(eventDao));
  }
}
