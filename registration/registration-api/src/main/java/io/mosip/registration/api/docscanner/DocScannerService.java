package io.mosip.registration.api.docscanner;

import io.mosip.registration.api.docscanner.dto.DocScanDevice;

import java.awt.image.BufferedImage;
import java.util.List;

public interface DocScannerService {

    String getServiceName();

    BufferedImage scan(DocScanDevice docScanDevice);

    List<DocScanDevice> getConnectedDevices();
}
