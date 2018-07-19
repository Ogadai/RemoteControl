package com.ogadai.alee.homerc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class VideoPlayer extends MediaCodec.Callback {
    private Surface mSurface;
    private int mWidth;
    private int mHeight;

    private MediaCodec mMediaCodec = null;
    private long startMs = 0;

    private ArrayList<byte[]> mDataBlocks;
    private int mBlockCount;

    private ByteBuffer mLastInputBuffer;
    private int mLastInputIndex;

    private static final String VIDEO_FORMAT = "video/avc"; // h.264
    private static final int MAX_BLOCK_QUEUE_INIT = 20;
    private static final int MAX_BLOCK_QUEUE = 2;

    public VideoPlayer(Surface surface, int width, int height) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;
    }

    public void stop() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        setLastInputBuffer(null, 0);
    }

    public void addData(byte[] data) {
        if (!initialised()) {
            // First block of data used to initialise the MediaFormat
            mDataBlocks = new ArrayList<byte[]>();
            mBlockCount = 1;
            initialise(data);
        } else {
            mBlockCount++;
            ByteBuffer lastInputBuffer = getLastInputBuffer();
            if (lastInputBuffer != null) {
                // Input Buffer is waiting for data, so feed it immediately
                System.out.println("Waited for block - " + data.length + " bytes");
                queueVideoData(lastInputBuffer, mLastInputIndex, data);
            } else {
                // To reduce lag, skip some blocks if we get a lot queued up
                if (mDataBlocks.size() >= (mBlockCount < 10 ? MAX_BLOCK_QUEUE_INIT : MAX_BLOCK_QUEUE)) {
                    // Skip the oldest block rather than the most recent one
                    byte[] removedBlock = mDataBlocks.remove(0);
                    System.out.println("Skipped block - " + removedBlock.length + " bytes");
                }
                // Add data to queue of blocks for when it is next ready
                mDataBlocks.add(data);
            }
        }
    }

    private boolean initialised() {
        return mMediaCodec != null;
    }

    private void initialise(byte[] csd0) {
        // Create the format based on the first block of data
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_FORMAT, mWidth, mHeight);
//        format.setInteger(MediaFormat.KEY_ROTATION, 180);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_FORMAT);
            mMediaCodec.setCallback(this);

            mMediaCodec.configure(format, mSurface, null, 0);
            mMediaCodec.start();
            startMs = System.currentTimeMillis();
        } catch (Exception e) {
            System.out.println("Error configuring codec - " + e);
            return;
        }
    }

    @Override
    public void onInputBufferAvailable(MediaCodec codec, int index) {
        setLastInputBuffer(null, 0);
        ByteBuffer inputBuffer = codec.getInputBuffer(index);
        if (mDataBlocks.size() > 0) {
            // Data available, so fill the imput buffer and queue it
            byte[] data = mDataBlocks.remove(0);
            queueVideoData(inputBuffer, index, data);
        } else {
            // Data not arrived, so keep the input buffer until we get some
            setLastInputBuffer(inputBuffer, index);
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

    private void queueVideoData(ByteBuffer buffer, int index, byte[] data) {
        buffer.put(data);
        long presentationTimeUs = System.currentTimeMillis() - startMs;
        mMediaCodec.queueInputBuffer(index, 0, data.length, presentationTimeUs, 0);
    }

    private synchronized ByteBuffer getLastInputBuffer() {
        ByteBuffer buffer = mLastInputBuffer;
        mLastInputBuffer = null;
        return buffer;
    }

    private synchronized void setLastInputBuffer(ByteBuffer buffer, int index) {
        mLastInputBuffer = buffer;
        mLastInputIndex = index;
    }

}
