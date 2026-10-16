package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.exception.PokemonProviderUnavailableException;
import com.thaynara.pokehub.domain.model.Pokemon;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingPokemonGatewayTest {

    private static final Duration TTL = Duration.ofSeconds(60);
    private static final long MAX_SIZE = 100;

    @Test
    void returnsTheCachedPageForTheSamePageAndSize() {
        PokemonGateway delegate = mock(PokemonGateway.class);
        AtomicLong clock = new AtomicLong();
        PokemonPage expected = page(0);
        when(delegate.findPage(0, 20)).thenReturn(expected);
        CachingPokemonGateway gateway = gateway(delegate, clock);

        PokemonPage first = gateway.findPage(0, 20);
        PokemonPage second = gateway.findPage(0, 20);

        assertThat(first).isSameAs(expected);
        assertThat(second).isSameAs(expected);
        verify(delegate, times(1)).findPage(0, 20);
    }

    @Test
    void returnsTheCachedDetailForTheSameId() {
        PokemonGateway delegate = mock(PokemonGateway.class);
        AtomicLong clock = new AtomicLong();
        PokemonDetail expected = detail(25);
        when(delegate.findById(25)).thenReturn(expected);
        CachingPokemonGateway gateway = gateway(delegate, clock);

        PokemonDetail first = gateway.findById(25);
        PokemonDetail second = gateway.findById(25);

        assertThat(first).isSameAs(expected);
        assertThat(second).isSameAs(expected);
        verify(delegate, times(1)).findById(25);
    }

    @Test
    void cachesDifferentKeysSeparately() {
        PokemonGateway delegate = mock(PokemonGateway.class);
        AtomicLong clock = new AtomicLong();
        PokemonPage firstPage = page(0);
        PokemonPage secondPage = page(1);
        PokemonDetail firstDetail = detail(25);
        PokemonDetail secondDetail = detail(133);
        when(delegate.findPage(0, 20)).thenReturn(firstPage);
        when(delegate.findPage(1, 20)).thenReturn(secondPage);
        when(delegate.findById(25)).thenReturn(firstDetail);
        when(delegate.findById(133)).thenReturn(secondDetail);
        CachingPokemonGateway gateway = gateway(delegate, clock);

        assertThat(gateway.findPage(0, 20)).isSameAs(firstPage);
        assertThat(gateway.findPage(1, 20)).isSameAs(secondPage);
        assertThat(gateway.findPage(0, 20)).isSameAs(firstPage);
        assertThat(gateway.findPage(1, 20)).isSameAs(secondPage);
        assertThat(gateway.findById(25)).isSameAs(firstDetail);
        assertThat(gateway.findById(133)).isSameAs(secondDetail);
        assertThat(gateway.findById(25)).isSameAs(firstDetail);
        assertThat(gateway.findById(133)).isSameAs(secondDetail);

        verify(delegate, times(1)).findPage(0, 20);
        verify(delegate, times(1)).findPage(1, 20);
        verify(delegate, times(1)).findById(25);
        verify(delegate, times(1)).findById(133);
    }

    @Test
    void doesNotCacheFailures() {
        PokemonGateway delegate = mock(PokemonGateway.class);
        AtomicLong clock = new AtomicLong();
        PokemonDetail expected = detail(25);
        when(delegate.findById(25))
                .thenThrow(new PokemonProviderUnavailableException("Unavailable"))
                .thenReturn(expected);
        CachingPokemonGateway gateway = gateway(delegate, clock);

        assertThatThrownBy(() -> gateway.findById(25))
                .isInstanceOf(PokemonProviderUnavailableException.class);
        assertThat(gateway.findById(25)).isSameAs(expected);

        verify(delegate, times(2)).findById(25);
    }

    @Test
    void reloadsAnEntryAfterTheTtlExpires() {
        PokemonGateway delegate = mock(PokemonGateway.class);
        AtomicLong clock = new AtomicLong();
        PokemonDetail firstValue = detail(25);
        PokemonDetail secondValue = detail(25);
        when(delegate.findById(25)).thenReturn(firstValue, secondValue);
        CachingPokemonGateway gateway = gateway(delegate, clock);

        assertThat(gateway.findById(25)).isSameAs(firstValue);
        clock.addAndGet(Duration.ofSeconds(59).toNanos());
        assertThat(gateway.findById(25)).isSameAs(firstValue);
        clock.addAndGet(Duration.ofSeconds(2).toNanos());
        assertThat(gateway.findById(25)).isSameAs(secondValue);

        verify(delegate, times(2)).findById(25);
    }

    private CachingPokemonGateway gateway(PokemonGateway delegate, AtomicLong clock) {
        return new CachingPokemonGateway(delegate, TTL, MAX_SIZE, clock::get);
    }

    private PokemonPage page(int page) {
        return new PokemonPage(List.of(), page, 20, 0);
    }

    private PokemonDetail detail(int id) {
        return new PokemonDetail(id, "pokemon-" + id, "", List.of(), "", List.of());
    }
}
