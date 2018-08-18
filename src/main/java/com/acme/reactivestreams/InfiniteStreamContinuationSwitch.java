package com.acme.reactivestreams;

/**
 * NOTE: Implementations must be thread safe
 */
public interface InfiniteStreamContinuationSwitch {
  /**
   * @param cumulativeDownstreamSentAmount total number of items sent downstream in the past
   * @return num of additional items to send downstream from now. Returning 0 will cancel the upstream
   */
  long getAdditionalDownstreamAmount(long cumulativeDownstreamSentAmount);
}
