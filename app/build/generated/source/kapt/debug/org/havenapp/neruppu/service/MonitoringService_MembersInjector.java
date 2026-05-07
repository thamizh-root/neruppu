package org.havenapp.neruppu.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.havenapp.neruppu.data.camera.CameraManager;
import org.havenapp.neruppu.domain.repository.SensorRepository;

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
public final class MonitoringService_MembersInjector implements MembersInjector<MonitoringService> {
  private final Provider<SensorRepository> sensorRepositoryProvider;

  private final Provider<CameraManager> cameraManagerProvider;

  public MonitoringService_MembersInjector(Provider<SensorRepository> sensorRepositoryProvider,
      Provider<CameraManager> cameraManagerProvider) {
    this.sensorRepositoryProvider = sensorRepositoryProvider;
    this.cameraManagerProvider = cameraManagerProvider;
  }

  public static MembersInjector<MonitoringService> create(
      Provider<SensorRepository> sensorRepositoryProvider,
      Provider<CameraManager> cameraManagerProvider) {
    return new MonitoringService_MembersInjector(sensorRepositoryProvider, cameraManagerProvider);
  }

  @Override
  public void injectMembers(MonitoringService instance) {
    injectSensorRepository(instance, sensorRepositoryProvider.get());
    injectCameraManager(instance, cameraManagerProvider.get());
  }

  @InjectedFieldSignature("org.havenapp.neruppu.service.MonitoringService.sensorRepository")
  public static void injectSensorRepository(MonitoringService instance,
      SensorRepository sensorRepository) {
    instance.sensorRepository = sensorRepository;
  }

  @InjectedFieldSignature("org.havenapp.neruppu.service.MonitoringService.cameraManager")
  public static void injectCameraManager(MonitoringService instance, CameraManager cameraManager) {
    instance.cameraManager = cameraManager;
  }
}
