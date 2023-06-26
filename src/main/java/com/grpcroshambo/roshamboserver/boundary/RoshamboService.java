package com.grpcroshambo.roshamboserver.boundary;

import com.grpcroshambo.roshamboserver.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Streamable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RoshamboService {
    private static final Logger logger = LoggerFactory.getLogger(RoshamboService.class.getName());
//    private static final int[][] resultTable = {
//            {
//                    Result.TIE_VALUE, Result.LOSE_VALUE, Result.LOSE_VALUE, Result.LOSE_VALUE
//            },
//            {
//                    Result.WIN_VALUE, Result.TIE_VALUE, Result.LOSE_VALUE, Result.WIN_VALUE
//            },
//            {
//                    Result.WIN_VALUE, Result.WIN_VALUE, Result.TIE_VALUE, Result.LOSE_VALUE
//            },
//            {
//                    Result.WIN_VALUE, Result.LOSE_VALUE, Result.WIN_VALUE, Result.TIE_VALUE
//            }
//    };

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchChoiceRepository matchChoiceRepository;

    public RoshamboService(PlayerRepository playerRepository, MatchRepository matchRepository, MatchChoiceRepository matchChoiceRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.matchChoiceRepository = matchChoiceRepository;
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

        // TODO: Send MatchRequest
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
        // final var result = Result.forNumber(resultTable[matchChoicePlayer1.getChoice().getNumber()][matchChoicePlayer2.getChoice().getNumber()]);
        // TODO: Calculate MatchResult send to player1
    }
}
