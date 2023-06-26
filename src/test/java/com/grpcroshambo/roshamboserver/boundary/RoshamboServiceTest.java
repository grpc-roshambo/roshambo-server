package com.grpcroshambo.roshamboserver.boundary;

import com.google.rpc.Code;
import com.grpcroshambo.roshambo.JoinRequest;
import com.grpcroshambo.roshambo.MatchRequestsFromServer;
import com.grpcroshambo.roshambo.RoshamboServiceGrpc;
import com.grpcroshambo.roshamboserver.entity.MatchChoiceRepository;
import com.grpcroshambo.roshamboserver.entity.MatchRepository;
import com.grpcroshambo.roshamboserver.entity.PlayerRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

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


    private void joinAsSmithAndExpectNoException() {
        var exceptionThrown = new AtomicBoolean(false);
        nonBlockingStub.join(JoinRequest.newBuilder().setName("Smith").build(), new StreamObserver<>() {
            @Override
            public void onNext(MatchRequestsFromServer matchRequestsFromServer) {
            }

            @Override
            public void onError(Throwable throwable) {
                exceptionThrown.set(true);
            }

            @Override
            public void onCompleted() {

            }
        });
        assertThat(exceptionThrown.get()).isFalse();
    }

    @Test
    public void join_validName_userId() {
        joinAsSmithAndExpectNoException();
    }

    @Test
    public void join_invalidName_errorCodeAlreadyExists() {
        joinAsSmithAndExpectNoException();

        var alreadyExistsExceptionThrown = new AtomicBoolean(false);
        nonBlockingStub.join(JoinRequest.newBuilder().setName("Smith").build(), new StreamObserver<>() {
            @Override
            public void onNext(MatchRequestsFromServer matchRequestsFromServer) {

            }

            @SneakyThrows
            @Override
            public void onError(Throwable throwable) {
                if (((StatusRuntimeException) throwable).getStatus().getCode().value() == Code.ALREADY_EXISTS.getNumber()) {
                    alreadyExistsExceptionThrown.set(true);
                }
            }

            @Override
            public void onCompleted() {

            }
        });
        assertThat(alreadyExistsExceptionThrown.get()).isTrue();
    }

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