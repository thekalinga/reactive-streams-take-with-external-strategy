package com.acme.reactivestreams;

import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;
import reactor.test.util.RaceTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.acme.reactivestreams.FluxWrapper.wrap;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.highestOneBit;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Flux.interval;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Flux.range;
import static reactor.test.publisher.PublisherProbe.of;

@Log4j2
public class TakeSubscriberTests {

  @Test
  void oneByOneTillLimitAndCancel() {
    TestPublisher<Integer> testPublisher = TestPublisher.create();

    Flux<Integer> oneByOneTillLimitSource = wrap(Flux.from(testPublisher).log("batchOperator"))
        .batch((__, cumulativeDownstreamAmount, ___) -> cumulativeDownstreamAmount == 3L ? 0L : 1L)
        .log("downstream");

    StepVerifier.create(oneByOneTillLimitSource)
      .then(testPublisher::assertWasSubscribed)
      .then(() -> testPublisher.assertMinRequested(1))
      .then(() -> testPublisher.next(1))
      .expectNext(1)
      .then(() -> testPublisher.assertMinRequested(1))
      .then(() -> testPublisher.next(2))
      .expectNext(2)
      .then(() -> testPublisher.assertMinRequested(1))
      .then(() -> testPublisher.next(3))
      .expectNext(3)
      .then(testPublisher::assertCancelled)
      .expectComplete()
      .verify();
  }

  @Test
  void unlimitedTillLimitAndCancel() {
    TestPublisher<Integer> testPublisher = TestPublisher.create();

    Flux<Integer> oneByOneTillLimitSource = wrap(Flux.from(testPublisher).log("batchOperator"))
        .batch((__, ___, cumulativeUpstreamRequestAmount) -> MAX_VALUE, cumulativeDownstreamAmount -> cumulativeDownstreamAmount == 3L ? 0L : 3L)
        .log("downstream");

    StepVerifier.create(oneByOneTillLimitSource)
        .then(testPublisher::assertWasSubscribed)
        .then(() -> testPublisher.assertMinRequested(MAX_VALUE))
        .then(() -> testPublisher.next(1))
        .expectNext(1)
        .then(() -> testPublisher.next(2))
        .expectNext(2)
        .then(() -> testPublisher.next(3))
        .expectNext(3)
        .then(testPublisher::assertCancelled)
        .expectComplete()
        .verify();
  }

  @Test
  void oneshotTillLimitAndUnlimited() {
    TestPublisher<Integer> testPublisher = TestPublisher.create();

    Flux<Integer> oneByOneTillLimitSource = wrap(Flux.from(testPublisher).log("batchOperator"))
        .batch((__, ___, cumulativeUpstreamRequestAmount) -> cumulativeUpstreamRequestAmount < 3L ? 3L : MAX_VALUE, __ -> MAX_VALUE)
        .log("downstream");

    StepVerifier.create(oneByOneTillLimitSource)
        .then(testPublisher::assertWasSubscribed)
        .then(() -> testPublisher.assertMinRequested(3L))
        .then(() -> testPublisher.next(1))
        .expectNext(1)
        .then(() -> testPublisher.next(2))
        .expectNext(2)
        .then(() -> testPublisher.next(3))
        .expectNext(3)
        .then(() -> testPublisher.assertMinRequested(MAX_VALUE))
        .then(() -> testPublisher.next(4))
        .expectNext(4)
        .thenCancel()
        .verify();
  }

  @Test
  void exponentialTillLimitThenCancel() {
    TestPublisher<Integer> testPublisher = TestPublisher.create();

    Flux<Integer> oneByOneTillLimitSource = wrap(Flux.from(testPublisher).log("batchOperator"))
        .batch((__, ___, cumulativeUpstreamRequestAmount) -> {
          long overallLimit = 10L ;
          if (cumulativeUpstreamRequestAmount == 0) {
            return 1L;
          } else if (cumulativeUpstreamRequestAmount == 1) {
            return 2L;
          }
          long nextPowerOf2 = highestOneBit(cumulativeUpstreamRequestAmount - 1) * 2L;
          return (nextPowerOf2 + cumulativeUpstreamRequestAmount) <= overallLimit ? nextPowerOf2 : (overallLimit - cumulativeUpstreamRequestAmount);
        })
        .log("downstream");

    StepVerifier.create(oneByOneTillLimitSource)
        .then(testPublisher::assertWasSubscribed)
        .then(() -> testPublisher.assertMinRequested(1))
        .then(() -> testPublisher.next(1))
        .expectNext(1)
        .then(() -> testPublisher.assertMinRequested(2))
        .then(() -> testPublisher.next(2, 3))
        .expectNext(2, 3)
        .then(() -> testPublisher.assertMinRequested(4))
        .then(() -> testPublisher.next(4, 5, 6, 7))
        .expectNext(4, 5, 6, 7)
        .then(() -> testPublisher.assertMinRequested(3))
        .then(() -> testPublisher.next(8, 9, 10))
        .expectNext(8, 9, 10)
        .then(testPublisher::assertCancelled)
        .expectComplete()
        .verify();
  }

  @Test
  public void limitRateLowTideRace() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(1);
    final List<Long> requests = Collections.synchronizedList(new ArrayList<>(10_000 / 50));
    final List<Long> downstreamRequests = Collections.synchronizedList(new ArrayList<>(2));

    final Flux<Integer> flux = wrap(
            Flux.range(1, 10_000)
            .hide()
            .doOnRequest(requests::add)
        )
        .batch((__, ___, upstreamReq) -> upstreamReq == 0 ? 100L : 50L)
        .doOnRequest(downstreamRequests::add);

    final BaseSubscriber<Integer> baseSubscriber = new BaseSubscriber<Integer>() {
      @Override
      protected void hookOnSubscribe(Subscription subscription) {	}

      @Override
      protected void hookOnNext(Integer value) {
        counter.incrementAndGet();
      }

      @Override
      protected void hookFinally(SignalType type) {
        latch.countDown();
      }
    };

    flux.subscribe(baseSubscriber);

    RaceTestUtils.race(
        () -> baseSubscriber.request(1_000),
        () -> baseSubscriber.request(9_000));

    latch.await(5, TimeUnit.SECONDS);

    System.out.println(downstreamRequests);

    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(requests).as("initial request").startsWith(100L);
      softly.assertThat(requests.subList(1, requests.size())).as("requests after first").allMatch(i -> i == 50);
      softly.assertThat(counter).as("number of elements received").hasValue(10_000);
      softly.assertThat(requests.stream().mapToLong(i -> i).sum()).as("total propagated demand").isEqualTo(10_000);
    });
  }
}
