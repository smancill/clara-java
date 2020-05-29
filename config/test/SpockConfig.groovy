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
}
