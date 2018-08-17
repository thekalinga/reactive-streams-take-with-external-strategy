package com.acme.reactivestreams;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static java.lang.Long.MAX_VALUE;

public class UnlimitedTillLimitAndThenCancelBatchAmountProvider
    implements TakeRequestBatchAmountProvider {

  @Override
  public Tuple2<Long, Long> getNextBatchAmount(long currentUpstreamRequestAmount, long cumulativeDownstreamAmount,
      long cumulativeUpstreamRequestAmount, long takeSuggestionAmount) {
    if (cumulativeUpstreamRequestAmount == MAX_VALUE) {
      return Tuples.of(0L, takeSuggestionAmount);
    }
    return Tuples.of(MAX_VALUE, takeSuggestionAmount);
  }

}
