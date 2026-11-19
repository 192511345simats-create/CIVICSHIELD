package com.civicshield.util;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ComplaintIdGenerator {
    private final AtomicInteger counter = new AtomicInteger(0);

    public String generate() {
        return String.format("CS-%d-%06d", LocalDate.now().getYear(), counter.incrementAndGet());
    }
}
