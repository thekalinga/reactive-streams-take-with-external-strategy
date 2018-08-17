package com.acme.reactivestreams;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class OneByOneTillLimitAndThenCancelBatchAmountProvider implements TakeRequestBatchAmountProvider {

  @Override
  public Tuple2<Long, Long> getNextBatchAmount(long currentUpstreamRequestAmount, long cumulativeDownstreamAmount,
      long cumulativeUpstreamRequestAmount, long suggestionAmount) {
    if (cumulativeDownstreamAmount == suggestionAmount) {
      return Tuples.of(0L, 0L);
    }
    return Tuples.of(1L, 0L);
  }

}
