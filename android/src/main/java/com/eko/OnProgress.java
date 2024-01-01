package com.eko;

import java.util.HashMap;
import android.app.DownloadManager;
import android.util.Log;

import com.eko.Downloader;
import com.eko.RNBGDTaskConfig;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.database.Cursor;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;

public class OnProgress extends Thread {
  private final long downloadId;
  private final DownloadManager.Query query;
  private final Downloader downloader;
  private Cursor cursor;
  private int lastBytesDownloaded;
  private int bytesTotal;
  private ProgressCallback callback;

  private RNBGDTaskConfig config;

  public OnProgress(RNBGDTaskConfig config, long downloadId,
      Downloader downloader,
      ProgressCallback callback) {
    this.config = config;
    this.callback = callback;

    this.downloadId = downloadId;
    this.query = new DownloadManager.Query();
    query.setFilterById(this.downloadId);

    this.downloader = downloader;
  }

  private void handleInterrupt() {
    try {
      Log.d("RNBackgroundDownloader", "RNBD: OnProgress handleInterrupt. downloadId " + downloadId);
      if (cursor != null) {
        cursor.close();
      }
    } catch (Exception e) {
      return;
    }
    this.interrupt();
  }

  @Override
  public void run() {
    Log.d("RNBackgroundDownloader", "RNBD: OnProgress-1. downloadId " + downloadId);
    while (downloadId > 0) {
      try {
        Log.d("RNBackgroundDownloader", "RNBD: OnProgress-2. downloadId " + downloadId + " destination " + config.destination);

        cursor = downloader.downloadManager.query(query);

        if (!cursor.moveToFirst()) {
          this.handleInterrupt();
        }

        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL) {
          this.handleInterrupt();
        }

        if (status == DownloadManager.STATUS_PAUSED) {
          Log.d("RNBackgroundDownloader", "RNBD: OnProgress-2.1. downloadId " + downloadId);
          Thread.sleep(5000);
        } else if (status == DownloadManager.STATUS_PENDING) {
          Log.d("RNBackgroundDownloader", "RNBD: OnProgress-2.2. downloadId " + downloadId);
          Thread.sleep(1000);
        } else {
          Log.d("RNBackgroundDownloader", "RNBD: OnProgress-2.3. downloadId " + downloadId);
          Thread.sleep(250);
        }

        // get total bytes of the file
        if (bytesTotal <= 0) {
          bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        }

        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

        if (bytesTotal > 0 && bytesDownloaded == bytesTotal) {
          this.handleInterrupt();
        } else {
          lastBytesDownloaded = bytesDownloaded;
        }

        Log.d("RNBackgroundDownloader", "RNBD: OnProgress-3. downloadId " + downloadId + " bytesDownloaded "
            + bytesDownloaded + " bytesTotal " + bytesTotal);

        if (lastBytesDownloaded > 0 && bytesTotal > 0) {
          callback.onProgress(config.id, lastBytesDownloaded, bytesTotal);
        }
      } catch (Exception e) {
        return;
      }

      try {
        if (cursor != null) {
          Log.d("RNBackgroundDownloader", "RNBD: OnProgress-5. downloadId " + downloadId);
          cursor.close();
          Log.d("RNBackgroundDownloader", "RNBD: OnProgress-6. downloadId " + downloadId);
        }
      } catch (Exception e) {
        return;
      }
    }
  }
}
