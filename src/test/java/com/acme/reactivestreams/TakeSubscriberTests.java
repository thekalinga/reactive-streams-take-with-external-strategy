package com.acme.reactivestreams;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;

import static java.lang.Long.MAX_VALUE;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofSeconds;
import static reactor.core.publisher.Flux.interval;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Flux.range;


@Log4j2
public class TakeSubscriberTests {

  @Test
  void finiteSource() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Subscriber<Integer> downstreamSubscriber = new Subscriber<Integer>() {
      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        log.debug("DOWNSTREAM: Received onSubscribe()");
        log.debug("DOWNSTREAM: Requesting {}L", MAX_VALUE);
        subscription.request(MAX_VALUE);
      }

      @Override
      public void onNext(Integer next) {
        log.debug("DOWNSTREM: Received onNext({}L)", next);
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
        log.error("DOWNSTREAM: Received onError", throwable);
      }

      @Override
      public void onComplete() {
        latch.countDown();
        log.debug("DOWNSTREAM: Received onComplete");
      }
    };

    OneByOneTillLimitAndThenCancelBatchAmountProvider onebyOneTillLimitAndCancelStrategy =
        new OneByOneTillLimitAndThenCancelBatchAmountProvider();

    ExponentialTillLimitAndThenCancelBatchAmountProvider exponentialTillLimitAndThenCancelStrategy =
        new ExponentialTillLimitAndThenCancelBatchAmountProvider();

    // finite source
    range(1, 12)
        .log()
        .subscribe(new TakeSubscriber<>(downstreamSubscriber, 10, exponentialTillLimitAndThenCancelStrategy));

    latch.await();
  }

  @Test
  void unlimitedSource() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Subscriber<Integer> downstreamSubscriber = new Subscriber<Integer>() {
      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        log.debug("DOWNSTREAM: Received onSubscribe()");
        log.debug("DOWNSTREAM: Requesting {}L", MAX_VALUE);
        subscription.request(MAX_VALUE);
      }

      @Override
      public void onNext(Integer next) {
        log.debug("DOWNSTREM: Received onNext({}L)", next);
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
        log.error("DOWNSTREAM: Received onError", throwable);
      }

      @Override
      public void onComplete() {
        latch.countDown();
        log.debug("DOWNSTREAM: Received onComplete");
      }
    };

    UnlimitedTillLimitAndThenCancelBatchAmountProvider unlimitedTillLimitAndCancelStrategy =
        new UnlimitedTillLimitAndThenCancelBatchAmountProvider();

   //infinite source
    interval(ZERO, ofSeconds(1L))
        .map(Long::intValue)
        .log()
        .subscribe(new TakeSubscriber<>(downstreamSubscriber, 10, unlimitedTillLimitAndCancelStrategy));

    latch.await();
  }

  @Test
  void suggestionLimitAndUnlimited() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Subscriber<Integer> downstreamSubscriber = new Subscriber<Integer>() {
      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        log.debug("DOWNSTREAM: Received onSubscribe()");
        log.debug("DOWNSTREAM: Requesting {}L", MAX_VALUE);
        subscription.request(1);
      }

      @Override
      public void onNext(Integer next) {
        log.debug("DOWNSTREM: Received onNext({}L)", next);
        subscription.request(1L);
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
        log.error("DOWNSTREAM: Received onError", throwable);
      }

      @Override
      public void onComplete() {
        latch.countDown();
        log.debug("DOWNSTREAM: Received onComplete");
      }
    };
    TakeSuggestionTillLimitThenUnlimitedBatchAmountProvider
        takeSuggestionTillLimitThenUnlimitedStrategy =
        new TakeSuggestionTillLimitThenUnlimitedBatchAmountProvider();

    // finite source
    range(1, 12)
        .log()
        .subscribe(new TakeSubscriber<>(downstreamSubscriber, 10, takeSuggestionTillLimitThenUnlimitedStrategy));

    latch.await();
  }
}
