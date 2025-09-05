package com.example.aichatterdemo;

import com.example.aichatterdemo.strategy.DownloadStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GdriveService {
    @Autowired
    public GdriveService(List<DownloadStrategy> downloadStrategies) {
        this.downloadStrategies = downloadStrategies;
    }

    private DownloadStrategy getDownloadStrategy(String mimeType) {
        for(DownloadStrategy downloadStrategy : downloadStrategies){
            if (downloadStrategy.supports(mimeType)) {
                return downloadStrategy;
            }
        }

        throw new IllegalArgumentException();
    }
}
