package com.acme.reactivestreams;

import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.util.function.Tuple2;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Long.MAX_VALUE;

@Log4j2
public class TakeSubscriber<T> implements Subscriber<T>, Subscription {

  private final Subscriber<T> downstreamSubscriber;
  private final long takeSuggestionAmount;
  private final AtomicLong numOfItemsToSendDownstream = new AtomicLong();;
  private final TakeRequestBatchAmountProvider requestBatchAmountProvider;

  private Subscription upstreamSubscription;
  // If this value would be set to 0, this indicates that we wont request new data anymore
  private final AtomicLong lastRequestedAmount = new AtomicLong(-1);
  private final AtomicLong cumulativeUpstreamAmountRequested = new AtomicLong();
  // since only onNext updates this,
  private long cumulativeDownstreamAmountSent = 0L;
  // Used only when numOfItemsToSendDownstream is MAX_VALUE. This exists to reduce the number queries we need to make to delegate. No need to worry about concurracy as this value is properly guarded by the way code is written & also this value is useful only when numOfItemsToSendDownstream = MAX_VALUE which is a ont time transtion. The only time this value will  be set is before the previous value is set
  private long nextUpstreamCutoffQuestionCumlulativeDownStreamAmount = 0L;
  private final AtomicLong additionalUpstreamAmountRequested = new AtomicLong();

  public TakeSubscriber(Subscriber<T> downstreamSubscriber, long takeSuggestionAmount, TakeRequestBatchAmountProvider requestBatchAmountProvider) {
    this.downstreamSubscriber = downstreamSubscriber;
    this.takeSuggestionAmount = takeSuggestionAmount;
    this.requestBatchAmountProvider = requestBatchAmountProvider;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    log.debug("Received onSubscribe");
    upstreamSubscription = subscription;
    downstreamSubscriber.onSubscribe(this);
  }

  @Override
  public void onNext(T t) {
    log.debug("Received onNext from upstream");
    for (;;) {
      long expected = numOfItemsToSendDownstream.get();
      if (expected > 0) {
        // we need check with delegate again for an unbound upstream case as we dont know when to stop the upstream
        if (expected == MAX_VALUE) {
          downstreamSubscriber.onNext(t);
          cumulativeDownstreamAmountSent++;
          // dont even bother asking delegate if it told us never to ask it again
          if (nextUpstreamCutoffQuestionCumlulativeDownStreamAmount == MAX_VALUE) {
            break;
          }
          // we need check with delegate again for an unbound upstream case (also we dont have worry about)
          if (nextUpstreamCutoffQuestionCumlulativeDownStreamAmount == cumulativeDownstreamAmountSent) {
            Tuple2<Long, Long> nextBatchAmountResponse = requestBatchAmountProvider.getNextBatchAmount(0, cumulativeDownstreamAmountSent, MAX_VALUE, takeSuggestionAmount);
            log.debug("Delegate response {}", nextBatchAmountResponse);
            nextUpstreamCutoffQuestionCumlulativeDownStreamAmount = nextBatchAmountResponse.getT2();
            lastRequestedAmount.set(nextBatchAmountResponse.getT1()); // dont worry about non CAS set as no else updates lastRequestedAmount when its value was MAX_VALUE
            if (lastRequestedAmount.get() == 0L) {
                log.debug("Cancelling upstream subscription as we dont want any more items from upstream");
                upstreamSubscription.cancel();
                downstreamSubscriber.onComplete();
            }
          }
          break;
        } else {
          long target = expected - 1;
          if (numOfItemsToSendDownstream.compareAndSet(expected, target)) {
            log.debug("Updated numOfItemsToSendDownstream to {}", target);
            downstreamSubscriber.onNext(t);
            cumulativeDownstreamAmountSent++;
            if (lastRequestedAmount.get() == 0L && target == 0L) { // since lastRequestedAmount can only set to 0 once, we dont have have to worry about the concurrent updates
              log.debug("Cancelling upstream subscription as we dont want any more items from upstream");
              upstreamSubscription.cancel();
              downstreamSubscriber.onComplete();
            }
            if (target == 0) {
              requestMoreIfPossible(0L); //if the downstream requested unlimited & the strategy did not, we will need to recompute how many more we need to ask upstream
            }
            break;
          }
        }
      } else {
        requestMoreIfPossible(0L); //if the downstream requested unlimited & the strategy did not, we will need to recompute how many more we need to ask upstream
        break;
      }
    }
  }

  @Override
  public void onError(Throwable throwable) {
    boolean streamAlreadyClosed = lastRequestedAmount.get() == 0L && (numOfItemsToSendDownstream.get() == 0L || numOfItemsToSendDownstream.get() == MAX_VALUE);
    if (!streamAlreadyClosed) {
      log.debug("Cascading error to downstream");
      downstreamSubscriber.onError(throwable);
    }
  }

