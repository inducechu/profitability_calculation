package com.induce.investmentservice.kafka

import com.induce.common.proto.cbr.MacroIndicatorsProtoEvent
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class CbrEventListener(
    private val cacheManager: CacheManager
) {
    private val logger = LoggerFactory.getLogger(CbrEventListener::class.java)

    companion object {
        const val TOPIC_MACRO_INDICATORS = "macro-indicators-topic"
    }

    @Suppress("unused")
    @KafkaListener(
        topics = [TOPIC_MACRO_INDICATORS],
        groupId = "investment-service-group"
    )
    fun handleMacroIndicatorsUpdated(payload: ByteArray) {
        runCatching {
            MacroIndicatorsProtoEvent.parseFrom(payload)
        }.onSuccess { event ->
            logger.info("Получено асинхронное Protobuf-событие из Кафки за дату: ${event.recordDate}")
            logger.info("Новая инфляция: ${event.inflationValue}%, Новая ключевая ставка: ${event.keyRate}%")

            val inflationCache = cacheManager.getCache("inflationRate")

            if (inflationCache != null) {
                inflationCache.evict("current")
                logger.info("Кэш 'inflationRate::current' успешно сброшен в Redis. Сигнал к обновлению принят.")

                inflationCache.put("current", java.math.BigDecimal(event.inflationValue))

            } else {
                logger.warn("Кэш с именем 'inflationRate' не найден в конфигурации!")
            }
        }.onFailure { ex ->
            logger.error("Ошибка при десериализации Protobuf-события из Кафки: ${ex.message}", ex)
        }
    }
}
