package com.acme.reactivestreams;

public class HighLowTideForeverBatchAmountProvider implements TakeRequestBatchAmountProvider {

  private final long highTide;
  private final long lowTide;

  public HighLowTideForeverBatchAmountProvider(long highTide, long lowTide) {
    this.highTide = highTide;
    this.lowTide = lowTide;
  }

  @Override
  public long getNextBatchAmount(long instantDownstreamDemand, long cumulativeDownstreamAmount, long cumulativeUpstreamRequestAmount) {
    return cumulativeUpstreamRequestAmount == 0 ? highTide : lowTide;
  }

}
