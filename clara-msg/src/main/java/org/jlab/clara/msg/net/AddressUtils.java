/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public final class AddressUtils {

    private static List<String> localHostIps = new ArrayList<>();

    private AddressUtils() { }

    /**
     * Returns the localhost IPv4.
     * <p>
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
        return toHostAddress("localhost");
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
        if (localHostIps.isEmpty()) {
            updateLocalHostIps();
        }
        return localHostIps;
    }

    /**
     * Updates the list of IPv4 addresses of the local node.
     * <p>
     * The address returned by {@code InetAddress.getLocalHost()} goes first.
     * Then all non-loopback site-local addresses, and finally all other
     * non-loopback addresses.
     * <p>
     * Fallbacks to the loopback address if no other address was found.
     *
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static void updateLocalHostIps() {
        var ips = new LinkedHashSet<String>();

        try {
            // prefer address returned by getLocalHost first, if any
            InetAddress local = InetAddress.getLocalHost();
            if (local instanceof Inet4Address && !local.isLoopbackAddress()) {
                ips.add(local.getHostAddress());
            }
        } catch (IOException e) {
            // ignore errors to try all network interfaces for an address
        }

        try {
            var localIps = new LinkedHashSet<String>();
            var nonLocalIps = new LinkedHashSet<String>();

            InetAddress loopback = null;

            // get all non-loopback addresses, if any
            Enumeration<NetworkInterface> allIfaces = NetworkInterface.getNetworkInterfaces();
            while (allIfaces.hasMoreElements()) {
                NetworkInterface iface = allIfaces.nextElement();
                if (!iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> allAddr = iface.getInetAddresses();
                while (allAddr.hasMoreElements()) {
                    InetAddress addr = allAddr.nextElement();
                    if (addr instanceof Inet4Address) {
                        if (addr.isLoopbackAddress()) {
                            if (loopback == null) {
                                loopback = addr;
                            }
                            continue;
                        }
                        if (addr.isSiteLocalAddress()) {
                            localIps.add(addr.getHostAddress());
                        } else {
                            nonLocalIps.add(addr.getHostAddress());
                        }
                    }
                }
            }

            ips.addAll(localIps);
            ips.addAll(nonLocalIps);

            // no non-loopback addresses found, default to loopback or throw an error
            if (ips.isEmpty()) {
                if (loopback != null) {
                    ips.add(loopback.getHostAddress());
                } else {
                    throw new IOException("no IPv4 address found");
                }
            }

            localHostIps = new ArrayList<>(ips);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Determines the IP address of the specified host.
     * <p>
     * In case of multiple network cards in the local host, this method will
     * return the first one in the list of addresses of all network cards,
     * with {@code InetAddress.getLocalHost()} being preferred.
     *
     * @param hostName The name of the host (accepts "localhost")
     * @return dotted notation of the IPv4 address
     * @throws UncheckedIOException if the hostname could not be resolved
     */
    public static String toHostAddress(String hostName) {
        if (isIP(hostName)) {
            return hostName;
        }

        if (hostName.equals("localhost")) {
            if (getLocalHostIps().size() > 0) {
                return getLocalHostIps().get(0);
            } else {
                updateLocalHostIps();
                return getLocalHostIps().get(0);
            }
        } else {
            try {
                return InetAddress.getByName(hostName).getHostAddress();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Checks if the host name is an IPv4 address.
     *
     * @param hostname Host name of the computing node.
     * @return true if host name has an IP form.
     */
    public static boolean isIP(String hostname) {
        var p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        var m = p.matcher(hostname);
        return m.find();
    }

    /**
     * Checks if the argument is a valid IP address.
     *
     * @param address the address to be validated
     * @return the given address
     * @throws IllegalArgumentException if the address is null or not a valid IP
     */
    public static String validateIP(String address) {
        if (address == null) {
            throw new IllegalArgumentException("null IP address");
        }
        if (!isIP(address)) {
            throw new IllegalArgumentException("invalid IP address: " + address);
        }
        return address;
    }
}
