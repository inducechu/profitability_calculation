package com.induce.cbrservice.kafka

import com.induce.common.proto.cbr.MacroIndicatorsProtoEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class CbrEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {
    companion object {
        const val TOPIC_MACRO_INDICATORS = "macro-indicators-topic"
    }

    fun sendIndicatorsUpdated(event: MacroIndicatorsProtoEvent) {
        val key = event.recordDate
        val payload: ByteArray = event.toByteArray()

        kafkaTemplate.send(TOPIC_MACRO_INDICATORS, key, payload)
            .whenComplete { _, ex ->
                if (ex == null) {
                    println("Событие (Protobuf) успешно отправлено в Кафку. Ключ: $key")
                } else {
                    println("Ошибка отправки в Кафку: ${ex.message}")
                }
            }
    }
}
