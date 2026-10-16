package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalPokemonController {

    private final SyncPokemonUseCase syncPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final ListLocalPokemonUseCase listLocalPokemonUseCase;

    public LocalPokemonController(
            SyncPokemonUseCase syncPokemonUseCase,
            GetLocalPokemonUseCase getLocalPokemonUseCase,
            ListLocalPokemonUseCase listLocalPokemonUseCase) {
        this.syncPokemonUseCase = syncPokemonUseCase;
        this.getLocalPokemonUseCase = getLocalPokemonUseCase;
        this.listLocalPokemonUseCase = listLocalPokemonUseCase;
    }

    @PostMapping("/api/pokemon/{id}/sync")
    public LocalPokemonResponse sync(@PathVariable int id) {
        return LocalPokemonResponse.from(syncPokemonUseCase.sync(id));
    }

    @GetMapping("/api/local-pokemon")
    public List<LocalPokemonResponse> list() {
        return listLocalPokemonUseCase.list().stream()
                .map(LocalPokemonResponse::from)
                .toList();
    }

    @GetMapping("/api/local-pokemon/{id}")
    public LocalPokemonResponse getById(@PathVariable int id) {
        return LocalPokemonResponse.from(getLocalPokemonUseCase.getById(id));
    }
}
