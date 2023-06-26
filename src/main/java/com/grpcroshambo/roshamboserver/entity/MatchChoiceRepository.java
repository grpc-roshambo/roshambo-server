package com.grpcroshambo.roshamboserver.entity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchChoiceRepository extends CrudRepository<MatchChoice, String> {
}
