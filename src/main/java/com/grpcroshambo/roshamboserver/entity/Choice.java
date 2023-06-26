package com.grpcroshambo.roshamboserver.entity;

public enum Choice {
  SURRENDER(0),
  ROCK(1),
  PAPER(2),
  SCISSORS(3),
  UNRECOGNIZED(-1);

  private final int value;

  Choice(int value) {
    this.value = value;
  }
}

