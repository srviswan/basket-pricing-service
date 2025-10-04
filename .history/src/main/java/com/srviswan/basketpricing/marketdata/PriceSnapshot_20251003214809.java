package com.srviswan.basketpricing.marketdata;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PriceSnapshot {
    String symbol;
    Double bid;
    Double ask;
    Double last;
    Instant timestamp;
}


