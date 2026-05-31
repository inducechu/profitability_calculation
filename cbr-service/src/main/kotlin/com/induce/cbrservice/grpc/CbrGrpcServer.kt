package com.induce.cbrservice.grpc

import com.induce.cbrservice.repository.MacroIndicatorRepository
import com.induce.common.proto.cbr.CbrGrpcServiceGrpc
import com.induce.common.proto.cbr.InflationRequest
import com.induce.common.proto.cbr.InflationResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Service

@Service
class CbrGrpcServer(
    private val repository: MacroIndicatorRepository
) : CbrGrpcServiceGrpc.CbrGrpcServiceImplBase() {

    override fun getInflationRate(
        request: InflationRequest,
        responseObserver: StreamObserver<InflationResponse>
    ) {
        println("Получен gRPC-запрос текущей инфляции от investment-service")

        runCatching {
            repository.findFirstByOrderByRecordDateDesc()
        }.onSuccess { indicator ->
            indicator?.let {
                val response = InflationResponse.newBuilder()
                    .setInflationRate(it.inflationValue.toString())
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } ?: responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Данные об инфляции отсутствуют в базе cbr-service")
                    .asRuntimeException()
            )
        }.onFailure { ex ->
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Внутренняя ошибка сервера при чтении БД")
                    .withCause(ex)
                    .asRuntimeException()
            )
        }
    }
}
