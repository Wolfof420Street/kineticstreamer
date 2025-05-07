// IStreamingService.aidl
package com.kevmo314.kineticstreamer;

import android.view.Surface;
import com.kevmo314.kineticstreamer.StreamingConfiguration;

interface IStreamingService {
    void setPreviewSurface(in Surface surface);

    void startStreaming(in StreamingConfiguration config);

    void stopStreaming();

    boolean isStreaming();

    String getActiveCameraId();

    void setActiveCameraId(String cameraId);
}