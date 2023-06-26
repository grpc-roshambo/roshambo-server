package com.grpcroshambo.roshamboserver.boundary;

import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.grpcroshambo.roshambo.*;
import com.grpcroshambo.roshamboserver.entity.MatchChoice;
import com.grpcroshambo.roshamboserver.entity.*;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Streamable;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

@GrpcService
public class RoshamboService extends RoshamboServiceGrpc.RoshamboServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(RoshamboService.class.getName());
    private static final int[][] resultTable = {
            {
                    Result.TIE_VALUE, Result.LOSE_VALUE, Result.LOSE_VALUE, Result.LOSE_VALUE
            },
            {
                    Result.WIN_VALUE, Result.TIE_VALUE, Result.LOSE_VALUE, Result.WIN_VALUE
            },
            {
                    Result.WIN_VALUE, Result.WIN_VALUE, Result.TIE_VALUE, Result.LOSE_VALUE
            },
            {
                    Result.WIN_VALUE, Result.LOSE_VALUE, Result.WIN_VALUE, Result.TIE_VALUE
            }
    };

    final HashMap<Long, StreamObserver<MatchRequestsFromServer>> playerObservers = new HashMap<>();
    final HashMap<String, StreamObserver<MatchResult>> matchObservers = new HashMap<>();
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchChoiceRepository matchChoiceRepository;

    public RoshamboService(PlayerRepository playerRepository, MatchRepository matchRepository, MatchChoiceRepository matchChoiceRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.matchChoiceRepository = matchChoiceRepository;
    }

    @Override
    public void join(JoinRequest joinRequest, StreamObserver<MatchRequestsFromServer> responseObserver) {
        final var name = joinRequest.getName();
        logger.info("join - {} wants to join", name);
        Optional<Player> existingPlayer = playerRepository.findByName(name);
        if (existingPlayer.isPresent() && existingPlayer.get().getActive()) {
            logger.error("join - {} already joined", name);
            com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS.getNumber())
                    .setMessage("Name already exists")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .setReason("Invalid Name")
                            .setDomain("com.devilopa.roshambo.roshamboserver")
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }
        Player player = existingPlayer.orElseGet(() -> {
            Player newPlayer = new Player();
            newPlayer.setName(name);
            return newPlayer;
        });
        player.setActive(true);
        playerRepository.save(player);
        logger.info("join - {} put in game", name);
        playerObservers.put(player.getId(), responseObserver);
    }

    @Override
    public void play(com.grpcroshambo.roshambo.MatchChoice request, StreamObserver<MatchResult> responseObserver) {
        final var matchChoiceOptional = matchChoiceRepository.findById(request.getMatchToken());
        if (matchChoiceOptional.isEmpty()) {
            logger.error("play - MatchToken {} does not exists", request.getMatchToken());
            com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                    .setCode(Code.NOT_FOUND.getNumber())
                    .setMessage("MatchToken does not exists")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .setReason("MatchToken does not exists")
                            .setDomain("com.devilopa.roshambo.roshamboserver")
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }
        if (matchObservers.containsKey(request.getMatchToken())) {
            logger.error("play - MatchToken {} already used", request.getMatchToken());
            com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS.getNumber())
                    .setMessage("MatchToken already used")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .setReason("Already used MatchToken")
                            .setDomain("com.devilopa.roshambo.roshamboserver")
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }
        final var matchChoice = matchChoiceOptional.get();
        matchChoice.setChoice(request.getChoice());
        matchObservers.put(request.getMatchToken(), responseObserver);
        matchChoiceRepository.save(matchChoice);
    }

    @Scheduled(cron = "0/10 * * * * *")
    public void startRound() {
        logger.info("Start round");
        final var players = new ArrayList<>(Streamable.of(playerRepository.findAllByActiveIsTrue()).toList());
        Collections.shuffle(players);
        logger.info("Shuffled " + players.size() + " active players.");
        for (int i = 0; i < players.size() - 1; i += 2) {
            final var match = new Match();
            final var matchChoices = new ArrayList<MatchChoice>();
            match.setMatchChoices(matchChoices);
            matchRepository.save(match);
            final var player1 = players.get(i);
            final var player2 = players.get(i + 1);

            try {
                askPlayerForMatch(match, player1, player2);
            } catch (Exception e) {
                logger.warn("Exception occurred while asking player1=" + player1.getName() + " for match. Will inactive him.");
                player1.setActive(false);
                playerRepository.save(player1);
                i++;
                continue;
            }
            try {
                askPlayerForMatch(match, player2, player1);
            } catch (Exception e) {
                logger.warn("Exception occurred while asking player2=" + player2.getName() + " for match. Will inactive him.");
                player2.setActive(false);
                playerRepository.save(player2);
            }
            matchRepository.save(match);

            logger.info("Started match " + match.getId());
        }
    }

    private void askPlayerForMatch(Match match, Player player, Player opponent) {
        final var matchChoice = new MatchChoice();
        matchChoice.setId(UUID.randomUUID().toString());
        matchChoice.setPlayer(player);
        matchChoice.setMatch(match);
        matchChoice.setChoice(Choice.SURRENDER);
        matchChoiceRepository.save(matchChoice);
        match.getMatchChoices().add(matchChoice);

        final var player1MatchRequest = MatchRequestsFromServer.newBuilder()
                .setMatchToken(matchChoice.getId())
                .setOpponentName(opponent.getName())
                .setOpponentLastChoice(Choice.ROCK)
                .build();
        playerObservers.get(player.getId()).onNext(player1MatchRequest);
    }

    @Scheduled(cron = "5/10 * * * * *")
    public void finishRound() {
        logger.info("Finish round");
        final var unfinishedMatches = matchRepository.findByFinishedIsFalse();
        for (var unfinishedMatch : unfinishedMatches) {
            try {
                final var matchChoicePlayer1 = unfinishedMatch.getMatchChoices().get(0);
                final var matchChoicePlayer2 = unfinishedMatch.getMatchChoices().get(1);

                logger.info("Match " + matchChoicePlayer1.getPlayer().getName() + " with " + matchChoicePlayer1.getChoice() + " vs. " + matchChoicePlayer2.getPlayer().getName() + " with " + matchChoicePlayer2.getChoice());
                informFirstPlayer(matchChoicePlayer1, matchChoicePlayer2);
                informFirstPlayer(matchChoicePlayer2, matchChoicePlayer1);
            } catch (Exception e) {
                logger.error("Exception occurred while finishing match " + unfinishedMatch.getId(), e);
            } finally {
                unfinishedMatch.setFinished(true);
                logger.info("Finished match " + unfinishedMatch.getId());
                matchRepository.save(unfinishedMatch);
            }
        }
    }

    private void informFirstPlayer(MatchChoice matchChoicePlayer1, MatchChoice matchChoicePlayer2) {
        final var result = Result.forNumber(resultTable[matchChoicePlayer1.getChoice().getNumber()][matchChoicePlayer2.getChoice().getNumber()]);
        final var matchResultPlayer1 = MatchResult
                .newBuilder()
                .setMatchToken(matchChoicePlayer1.getId())
                .setResult(result)
                .setOpponentChoice(matchChoicePlayer2.getChoice())
                .build();
        matchObservers.get(matchChoicePlayer1.getId()).onNext(matchResultPlayer1);
        matchObservers.get(matchChoicePlayer1.getId()).onCompleted();
        matchObservers.remove(matchChoicePlayer1.getId());
        logger.info(matchChoicePlayer1.getPlayer().getName() + " informed about result=" + matchResultPlayer1.getResult());
    }
}
