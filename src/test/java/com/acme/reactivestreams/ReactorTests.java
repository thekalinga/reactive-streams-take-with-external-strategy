package com.acme.reactivestreams;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

public class ReactorTests {
  @Test
  void test() {
    Flux
        .just(1, 2, 3)
        //        .limitRate(1) // does not work
        .doOnNext(next -> System.out.format("next: %d%n", next))
//        .limitRate(1) // does not work
        .zipWith(
            Flux.just(10, 100, 1000),
            (next, multiplier) -> next * multiplier)
//        .limitRate(1) // does not work
        .doOnNext(multiplied -> System.out.format("multiplied: %d%n", multiplied))
//        .limitRate(1) // does not work
        .blockLast();
  }
}
