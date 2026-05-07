package org.havenapp.neruppu.data.camera.analyzer;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u00002\u00020\u0001B\u0019\u0012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0010\u001a\u00020\u00052\u0006\u0010\u0011\u001a\u00020\u0012H\u0016J8\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\b2\u0006\u0010\u0015\u001a\u00020\b2\u0006\u0010\u0016\u001a\u00020\f2\u0006\u0010\u0017\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\f2\u0006\u0010\u0019\u001a\u00020\fH\u0002R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\nX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lorg/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer;", "Landroidx/camera/core/ImageAnalysis$Analyzer;", "onMotionDetected", "Lkotlin/Function1;", "", "", "(Lkotlin/jvm/functions/Function1;)V", "bufferReuse", "", "lastReferenceUpdateTime", "", "pixelDiffThreshold", "", "referenceBuffer", "referenceUpdateIntervalMs", "samplingStep", "analyze", "image", "Landroidx/camera/core/ImageProxy;", "calculateMotion", "current", "reference", "width", "height", "rowStride", "pixelStride", "data_debug"})
public final class MotionAnalyzer implements androidx.camera.core.ImageAnalysis.Analyzer {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.Double, kotlin.Unit> onMotionDetected = null;
    @org.jetbrains.annotations.Nullable()
    private byte[] referenceBuffer;
    @org.jetbrains.annotations.Nullable()
    private byte[] bufferReuse;
    private long lastReferenceUpdateTime = 0L;
    private final long referenceUpdateIntervalMs = 500L;
    private final int pixelDiffThreshold = 15;
    private final int samplingStep = 4;
    
    public MotionAnalyzer(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Double, kotlin.Unit> onMotionDetected) {
        super();
    }
    
    @java.lang.Override()
    public void analyze(@org.jetbrains.annotations.NotNull()
    androidx.camera.core.ImageProxy image) {
    }
    
    private final double calculateMotion(byte[] current, byte[] reference, int width, int height, int rowStride, int pixelStride) {
        return 0.0;
    }
}