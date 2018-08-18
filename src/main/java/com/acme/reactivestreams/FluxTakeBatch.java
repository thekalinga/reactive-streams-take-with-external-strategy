package com.acme.reactivestreams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;

public class FluxTakeBatch<T> extends FluxOperator<T, T> {
  private final TakeRequestBatchAmountProvider requestBatchAmountProvider;
  private final InfiniteStreamContinuationSwitch infiniteStreamContinuationSwitch;

  public FluxTakeBatch(Flux<? extends T> source, TakeRequestBatchAmountProvider requestBatchAmountProvider, @Nullable InfiniteStreamContinuationSwitch infiniteStreamContinuationSwitch) {
    super(source);
    this.requestBatchAmountProvider = requestBatchAmountProvider;
    this.infiniteStreamContinuationSwitch = infiniteStreamContinuationSwitch;
  }

  @Override
  public void subscribe(@NotNull CoreSubscriber<? super T> actual) {
    source.subscribe(new TakeSubscriber<T>(actual, requestBatchAmountProvider, infiniteStreamContinuationSwitch));
  }
}
