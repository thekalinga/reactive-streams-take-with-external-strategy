package com.acme.reactivestreams;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class FluxWrapper<T> {

  private final Flux<T> source;

  public FluxWrapper(Flux<T> source) {
    this.source = source;
  }

  public Flux<T> batch(TakeRequestBatchAmountProvider strategy) {
    return new FluxTakeBatch<>(source, strategy, null);
  }

  public Flux<T> batch(TakeRequestBatchAmountProvider requestBatchAmountProvider, InfiniteStreamContinuationSwitch infiniteStreamContinuationSwitch) {
    return new FluxTakeBatch<>(source, requestBatchAmountProvider, infiniteStreamContinuationSwitch);
  }

  public <U extends TakeRequestBatchAmountProvider & InfiniteStreamContinuationSwitch> Flux<T> batchUnified(U merged) {
    return new FluxTakeBatch<>(source, merged, merged);
  }

  public static <T> FluxWrapper<T> wrap(Flux<T> source) {
    return new FluxWrapper<>(source);
  }

}
