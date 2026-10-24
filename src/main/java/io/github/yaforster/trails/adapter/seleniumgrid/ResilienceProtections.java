package io.github.yaforster.trails.adapter.seleniumgrid;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

record ResilienceProtections(CircuitBreaker circuitBreaker, Retry retry, Bulkhead bulkhead) {
}
