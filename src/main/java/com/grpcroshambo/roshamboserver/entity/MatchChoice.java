package com.grpcroshambo.roshamboserver.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Getter
@Setter
@Entity
public class MatchChoice {
    @Id
    @Column
    private String id;

    @OneToOne
    private Match match;

    @OneToOne
    private Player player;

    @Column
    private Choice choice;
}
