package com.acme.reactivestreams;

import reactor.util.function.Tuple2;

/**
 * NOTE: Implementations must be thread safe
 */
public interface TakeRequestBatchAmountProvider {
  /**
   * @param currentUpstreamRequestAmount current number of items downstream want from upstream, can be 0 if this is is not triggered by downstream rather because we already got the previous batch
   * @param cumulativeDownstreamAmount total number of items sent downstream in the past
   * @param cumulativeUpstreamRequestAmount total number of requests sent to upstream
   * @param suggestionAmount take suggestion amount specified via `upstream.take(suggestionAmount)`
   * @return a tuple<new batch amount to request, num of items that should have been pushed downstream before reevaluating whether to cancel upstream or not>. return 0 if you don't want any more items from upstream. Please note that the return tuple's T2 will be considered only when T1 is Long.MAX_VALUE & the reevaluation happens till you explicitly indicate not to receive any more by returning 0L for T1
   */
  Tuple2<Long, Long> getNextBatchAmount(long currentUpstreamRequestAmount,
      long cumulativeDownstreamAmount, long cumulativeUpstreamRequestAmount, long suggestionAmount);
}
