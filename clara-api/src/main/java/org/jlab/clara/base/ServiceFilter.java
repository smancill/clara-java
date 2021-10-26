/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.msg.data.RegQuery;

/**
 * A filter to select services.
 * Use the {@link ClaraFilters} factory to choose one of the filters.
 */
public final class ServiceFilter extends ClaraFilter {

    ServiceFilter(RegQuery query) {
        super(query, TYPE_SERVICE);
    }
}
