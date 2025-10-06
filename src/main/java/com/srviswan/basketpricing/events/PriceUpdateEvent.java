package com.srviswan.basketpricing.events;

import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a price update occurs
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PriceUpdateEvent extends ApplicationEvent {
    private final String symbol;
    private final PriceSnapshot snapshot;
    
    public PriceUpdateEvent(Object source, String symbol, PriceSnapshot snapshot) {
        super(source);
        this.symbol = symbol;
        this.snapshot = snapshot;
    }
}
