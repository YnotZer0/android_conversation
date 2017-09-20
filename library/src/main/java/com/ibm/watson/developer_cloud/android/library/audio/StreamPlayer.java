/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.developer_cloud.android.library.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Exposes the ability to play raw audio data from an InputStream.
 */
public final class StreamPlayer {
  private final String TAG = "StreamPlayer";

  private AudioTrack audioTrack;
  private int sampleRate;

  private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buff = new byte[10240];
    int i;
    while ((i = is.read(buff, 0, buff.length)) > 0) {
      baos.write(buff, 0, i);
    }

    return baos.toByteArray();
  }

  private static int readInt(final byte[] data, final int offset) {
    return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8) | ((data[offset + 2] & 0xff) << 16)
            | (data[offset + 3] << 24); // no 0xff on the last one to keep the sign
  }

  /**
   * Play the given InputStream.
   *
   * @param stream the stream
   */
  public void playStream(InputStream stream) {
    try {
      byte[] data = convertStreamToByteArray(stream);
      if (data.length > 28) {
        sampleRate = readInt(data, 24);
      }
      int headSize = 44, metaDataSize = 48;
      int destPos = headSize + metaDataSize;
      int rawLength = data.length - destPos;
      byte[] d = new byte[rawLength];
      System.arraycopy(data, destPos, d, 0, rawLength);
      initPlayer();
      audioTrack.write(d, 0, d.length);
      stream.close();
      if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
        audioTrack.release();
      }
    } catch (IOException e2) {
      Log.e(TAG, e2.getMessage());
    }
  }

  /**
   * Interrupt the audioStream.
   */
  public void interrupt() {
    if (audioTrack != null) {
      audioTrack.flush();
      audioTrack.stop();
      audioTrack.release();
    }
  }

  /**
   * Initialize AudioTrack by getting buffersize
   */
  private void initPlayer() {
    synchronized (this) {
      int bs = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
      audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
              AudioFormat.ENCODING_PCM_16BIT, bs, AudioTrack.MODE_STREAM);
      if (audioTrack != null)
        audioTrack.play();
    }
  }
}
