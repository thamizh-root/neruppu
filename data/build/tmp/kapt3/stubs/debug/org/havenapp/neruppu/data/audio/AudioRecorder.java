package org.havenapp.neruppu.data.audio;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\t\u001a\u00020\bH\u0002J\u0006\u0010\n\u001a\u00020\u000bJ\u0010\u0010\f\u001a\u0004\u0018\u00010\u0006H\u0086@\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0086@\u00a2\u0006\u0002\u0010\rR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lorg/havenapp/neruppu/data/audio/AudioRecorder;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "currentOutputFile", "Ljava/io/File;", "mediaRecorder", "Landroid/media/MediaRecorder;", "createMediaRecorder", "getMaxAmplitude", "", "startRecording", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "stopRecording", "Landroid/net/Uri;", "data_debug"})
public final class AudioRecorder {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private android.media.MediaRecorder mediaRecorder;
    @org.jetbrains.annotations.Nullable()
    private java.io.File currentOutputFile;
    
    public AudioRecorder(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @kotlin.Suppress(names = {"DEPRECATION"})
    private final android.media.MediaRecorder createMediaRecorder() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object startRecording(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion) {
        return null;
    }
    
    public final int getMaxAmplitude() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object stopRecording(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super android.net.Uri> $completion) {
        return null;
    }
}