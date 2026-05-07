package org.havenapp.neruppu;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SensorRepository> sensorRepositoryProvider;

  private final Provider<CameraManager> cameraManagerProvider;

  public MainActivity_MembersInjector(Provider<SensorRepository> sensorRepositoryProvider,
      Provider<CameraManager> cameraManagerProvider) {
    this.sensorRepositoryProvider = sensorRepositoryProvider;
    this.cameraManagerProvider = cameraManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SensorRepository> sensorRepositoryProvider,
      Provider<CameraManager> cameraManagerProvider) {
    return new MainActivity_MembersInjector(sensorRepositoryProvider, cameraManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSensorRepository(instance, sensorRepositoryProvider.get());
    injectCameraManager(instance, cameraManagerProvider.get());
  }

  @InjectedFieldSignature("org.havenapp.neruppu.MainActivity.sensorRepository")
  public static void injectSensorRepository(MainActivity instance,
      SensorRepository sensorRepository) {
    instance.sensorRepository = sensorRepository;
  }

  @InjectedFieldSignature("org.havenapp.neruppu.MainActivity.cameraManager")
  public static void injectCameraManager(MainActivity instance, CameraManager cameraManager) {
    instance.cameraManager = cameraManager;
  }
}
