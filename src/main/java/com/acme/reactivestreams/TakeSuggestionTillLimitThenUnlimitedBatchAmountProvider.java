package com.acme.reactivestreams;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static java.lang.Long.MAX_VALUE;

public class TakeSuggestionTillLimitThenUnlimitedBatchAmountProvider
    implements TakeRequestBatchAmountProvider {

  @Override
  public Tuple2<Long, Long> getNextBatchAmount(long currentUpstreamRequestAmount,
      long cumulativeDownstreamAmount, long cumulativeUpstreamRequestAmount,
      long takeSuggestionAmount) {
    if ((currentUpstreamRequestAmount + cumulativeUpstreamRequestAmount) <= takeSuggestionAmount) {
      return Tuples.of(takeSuggestionAmount, 0L);
    }
    return Tuples.of(MAX_VALUE, MAX_VALUE);
  }

}
