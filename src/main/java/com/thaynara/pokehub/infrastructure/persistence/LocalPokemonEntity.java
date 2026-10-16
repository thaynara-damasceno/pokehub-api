package com.thaynara.pokehub.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "local_pokemon")
public class LocalPokemonEntity {

    @Id
    private Integer id;

    private String name;

    private String imageUrl;

    @Column(length = 2000)
    private String description;

    private String localizedName;

    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @jakarta.persistence.CollectionTable(
            name = "local_pokemon_tags",
            joinColumns = @jakarta.persistence.JoinColumn(name = "local_pokemon_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    protected LocalPokemonEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
