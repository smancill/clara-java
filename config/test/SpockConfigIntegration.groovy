/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: MIT-0
 */

import org.jlab.clara.tests.Integration

runner {
    unroll {
        unrollByDefault true
    }
    include {
        annotation Integration
    }
}
