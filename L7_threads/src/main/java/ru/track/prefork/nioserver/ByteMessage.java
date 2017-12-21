package ru.track.prefork.nioserver;

import java.nio.ByteBuffer;

public class ByteMessage {

    private byte[] byteArray = null;
    private int position = 0;
    private int size = 0;
    private int capacity = 0;

    public void expand() {
        capacity = capacity * 2 + 128;
        byte[] array = new byte[capacity];
        for (int i = 0; i < size; i++) {
            array[i] = byteArray[i];
        }
        byteArray = array;
    }

    public int writeToMessage(byte[] bytes) {
        return writeToMessage(bytes, 0, bytes.length);
    }

    public int writeToMessage(ByteBuffer byteBuffer, int length) {
        int remaining = Math.min(byteBuffer.remaining(), length);
        while (size + remaining > capacity) {
            expand();
        }
        int bytesToCopy = remaining;
        byteBuffer.get(byteArray, position, bytesToCopy);
        position += bytesToCopy;
        size += bytesToCopy;
        return bytesToCopy;
    }

    public int writeToMessage(byte[] bytes, int offset, int length) {
        int remaining = Math.min(bytes.length - offset, length);
        while(remaining + size > capacity) {
            expand();
        }
        int bytesToCopy = remaining;
        System.arraycopy(bytes, offset, byteArray, position, bytesToCopy);
        this.size = Math.max(size, position + bytesToCopy);
        this.position += bytesToCopy;
        return bytesToCopy;
    }

    public int readToBuffer(ByteBuffer byteBuffer, int offset) {
        int length = Math.min(byteBuffer.remaining(), size - offset);
        byteBuffer.put(byteArray, offset, length);
        return length;
    }

    public byte[] getByteArray() {
        byte[] bytes = new byte[size];
        System.arraycopy(byteArray, 0, bytes, 0, size);
        return bytes;
    }

    public int getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
