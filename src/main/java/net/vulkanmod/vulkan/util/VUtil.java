package net.vulkanmod.vulkan.util;

import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Collection;

public class VUtil {
    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {

        MemoryStack stack = MemoryStack.stackGet();

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static void memcpy(ByteBuffer src, long dstPtr) {
        MemoryUtil.memCopy(MemoryUtil.memAddress0(src), dstPtr, src.capacity());
    }

    public static void memcpy(ByteBuffer src, Buffer dst, long size) {
        if (size > dst.getBufferSize() - dst.getUsedBytes()) {
            throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
        }

        final long srcPtr = MemoryUtil.memAddress(src);
        final long dstPtr = dst.getDataPtr() + dst.getUsedBytes();

        MemoryUtil.memCopy(srcPtr, dstPtr, size);
    }

    public static void memcpy(Buffer src, ByteBuffer dst, long size) {
        if (size > dst.remaining()) {
            throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
        }

        final long srcPtr = src.getDataPtr();
        final long dstPtr = MemoryUtil.memAddress(dst);

        MemoryUtil.memCopy(srcPtr, dstPtr, size);
    }

    public static void memcpy(ByteBuffer src, Buffer dst, long size, long srcOffset, long dstOffset) {
        if (size > dst.getBufferSize() - dstOffset) {
            throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
        }

        final long dstPtr = dst.getDataPtr() + dstOffset;
        final long srcPtr = MemoryUtil.memAddress(src) + srcOffset;
        MemoryUtil.memCopy(srcPtr, dstPtr, size);
    }

    public static int align(int x, int align) {
        int r = x % align;
        return r == 0 ? x : x + align - r;
    }

}
