package com.srviswan.basketpricing.marketdata;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PriceSnapshotTest {

    @Test
    void builder_ShouldCreatePriceSnapshot_WhenAllFieldsProvided() {
        // Given
        String symbol = "IBM.N";
        Double bid = 150.0;
        Double ask = 150.5;
        Double last = 150.25;
        Instant timestamp = Instant.now();

        // When
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .symbol(symbol)
                .bid(bid)
                .ask(ask)
                .last(last)
                .timestamp(timestamp)
                .build();

        // Then
        assertThat(snapshot.getSymbol()).isEqualTo(symbol);
        assertThat(snapshot.getBid()).isEqualTo(bid);
        assertThat(snapshot.getAsk()).isEqualTo(ask);
        assertThat(snapshot.getLast()).isEqualTo(last);
        assertThat(snapshot.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void builder_ShouldCreatePriceSnapshot_WhenSomeFieldsAreNull() {
        // Given
        String symbol = "IBM.N";
        Double bid = 150.0;
        Double ask = null;
        Double last = 150.25;
        Instant timestamp = Instant.now();

        // When
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .symbol(symbol)
                .bid(bid)
                .ask(ask)
                .last(last)
                .timestamp(timestamp)
                .build();

        // Then
        assertThat(snapshot.getSymbol()).isEqualTo(symbol);
        assertThat(snapshot.getBid()).isEqualTo(bid);
        assertThat(snapshot.getAsk()).isNull();
        assertThat(snapshot.getLast()).isEqualTo(last);
        assertThat(snapshot.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void equals_ShouldReturnTrue_WhenSnapshotsAreEqual() {
        // Given
        Instant timestamp = Instant.now();
        PriceSnapshot snapshot1 = PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(timestamp)
                .build();

        PriceSnapshot snapshot2 = PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(timestamp)
                .build();

        // When & Then
        assertThat(snapshot1).isEqualTo(snapshot2);
        assertThat(snapshot1.hashCode()).isEqualTo(snapshot2.hashCode());
    }

    @Test
    void equals_ShouldReturnFalse_WhenSnapshotsAreDifferent() {
        // Given
        Instant timestamp = Instant.now();
        PriceSnapshot snapshot1 = PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(timestamp)
                .build();

        PriceSnapshot snapshot2 = PriceSnapshot.builder()
                .symbol("MSFT.O")
                .bid(300.0)
                .ask(300.5)
                .last(300.25)
                .timestamp(timestamp)
                .build();

        // When & Then
        assertThat(snapshot1).isNotEqualTo(snapshot2);
        assertThat(snapshot1.hashCode()).isNotEqualTo(snapshot2.hashCode());
    }

    @Test
    void toString_ShouldContainAllFields_WhenCalled() {
        // Given
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(Instant.now())
                .build();

        // When
        String result = snapshot.toString();

        // Then
        assertThat(result).contains("IBM.N");
        assertThat(result).contains("150.0");
        assertThat(result).contains("150.5");
        assertThat(result).contains("150.25");
    }
}
