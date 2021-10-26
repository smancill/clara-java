/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.msg.data.RegQuery;

/**
 * A filter to select DPEs.
 * Use the {@link ClaraFilters} factory to choose one of the filters.
 */
public class DpeFilter extends ClaraFilter {

    DpeFilter(RegQuery query) {
        super(query, TYPE_DPE);
    }
}
