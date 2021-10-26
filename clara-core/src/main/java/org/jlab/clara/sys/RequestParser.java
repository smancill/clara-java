/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.data.MetaDataProto.MetaDataOrBuilder;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

final class RequestParser {

    private final MetaDataOrBuilder cmdMeta;
    private final String cmdData;
    private final StringTokenizer tokenizer;


    private RequestParser(MetaDataOrBuilder meta, String data) {
        cmdMeta = meta;
        cmdData = data;
        tokenizer = new StringTokenizer(cmdData, ClaraConstants.DATA_SEP);
    }

    static RequestParser build(Message msg) throws RequestException {
        String mimeType = msg.getMimeType();
        if (mimeType.equals("text/string")) {
            return new RequestParser(msg.getMetaData(), new String(msg.getData()));
        }
        throw new RequestException("Invalid mime-type = " + mimeType);
    }

    public String nextString() throws RequestException {
        try {
            return tokenizer.nextToken();
        } catch (NoSuchElementException e) {
            throw new RequestException(invalidRequestMsg() + ": " + cmdData);
        }
    }

    public String nextString(String defaultValue) {
        return tokenizer.hasMoreElements() ? tokenizer.nextToken() : defaultValue;
    }

    public int nextInteger() throws RequestException {
        try {
            return Integer.parseInt(tokenizer.nextToken());
        } catch (NoSuchElementException | NumberFormatException e) {
            throw new RequestException(invalidRequestMsg() + ": " + cmdData);
        }
    }

    public String request() {
        return cmdData;
    }

    private String invalidRequestMsg() {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid request");
        if (cmdMeta.hasAuthor()) {
            sb.append(" from author = ").append(cmdMeta.getAuthor());
        }
        return sb.toString();
    }


    static class RequestException extends Exception {
        RequestException(String msg) {
            super(msg);
        }
    }
}
