package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonPage;
import java.time.Duration;
import java.util.function.LongSupplier;

public class CachingPokemonGateway implements PokemonGateway {

    private final PokemonGateway delegate;
    private final Duration ttl;
    private final long maxSize;
    private final LongSupplier nanoClock;

    public CachingPokemonGateway(
            PokemonGateway delegate,
            Duration ttl,
            long maxSize,
            LongSupplier nanoClock) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.nanoClock = nanoClock;
    }

    @Override
    public PokemonPage findPage(int page, int size) {
        return delegate.findPage(page, size);
    }

    @Override
    public PokemonDetail findById(int id) {
        return delegate.findById(id);
    }
}
