package com.zalora.zcast.interceptor;

import net.jpountz.lz4.*;
import java.io.Serializable;
import com.hazelcast.logging.*;
import com.hazelcast.map.MapInterceptor;
import com.google.common.collect.ImmutableList;
import com.hazelcast.internal.ascii.memcache.MemcacheEntry;

import static com.hazelcast.util.StringUtil.bytesToString;

/**
 * Use LZ4 to bring down the storage size a little
 * This class only works with php-memcached because it relies on the data types encoded in the flags
 *
 * @author Wolfram Huesken <wolfram.huesken@zalora.com>
 */
public class CompressionInterceptor implements MapInterceptor, Serializable {

    private static final int PHP_FLAG_STRING = 0;
    private static final int PHP_FLAG_LONG = 1;
    private static final int PHP_FLAG_PHP_SERIALIZED = 4;
    private static final int PHP_FLAG_JSON_SERIALIZED = 6;

    public static final ImmutableList<Integer> PHP_FLAGS = ImmutableList.of(
        PHP_FLAG_STRING, PHP_FLAG_LONG, PHP_FLAG_PHP_SERIALIZED, PHP_FLAG_JSON_SERIALIZED
    );

    private static final int ZCAST_FLAG_STRING = 1;
    private static final int ZCAST_FLAG_PHP_SERIALIZED = 2;
    private static final int ZCAST_FLAG_JSON_SERIALIZED = 4;

    private static final int COMPRESSION_THRESHOLD = 2048;

    private static final LZ4Factory lz4Factory;
    private static final LZ4Compressor compressor;
    private static final LZ4FastDecompressor fastDecompressor;

    private final transient ILogger logger;

    static {
        lz4Factory = LZ4Factory.fastestInstance();
        compressor = lz4Factory.fastCompressor();
        fastDecompressor = lz4Factory.fastDecompressor();
    }

    /**
     * Init logger
     * @param loggingService Hz's logging service
     */
    public CompressionInterceptor(LoggingService loggingService, String map) {
        logger = loggingService.getLogger("zcast.interceptor." + map);
    }

    @Override
    public Object interceptGet(Object value) {
        if (value == null) {
            logger.finest("Item not found");
            return null;
        }

        if (!(value instanceof MemcacheEntry)) {
            logger.warning(String.format("Item is not a MemcacheEntry, but a %s", value.getClass().getName()));
        }

        final MemcacheEntry entry = (MemcacheEntry) value;
        final int flag = entry.getFlag();
        String key = getKey(entry);

        // If it's uncompressed, we're done here
        if (PHP_FLAGS.contains(flag)) {
            logger.fine(String.format("Key '%s' has flag %d: Stays untouched", key, flag));
            return null;
        }

        final byte[] compressed = entry.getValue();
        final int decompressedLength = getOriginalEntrySize(flag);
        byte[] uncompressed = new byte[decompressedLength];

        try {
            fastDecompressor.decompress(compressed, 0, uncompressed, 0, decompressedLength);
        } catch (LZ4Exception lex) {
            logger.severe(String.format("Key '%s' failed to decompress", key), lex);
            return null;
        }

        // Return uncompressed item to memcached client
        int originalFlag = getOriginalFlag(flag);

        logger.fine(String.format("Decompressed Key '%s', restored flag to %d", key, originalFlag));
        return new MemcacheEntry(key, uncompressed, originalFlag);
    }

    /**
     * Intercept the new object to set and decide if it has to be compressed or not
     * @param oldValue Already in hazelcast
     * @param value The new value which is freshly coming in
     * @return Either the original or the compressed memcached item
     */
    @Override
    public Object interceptPut(Object oldValue, Object value) {
        if (value == null || !(value instanceof MemcacheEntry)) {
            return null;
        }

        final MemcacheEntry entry = (MemcacheEntry) value;
        final int flag = entry.getFlag();
        final String key = getKey(entry);

        // If flags are not PHP values, then it's probably already compressed, so we don't touch it
        if (!PHP_FLAGS.contains(flag)) {
            logger.fine(String.format("Key '%s' has flag %d and stays uncompressed", key, flag));
            return null;
        }

        final byte[] data = entry.getValue();
        final int decompressedLength = data.length;

        // If data length is below the threshold, we leave it uncompressed
        if (decompressedLength < COMPRESSION_THRESHOLD) {
            logger.fine(String.format("Key '%s' is too small to be compressed: %d bytes", getKey(entry), data.length));
            return null;
        }

        // Do the compression magic
        int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = compressor.compress(data, 0, decompressedLength, compressed, 0, maxCompressedLength);

        byte[] trimmedCompressed = new byte[compressedLength];
        System.arraycopy(compressed, 0, trimmedCompressed, 0, compressedLength);

        // Put compressed item in hz
        int newFlag = getNewFlag(flag, decompressedLength);

        logger.fine(String.format("Key '%s' now is %d bytes and has the flag %d", key, compressedLength, newFlag));
        return new MemcacheEntry(key, trimmedCompressed, newFlag);
    }

    /**
     * I really wonder why the entry doesn't store the key as a property
     * @param entry Hazelcast's mc item representation
     * @return The key
     */
    private String getKey(MemcacheEntry entry) {
        String[] blob = bytesToString(entry.getBytes()).split(" ");
        if (blob.length > 2) {
            return blob[1];
        }

        throw new RuntimeException("Cannot parse out key from MemcacheEntry");
    }

    /**
     * Encode uncompressed size and datatype with a bitmask (3 bit for 3 datatypes)
     * @param flag The current flag set by php-memcached
     * @param length The uncompressed length we want to preserve
     * @return the encoded result
     */
    private int getNewFlag(int flag, int length) {
        int newFlag = length << 3;

        switch (flag) {
            case PHP_FLAG_STRING:
                newFlag |= ZCAST_FLAG_STRING;
                break;
            case PHP_FLAG_PHP_SERIALIZED:
                newFlag |= ZCAST_FLAG_PHP_SERIALIZED;
                break;
            case PHP_FLAG_JSON_SERIALIZED:
                newFlag |= ZCAST_FLAG_JSON_SERIALIZED;
                break;
        }

        return newFlag;
    }

    /**
     * As the last 3 bits are reserved for the data type, we can kick them out by shifting 3 bits to the right
     * @param flag encoded flag
     * @return the uncompressed size in bytes
     */
    private int getOriginalEntrySize(int flag) {
        return flag >> 3;
    }

    /**
     * Magic number 7 leaves the last 3 bits intact
     * @param flag encoded flag
     * @return return the data type set by PHP
     */
    private int getOriginalFlag(int flag) {
        flag &= 7;

        switch (flag) {
            case ZCAST_FLAG_STRING:
                return PHP_FLAG_STRING;
            case ZCAST_FLAG_PHP_SERIALIZED:
                return PHP_FLAG_PHP_SERIALIZED;
            case ZCAST_FLAG_JSON_SERIALIZED:
                return PHP_FLAG_JSON_SERIALIZED;
        }

        throw new RuntimeException(String.format("Bitshit didn't work. Flag is now %d", flag));
    }

    @Override
    public void afterPut(Object value) {}

    @Override
    public Object interceptRemove(Object value) { return null; }

    @Override
    public void afterRemove(Object value) {}

    @Override
    public void afterGet(Object value) {}
}
