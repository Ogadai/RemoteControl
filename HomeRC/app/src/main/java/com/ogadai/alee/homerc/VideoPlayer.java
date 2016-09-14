package com.ogadai.alee.homerc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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

    private ArrayList<byte[]> mDataBlocks;
    private int mBlockCount;
    private static final int MAX_BLOCK_QUEUE_INIT = 10;
    private static final int MAX_BLOCK_QUEUE = 1;

    private ByteBuffer mLastInputBuffer;
    private int mLastInputIndex;

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
        mLastInputBuffer = null;
    }

    public void addData(byte[] data) {
        if (!initialised()) {
            mDataBlocks = new ArrayList<byte[]>();
            mBlockCount = 1;
            initialise(data);
        } else {
            mBlockCount++;
            if (mAsync) {
                if (mDataBlocks.size() >= (mBlockCount < 10 ? MAX_BLOCK_QUEUE_INIT : MAX_BLOCK_QUEUE)) {
                    System.out.println("Skipped block - " + data.length + " bytes");
                    return;
                }

                if (mLastInputBuffer != null) {
                    System.out.println("Waited for block - " + data.length + " bytes");
                    ByteBuffer buffer = mLastInputBuffer;
                    mLastInputBuffer = null;

                    queueVideoData(buffer, mLastInputIndex, data);
                } else {
                    mDataBlocks.add(data);
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
                        mLastInputBuffer = null;
                        ByteBuffer inputBuffer = codec.getInputBuffer(index);
                        if (mDataBlocks.size() > 0) {
                            byte[] data = mDataBlocks.get(0);
                            mDataBlocks.remove(0);

                            queueVideoData(inputBuffer, index, data);
                        } else {
                            mLastInputBuffer = inputBuffer;
                            mLastInputIndex = index;
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
System.out.println("Video block: " + data.length + " bytes");
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
