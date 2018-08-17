package com.acme.reactivestreams;

import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

@Log4j2
public class TakeSubscriberTckTests extends SubscriberBlackboxVerification<Integer> {

  public TakeSubscriberTckTests() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber() {
    return new TakeSubscriber<>(new Subscriber<Integer>() {
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(Integer integer) {
        if (integer == null) {
          throw new NullPointerException();
        }
        log.debug("Next val {}", integer);
      }

      @Override
      public void onError(Throwable throwable) {
        if (throwable == null) {
          throw new NullPointerException();
        }
        log.error("Received error", throwable);
      }

      @Override
      public void onComplete() {
        log.error("Is completed");
      }
    }, 10, new OneByOneTillLimitThenCancelBatchAmountProvider());
  }

  @Override
  public Integer createElement(int i) {
    return i;
  }

  @Override
  public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() {
  }
}
