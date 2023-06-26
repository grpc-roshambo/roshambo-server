package com.grpcroshambo.roshamboserver.entity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends CrudRepository<Match, String> {
    List<Match> findByFinishedIsFalse();
}
