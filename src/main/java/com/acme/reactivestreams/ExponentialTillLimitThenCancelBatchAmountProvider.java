package com.acme.reactivestreams;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Long.highestOneBit;

public class ExponentialTillLimitThenCancelBatchAmountProvider implements TakeRequestBatchAmountProvider {

  @Override
  public Tuple2<Long, Long> getNextBatchAmount(long currentUpstreamRequestAmount, long cumulativeDownstreamAmount,
      long cumulativeUpstreamRequestAmount, long suggestionAmount) {
    if (cumulativeDownstreamAmount == suggestionAmount) {
      return Tuples.of(0L, 0L);
    }
    if (cumulativeUpstreamRequestAmount == 0) {
      return Tuples.of(1L, 0L);
    } else if (cumulativeUpstreamRequestAmount == 1) {
      return Tuples.of(2L, 0L);
    }
    long nextPowerOfTwo = highestOneBit(cumulativeUpstreamRequestAmount - 1) * 2L;
    return Tuples.of(nextPowerOfTwo, 0L);
  }

}
