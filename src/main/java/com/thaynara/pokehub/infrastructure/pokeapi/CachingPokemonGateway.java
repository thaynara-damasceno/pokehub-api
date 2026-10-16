package com.thaynara.pokehub.infrastructure.pokeapi;

import com.thaynara.pokehub.application.port.out.PokemonGateway;
import com.thaynara.pokehub.domain.model.PokemonDetail;
import com.thaynara.pokehub.domain.model.PokemonPage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.function.LongSupplier;

public class CachingPokemonGateway implements PokemonGateway {

    private final PokemonGateway delegate;
    private final Cache<PageKey, PokemonPage> pages;
    private final Cache<Integer, PokemonDetail> details;

    public CachingPokemonGateway(
            PokemonGateway delegate,
            Duration ttl,
            long maxSize,
            LongSupplier nanoClock) {
        this.delegate = delegate;
        this.pages = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .ticker(nanoClock::getAsLong)
                .build();
        this.details = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .ticker(nanoClock::getAsLong)
                .build();
    }

    @Override
    public PokemonPage findPage(int page, int size) {
        PageKey key = new PageKey(page, size);
        return pages.get(key, k -> delegate.findPage(k.page(), k.size()));
    }

    @Override
    public PokemonDetail findById(int id) {
        return details.get(id, delegate::findById);
    }

    private record PageKey(int page, int size) {
    }
}
