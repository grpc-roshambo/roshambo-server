package com.grpcroshambo.roshamboserver.boundary;

import com.grpcroshambo.roshambo.RoshamboServiceGrpc;
import com.grpcroshambo.roshamboserver.entity.MatchChoiceRepository;
import com.grpcroshambo.roshamboserver.entity.MatchRepository;
import com.grpcroshambo.roshamboserver.entity.PlayerRepository;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
class RoshamboServiceTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private RoshamboServiceGrpc.RoshamboServiceStub nonBlockingStub;

    @SpyBean
    private PlayerRepository playerRepository;
    @SpyBean
    private MatchRepository matchRepository;
    @SpyBean
    private MatchChoiceRepository matchChoiceRepository;

    @BeforeEach
    public void startServer() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new RoshamboService(playerRepository, matchRepository, matchChoiceRepository))
                .build()
                .start());

        nonBlockingStub = RoshamboServiceGrpc.newStub(grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build()));
    }

    @BeforeEach
    public void resetRepositories() {
        matchChoiceRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
    }
}