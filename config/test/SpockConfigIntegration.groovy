import org.jlab.clara.tests.Integration

runner {
    unroll {
        unrollByDefault true
    }
    include {
        annotation Integration
    }
}
