/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import com.google.protobuf.ByteString;
import org.jlab.clara.msg.net.AddressUtils;
import org.jlab.clara.msg.sys.utils.ThreadUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods for actors.
 */
public final class ActorUtils {

    private static final int REPLY_TO_SEQUENCE_SIZE = 1000000;

    private static final Random randomGenerator; // nocheck: ConstantName
    private static final AtomicInteger replyToGenerator; // nocheck: ConstantName

    static {
        randomGenerator = new Random();
        replyToGenerator = new AtomicInteger(randomGenerator.nextInt(REPLY_TO_SEQUENCE_SIZE));
    }

    private ActorUtils() { }

    /**
     * Thread sleep wrapper.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    public static void sleep(long millis) {
        ThreadUtils.sleep(millis);
    }

    /**
     * Keeps the current thread sleeping forever.
     */
    public static void keepAlive() {
        while (true) {
            ThreadUtils.sleep(100);
        }
    }

    /**
     * Returns the localhost IPv4.
     *
     * In case of multiple network cards, this method will return the first one
     * in the list of addresses of all network cards, with {@code
     * InetAddress.getLocalHost()} being preferred.
     * <p>
     * Fallbacks to the loopback address if no other address was found.
     *
     * @return a string with the localhost IPv4
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String localhost() {
        return AddressUtils.toHostAddress("localhost");
    }

    /**
     * Returns the list of IPv4 addresses of the local node.
     * Useful when the host has multiple network cards.
     * <p>
     * The address returned by {@code InetAddress.getLocalHost()} goes first.
     * Then all non-loopback site-local addresses, and finally all other
     * non-loopback addresses.
     * <p>
     * Fallbacks to the loopback address if no other address was found.
     *
     * @return list of IP addresses
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static List<String> getLocalHostIps() {
        return AddressUtils.getLocalHostIps();
    }

    /**
     * Determines the IP address of the specified host.
     *
     * In case of multiple network cards in the local host, this method will
     * return the first one in the list of addresses of all network cards,
     * with {@code InetAddress.getLocalHost()} being preferred.
     *
     * @param hostName The name of the host (accepts "localhost")
     * @return dotted notation of the IPv4 address
     * @throws UncheckedIOException if the hostname could not be resolved
     */
    public static String toHostAddress(String hostName) {
        return AddressUtils.toHostAddress(hostName);
    }

    /**
     * Checks if the host name is an IPv4 address.
     *
     * @param hostname Host name of the computing node.
     * @return true if host name has an IP form.
     */
    public static boolean isIP(String hostname) {
        return AddressUtils.isIP(hostname);
    }

    static String getUniqueReplyTo(String subject) {
        long next = replyToGenerator.getAndIncrement() & 0xffffffffL;
        int id = (int) (next % REPLY_TO_SEQUENCE_SIZE + REPLY_TO_SEQUENCE_SIZE);
        return "ret:" + subject + ":" + id;
    }

    // for testing
    static void setUniqueReplyToGenerator(int value) {
        replyToGenerator.set(value);
    }

    static String encodeIdentity(String address, String name) {
        String id = address + "#" + name + "#" + randomGenerator.nextInt(100);
        int idHash = id.hashCode() & Integer.MAX_VALUE;
        int minValue = 0x1000_0000;
        if (idHash < minValue) {
            idHash += minValue;
        }
        return Integer.toHexString(idHash);
    }

    /**
     * Serializes an Object into a protobuf {@link ByteString}.
     *
     * @param object a serializable object
     * @return the serialization of the object as a ByteString
     * @throws IOException if there was an error
     */
    public static ByteString serializeToByteString(Object object) throws IOException {
        if (object instanceof byte[]) {
            return ByteString.copyFrom((byte[]) object);
        } else {
            try (ByteString.Output bs = ByteString.newOutput();
                 ObjectOutputStream out = new ObjectOutputStream(bs)) {
                out.writeObject(object);
                out.flush();
                return bs.toByteString();
            }
        }
    }

    /**
     * Serializes an Object into a byte array.
     *
     * @param object a serializable object
     * @return the serialization of the object as a byte array.
     * @throws IOException if there was an error
     */
    public static byte[] serializeToBytes(Object object)
            throws IOException {
        if (object instanceof byte[]) {
            return (byte[]) object;
        }
        try (ByteArrayOutputStream bs = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bs)) {
            out.writeObject(object);
            out.flush();
            return bs.toByteArray();
        }
    }

    /**
     * De-serializes a protobuf {@link ByteString} into an Object.
     *
     * @param bytes the serialization of the object
     * @return the deserialized Object
     * @throws ClassNotFoundException if class of a serialized object cannot be found.
     * @throws IOException if there was an error
     */
    public static Object deserialize(ByteString bytes)
            throws ClassNotFoundException, IOException {
        byte[] bb = bytes.toByteArray();
        return deserialize(bb);
    }

    /**
     * De-serializes a byte array into an Object.
     *
     * @param bytes the serialization of the object
     * @return the deserialized Object
     * @throws ClassNotFoundException if class of a serialized object cannot be found.
     * @throws IOException if there was an error
     */
    public static Object deserialize(byte[] bytes)
            throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bs)) {
            return in.readObject();
        }
    }

    /**
     * Creates a new Thread that reports uncaught exceptions.
     *
     * @param name the name for the thread
     * @param target the object whose run method is invoked when this thread is started
     * @return a Thread object that will run the target
     */
    public static Thread newThread(String name, Runnable target) {
        return ThreadUtils.newThread(name, target);
    }

    /**
     * Creates a new ThreadPoolExecutor.
     *
     * @param maxThreads the maximum number of threads
     * @param namePrefix the prefix for the name of the threads
     * @return the created custom thread pool executor
     */
    public static ThreadPoolExecutor newThreadPool(int maxThreads, String namePrefix) {
        return ThreadUtils.newThreadPool(maxThreads, namePrefix, new LinkedBlockingQueue<>());
    }

    /**
     * Creates a new ThreadPoolExecutor with a user controlled queue.
     *
     * @param maxThreads the maximum number of threads
     * @param namePrefix the prefix for the name of the threads
     * @param workQueue the queue to hold waiting tasks
     * @return the created custom thread pool executor
     */
    public static ThreadPoolExecutor newThreadPool(int maxThreads,
                                                   String namePrefix,
                                                   BlockingQueue<Runnable> workQueue) {
        return ThreadUtils.newThreadPool(maxThreads, namePrefix, workQueue);
    }
}
