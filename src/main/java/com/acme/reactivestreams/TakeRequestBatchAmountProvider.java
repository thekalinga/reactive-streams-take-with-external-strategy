package com.acme.reactivestreams;

/**
 * NOTE: Implementations must be thread safe
 */
public interface TakeRequestBatchAmountProvider {
  /**
   * @param instantDownstreamDemand number of items downstream want from upstream at this instant (excluding previous demands), can be 0 if this is is not triggered by downstream rather because we already got the previous batch
   * @param cumulativeDownstreamSentAmount total number of items sent downstream in the past
   * @param cumulativeUpstreamRequestAmount total number of requests sent to upstream
   * @return new batch amount to request. return 0 if you don't want any more items from upstream
   */
  long getNextBatchAmount(long instantDownstreamDemand, long cumulativeDownstreamSentAmount,
      long cumulativeUpstreamRequestAmount);
}
