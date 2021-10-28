/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Data classes for pub/sub messages and registration requests.
 * <a href="https://developers.google.com/protocol-buffers/">Protocol
 * Buffers</a> are used to generate and serialize the internal data classes.
 * <p>
 * The {@link org.jlab.clara.msg.data.MetaDataProto.MetaData MetaData} class is used to
 * store extra information about a {@link org.jlab.clara.msg.core.Message
 * message} and its data. Although the message class requires the data to be a
 * binary byte array, and the serialization is left to be handled by the
 * applications, the {@link org.jlab.clara.msg.data.PlainDataProto.PlainData PlainData}
 * class is provided as a quick container to send and serialize values of basic
 * types data between actors, working for all supported languages. The
 * default mime-types are listed in {@link org.jlab.clara.msg.data.MimeType MimeType}.
 * <p>
 * The {@link org.jlab.clara.msg.data.RegInfo RegInfo} is used
 * to handle the registration of an actor with a
 * {@link org.jlab.clara.msg.sys.Registrar registrar server},
 * and the {@link org.jlab.clara.msg.data.RegQuery RegQuery}
 * class helps creating a query to a registrar for the specified matching
 * actors.
 * The registration data is returned as
 * {@link org.jlab.clara.msg.data.RegDataProto.RegData RegData}
 * objects, but the {@link org.jlab.clara.msg.data.RegRecord RegRecord}
 * wrapper is provided to simplify accessing the different fields.
 */
package org.jlab.clara.msg.data;
