/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: MIT-0
 */

import org.jlab.clara.tests.Integration
import org.spockframework.runtime.model.parallel.ExecutionMode

runner {
    parallel {
        enabled false
        defaultSpecificationExecutionMode = ExecutionMode.CONCURRENT
        defaultExecutionMode = ExecutionMode.SAME_THREAD
    }
    unroll {
        unrollByDefault true
    }
    exclude {
        annotation Integration
    }
}