  @Override
  public void onComplete() {
    boolean streamAlreadyClosed = lastRequestedAmount.get() == 0L && (numOfItemsToSendDownstream.get() == 0L || numOfItemsToSendDownstream.get() == MAX_VALUE);
    if (!streamAlreadyClosed) {
      log.debug("Cascading complete to downstream");
      downstreamSubscriber.onComplete();
    }
  }

  @Override
  public void request(long currentDownstreamRequestAmount) {
    log.debug("Received a request({}L) from downstream", currentDownstreamRequestAmount);
    // if we have already resulted more than the downstream demand, lets not request anymore
    for (;;) {
      long expected = additionalUpstreamAmountRequested.get();
      if (expected == MAX_VALUE) {
        log.debug("Since we have already asked upstream to send as many as it can, ignoring this request");
        return;
      } else if (expected >= currentDownstreamRequestAmount) {
        long target = expected - currentDownstreamRequestAmount;
        if (additionalUpstreamAmountRequested.compareAndSet(expected, target)) {
          log.debug("Since we have already asked upstream for this amount, we are ignoring the request");
          return;
        }
      } else {
        // TODO: Should the amount be reduced by (currentDownstreamRequestAmount - expected), if so how to deal with additionalUpstreamAmountRequested when we have concurrent requests?
        requestMoreIfPossible(currentDownstreamRequestAmount);
        return;
      }
    }
  }

  private void requestMoreIfPossible(long currentDownstreamRequestAmount) {
    log.debug("Attempting to request more from upstream");
    for (;;) {
      long expected = lastRequestedAmount.get();
      // since we might stay in this loop for more than onc because of CAS let be certain no one set the value to 0
      if (expected == 0L || expected == MAX_VALUE) {
        log.debug("Ignoring the request as lastRequestedAmount = {}", expected);
        // there nothing more to request as someone else already requested upstream, just ignore the request
        break;
      }
      Tuple2<Long, Long> nextRequestBatchAmountResponse = requestBatchAmountProvider.getNextBatchAmount(currentDownstreamRequestAmount, cumulativeDownstreamAmountSent, cumulativeUpstreamAmountRequested.get(), takeSuggestionAmount);
      long nextRequestBatchAmount = nextRequestBatchAmountResponse.getT1();
      if (lastRequestedAmount.compareAndSet(expected, nextRequestBatchAmount)) {
        log.debug("Will request {} more items after booking keeping", nextRequestBatchAmount);
        if (nextRequestBatchAmount == MAX_VALUE) {
          nextUpstreamCutoffQuestionCumlulativeDownStreamAmount = nextRequestBatchAmountResponse.getT2();
        }
        incrementAdditionalAmountRequestedFromUpstreamTarget(nextRequestBatchAmount, currentDownstreamRequestAmount);
        break;
      }
    }
  }

  private void incrementAdditionalAmountRequestedFromUpstreamTarget(long nextRequestBatchAmount, long currentDownstreamRequestAmount) {
    for (;;) {
      long expected = additionalUpstreamAmountRequested.get();
      if (expected == MAX_VALUE) {
        break;
      }
      long target;
      if (nextRequestBatchAmount == MAX_VALUE) {
        target = MAX_VALUE;
      } else {
        target = expected + (nextRequestBatchAmount - currentDownstreamRequestAmount);
      }
      if (additionalUpstreamAmountRequested.compareAndSet(expected, target)) {
        log.debug("Updated additionalAmountRequestedFromUpstream to {}", target);
        incrementNumOfItemsToSendDownstreamAndRequest(nextRequestBatchAmount);
        break;
      }
    }
  }

  private void incrementNumOfItemsToSendDownstreamAndRequest(long nextRequestBatchAmount) {
    for (;;) {
      long expected = numOfItemsToSendDownstream.get();
      if (expected == MAX_VALUE) {
        break;
      }
      long target = nextRequestBatchAmount == MAX_VALUE ? MAX_VALUE : expected + nextRequestBatchAmount;
      if (numOfItemsToSendDownstream.compareAndSet(expected, target)) {
        log.debug("Updated numOfItemsToSendDownstream to {}", target);
        incrementCumulativeUpstreamRequestAmountRequest(nextRequestBatchAmount);
        break;
      }
    }
  }

  private void incrementCumulativeUpstreamRequestAmountRequest(long nextRequestBatchAmount) {
    for (;;) {
      long expected = cumulativeUpstreamAmountRequested.get();
      if (expected == MAX_VALUE) {
        break;
      }
      long target = nextRequestBatchAmount == MAX_VALUE ? MAX_VALUE : expected + nextRequestBatchAmount;
      if (cumulativeUpstreamAmountRequested.compareAndSet(expected, target)) {
        log.debug("Updated cumulativeUpstreamAmountRequested to {}", target);
        upstreamSubscription.request(nextRequestBatchAmount);
        log.debug("Requested {} items from upstream", nextRequestBatchAmount);
        break;
      }
    }
  }

  @Override
  public void cancel() {
    upstreamSubscription.cancel();
  }
}
