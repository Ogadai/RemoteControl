package com.ogadai.alee.homerc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Created by alee on 14/04/2016.
 */
public class VideoPlayer {
    private Surface mSurface;
    private int mWidth;
    private int mHeight;

    private boolean mAsync;

    private MediaCodec mMediaCodec = null;
    private long startMs = 0;

    private byte[] mLastDataBlock;
    private ByteBuffer m_LastInputBuffer;
    private int m_LastInputIndex;

    private static final String VIDEO_FORMAT = "video/avc"; // h.264

    public VideoPlayer(Surface surface, int width, int height) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;
        mAsync = true;
    }

    public void stop() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        m_LastInputBuffer = null;
    }

    public void addData(byte[] data) {
        if (!initialised()) {
            initialise(data);
            mLastDataBlock = null;
        } else {
            if (mAsync) {
                if (mLastDataBlock != null) {
                    System.out.println("Skipped block - " + mLastDataBlock.length + " bytes");
                    mLastDataBlock = null;
                }

                if (m_LastInputBuffer != null) {
                    System.out.println("Waited for block - " + data.length + " bytes");
                    ByteBuffer buffer = m_LastInputBuffer;
                    m_LastInputBuffer = null;

                    queueVideoData(buffer, m_LastInputIndex, data);
                } else {
                    mLastDataBlock = data;
                }
            } else {
                decodeData(data);
                processOutput();
            }
        }
    }

    private boolean initialised() {
        return mMediaCodec != null;
    }

    private void initialise(byte[] csd0) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_FORMAT, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_ROTATION, 180);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_FORMAT);

            if (mAsync) {
                mMediaCodec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(MediaCodec codec, int index) {
                        m_LastInputBuffer = null;
                        ByteBuffer inputBuffer = codec.getInputBuffer(index);
                        if (mLastDataBlock != null) {
                            byte[] data = mLastDataBlock;
                            mLastDataBlock = null;

                            queueVideoData(inputBuffer, index, data);
                        } else {
                            m_LastInputBuffer = inputBuffer;
                            m_LastInputIndex = index;
                        }
                    }

                    @Override
                    public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                        codec.releaseOutputBuffer(index, true);
                    }

                    @Override
                    public void onError(MediaCodec codec, MediaCodec.CodecException e) {

                    }

                    @Override
                    public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {

                    }
                });
            }

            mMediaCodec.configure(format, mSurface, null, 0);
            mMediaCodec.start();
            startMs = System.currentTimeMillis();
        } catch (Exception e) {
            System.out.println("Error configuring codec - " + e);
            return;
        }
    }

    private void queueVideoData(ByteBuffer buffer, int index, byte[] data) {
        buffer.put(data);
        long presentationTimeUs = System.currentTimeMillis() - startMs;
        mMediaCodec.queueInputBuffer(index, 0, data.length, presentationTimeUs, 0);
    }

    private void decodeData(byte[] data) {
        int inIndex = mMediaCodec.dequeueInputBuffer(1000);
        if (inIndex >= 0) {
            ByteBuffer buffer = mMediaCodec.getInputBuffer(inIndex);
            if (buffer != null) {
                buffer.put(data);
                long presentationTimeUs = System.currentTimeMillis() - startMs;
                mMediaCodec.queueInputBuffer(inIndex, 0, data.length, presentationTimeUs, 0);
            }
        }
    }

    public void processOutput() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outIndex = mMediaCodec.dequeueOutputBuffer(info, 1000);
        if(outIndex >= 0) {
            mMediaCodec.releaseOutputBuffer(outIndex, true);
        }
    }
}
