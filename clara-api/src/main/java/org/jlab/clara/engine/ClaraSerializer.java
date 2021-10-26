/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine;

import org.jlab.clara.base.error.ClaraException;

import java.nio.ByteBuffer;

/**
 * Provides the custom serialization methods to send user defined data through
 * the network.
 */
public interface ClaraSerializer {

    /**
     * Serializes the user object into a byte buffer and returns it.
     *
     * @param data the user object stored on the {@link EngineData}
     * @throws ClaraException if the data could not be serialized
     * @return the serialized user object
     */
    ByteBuffer write(Object data) throws ClaraException;

    /**
     * De-serializes the byte buffer into the user object and returns it.
     *
     * @param buffer the serialized data
     * @throws ClaraException if the data could not be deserialized
     * @return the user-object
     */
    Object read(ByteBuffer buffer) throws ClaraException;
}
