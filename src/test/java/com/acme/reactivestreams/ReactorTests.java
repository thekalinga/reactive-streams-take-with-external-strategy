package com.acme.reactivestreams;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

@Log4j2
public class ReactorTests {

  @Test
  void test() {
//    Flux
//        .just(1, 2, 3, 4, 5)
//        //        .limitRate(1) // does not work
//        .doOnNext(next -> System.out.format("next: %d%n", next))
//        .delayElements(ofSeconds(1L))
//        .limitRate(1) // does not work
//        .zipWith(
//            Flux.just(10000, 100000, 10000, 1000000, 10),
//            (next, multiplier) -> next * multiplier)
////        .limitRate(1) // does not work
//        .doOnNext(multiplied -> System.out.format("multiplied: %d%n", multiplied))
////        .limitRate(1) // does not work
//        .blockLast();

//    Flux
//        .just(1, 2, 3, 4, 5, 6)
//        .delayElements(ofSeconds(1L))
//        .limitRate(1)
//        .doOnNext(next -> System.out.format("next: %d%n", next))
//        .zipWith(
//            Flux.just(10, 100, 1_000, 10_000, 100_000), 1,
//            (next, multiplier) -> next * multiplier)
//        .doOnNext(multiplied -> System.out.format("multiplied: %d%n", multiplied))
//        .blockLast();

//    Flux.just(1)
//        .publishOn(Schedulers.elastic())
//        .doOnSubscribe(v -> {
//          System.out.println(Thread.currentThread().getName());
//        })
//        .doOnNext(v -> {
//          System.out.println(Thread.currentThread().getName());
//        })
//        .subscribeOn(Schedulers.parallel())
//        .subscribe();

//    Flux.range(1, 100)
//        .window(10)
//        .concatMap(list ->
//            list
//                .subscribeOn(Schedulers.parallel())
//                .map(Object::hashCode)
//                .doOnNext(v -> System.out.println(Thread.currentThread().getName()))
//        )
//        .subscribeOn(Schedulers.elastic())
//        .subscribe(System.out::println);

    Flux.range(1, 9)
        .buffer(2, 1)
        .filter(b -> b.size() > 1)
        .map(b -> b.get(0) + b.get(1))
        .subscribe(System.out::println);
  }


  @Test public void bufferTimeshiftOverlap() {
    Flux<String> source = Flux.just("GREEN", "YELLOW", "BLUE", "PINK", "ORANGE")
        .delayElements(Duration.ofMillis(100));
    source.buffer(Duration.ofMillis(400), Duration.ofMillis(300))
        .map(Object::toString)
        .log()
        .blockLast();
  }

  @Test public void error() {
//    Flux.just(1, 2)
//        .materialize()
//        .log()
//        .subscribe(v -> log.debug("Value {}", v), e -> log.error("Received error", e), () -> log.debug("Received completed"));
    Flux.error(new RuntimeException(""))
        .log("materialize")
        .subscribe();
  }

  @Test
  void monoFromFlux() {
    Mono.from(Flux.just(1, 2, 3).log())
        .subscribe(message -> {
          log.debug(message);
        }, e -> {}, () -> log.debug("complete"));
  }

  @Test
  void doOnCancel() {
    Flux<Long> source = Flux.interval(ofSeconds(5))
        .log()
        .doOnCancel(() -> {
          try {
            log.debug("sleeping");
            Thread.sleep(1000L);
            log.debug("wokeup");
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        });
    StepVerifier.create(source)
        .thenCancel()
        .verify();
  }

  @Test
  void doOnComplete() {
    Flux.empty()
        .doOnComplete(() -> {
          try {
            log.debug("sleeping");
            Thread.sleep(1000L);
            log.debug("wokeup");
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        })
        .subscribe(v -> {}, e -> {}, () -> {
          log.debug("complete");
        });
  }

  @Test
  void monoAnd() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Mono.just(1)
        .log()
        .subscribe(log::debug, e -> {}, () -> {
          log.debug("completed");
          latch.countDown();
        });
    latch.await();
  }
}
