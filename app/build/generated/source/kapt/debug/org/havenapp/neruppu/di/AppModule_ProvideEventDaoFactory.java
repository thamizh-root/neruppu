package org.havenapp.neruppu.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.havenapp.neruppu.data.local.NeruppuDatabase;
import org.havenapp.neruppu.data.local.dao.EventDao;

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
public final class AppModule_ProvideEventDaoFactory implements Factory<EventDao> {
  private final Provider<NeruppuDatabase> dbProvider;

  public AppModule_ProvideEventDaoFactory(Provider<NeruppuDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public EventDao get() {
    return provideEventDao(dbProvider.get());
  }

  public static AppModule_ProvideEventDaoFactory create(Provider<NeruppuDatabase> dbProvider) {
    return new AppModule_ProvideEventDaoFactory(dbProvider);
  }

  public static EventDao provideEventDao(NeruppuDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideEventDao(db));
  }
}
