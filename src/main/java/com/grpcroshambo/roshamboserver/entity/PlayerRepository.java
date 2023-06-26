package com.grpcroshambo.roshamboserver.entity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends CrudRepository<Player, String> {
    Optional<Player> findByName(String name);

    Iterable<Player> findAllByActiveIsTrue();
}
