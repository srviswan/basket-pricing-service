package com.srviswan.basketpricing.marketdata;

import java.util.Collection;
import java.util.Map;

public interface MarketDataProvider {
    Map<String, PriceSnapshot> getLatestPrices(Collection<String> symbols);
    void subscribe(Collection<String> symbols);
}


