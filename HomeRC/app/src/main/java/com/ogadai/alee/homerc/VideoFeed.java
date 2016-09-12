package com.ogadai.alee.homerc;

public class VideoFeed {
    private VideoFeed.Callback mCallback;

    private byte[] mUnusedData = null;

    private static byte[] blockMarker = new byte[] { 0, 0, 0, 1 };

    public VideoFeed(VideoFeed.Callback callback) {
        mCallback = callback;
    }

    public void addData(byte[] dataBlock) {
        appendData(dataBlock, dataBlock.length);
        processNextBlock();
    }
    public void addData(byte[] dataBlock, int dataLength) {
        appendData(dataBlock, dataLength);
        processNextBlock();
    }

    private void appendData(byte[] dataBlock, int dataLength) {
        int currentLength = (mUnusedData != null) ? mUnusedData.length : 0;
        int newCount = currentLength + dataLength;
        byte[] newBytes = new byte[newCount];
        for(int n = 0; n < currentLength; n++) {
            newBytes[n] = mUnusedData[n];
        }
        for(int n = 0; n < dataBlock.length; n++) {
            newBytes[currentLength + n] = dataBlock[n];
        }
        mUnusedData = newBytes;
    }

    private void processNextBlock() {
        byte[] nextBlock = getBlock();
        if (nextBlock != null) {
            mCallback.onData(nextBlock);
        }
    }

    private byte[] getBlock() {
        int index = 2;
        while(index < mUnusedData.length && !isBlockMarker(mUnusedData, index)) {
            index++;
        }

        if (index == mUnusedData.length) return null;

        byte[] result = new byte[index];
        for(int n = 0; n < index; n++) {
            result[n] = mUnusedData[n];
        }

        int remainCount = mUnusedData.length - index;
        byte[] remaining = new byte[remainCount];
        for(int n = 0; n < remainCount; n++) {
            remaining[n] = mUnusedData[index + n];
        }
        mUnusedData = remaining;
        return result;
    }

    private boolean isBlockMarker(byte[] entireBytes, int index) {
        if (index >= entireBytes.length - blockMarker.length) return false;

        for(int n = 0; n < blockMarker.length; n++) {
            if (entireBytes[index + n] != blockMarker[n]) return false;
        }
        return true;
    }

    public interface Callback {
        void onData(byte[] dataBlock);
    }
}
