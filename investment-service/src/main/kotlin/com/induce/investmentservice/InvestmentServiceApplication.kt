package com.induce.investmentservice

import com.induce.common.proto.cbr.CbrGrpcServiceGrpc
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.grpc.client.ImportGrpcClients

@SpringBootApplication
@ImportGrpcClients(basePackageClasses = [CbrGrpcServiceGrpc::class])
@EnableCaching
class InvestmentServiceApplication

fun main(args: Array<String>) {
    runApplication<InvestmentServiceApplication>(*args)
}
