package com.grpcroshambo.roshamboserver.boundary;

import com.grpcroshambo.roshamboserver.entity.MatchChoiceRepository;
import com.grpcroshambo.roshamboserver.entity.MatchRepository;
import com.grpcroshambo.roshamboserver.entity.PlayerRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RoshamboServiceTest {

    @SpyBean
    private PlayerRepository playerRepository;
    @SpyBean
    private MatchRepository matchRepository;
    @SpyBean
    private MatchChoiceRepository matchChoiceRepository;

}