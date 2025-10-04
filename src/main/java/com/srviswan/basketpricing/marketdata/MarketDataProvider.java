package com.srviswan.basketpricing.marketdata;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MarketDataProvider {
    Map<String, PriceSnapshot> getLatestPrices(Collection<String> symbols);
    void subscribe(Collection<String> symbols);
    void unsubscribe(Collection<String> symbols);
    Set<String> getSubscribedSymbols();
}


