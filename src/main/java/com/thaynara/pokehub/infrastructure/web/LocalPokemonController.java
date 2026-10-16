package com.thaynara.pokehub.infrastructure.web;

import com.thaynara.pokehub.application.port.in.GetLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.ListLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.SyncPokemonUseCase;
import com.thaynara.pokehub.application.port.in.UpdateLocalPokemonUseCase;
import com.thaynara.pokehub.application.port.in.DeleteLocalPokemonUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class LocalPokemonController {

    private final SyncPokemonUseCase syncPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final ListLocalPokemonUseCase listLocalPokemonUseCase;
    private final UpdateLocalPokemonUseCase updateLocalPokemonUseCase;
    private final DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

    public LocalPokemonController(
            SyncPokemonUseCase syncPokemonUseCase,
            GetLocalPokemonUseCase getLocalPokemonUseCase,
            ListLocalPokemonUseCase listLocalPokemonUseCase,
            UpdateLocalPokemonUseCase updateLocalPokemonUseCase,
            DeleteLocalPokemonUseCase deleteLocalPokemonUseCase) {
        this.syncPokemonUseCase = syncPokemonUseCase;
        this.getLocalPokemonUseCase = getLocalPokemonUseCase;
        this.listLocalPokemonUseCase = listLocalPokemonUseCase;
        this.updateLocalPokemonUseCase = updateLocalPokemonUseCase;
        this.deleteLocalPokemonUseCase = deleteLocalPokemonUseCase;
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

    @PutMapping("/api/local-pokemon/{id}")
    public LocalPokemonResponse update(
            @PathVariable int id,
            @RequestBody UpdateLocalPokemonRequest request) {
        return LocalPokemonResponse.from(updateLocalPokemonUseCase.update(
                id, request.localizedName(), request.region(), request.tags()));
    }

    @DeleteMapping("/api/local-pokemon/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        deleteLocalPokemonUseCase.delete(id);
    }
}
