package com.induce.cbrservice.service

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.induce.cbrservice.dto.MacroIndicatorData
import com.induce.cbrservice.dto.MacroIndicatorRecord
import com.induce.cbrservice.dto.MacroIndicatorResponse
import com.induce.cbrservice.dto.toResponse
import com.induce.cbrservice.exception.InvalidPeriodException
import com.induce.cbrservice.kafka.CbrEventProducer
import com.induce.cbrservice.model.MacroIndicator
import com.induce.cbrservice.repository.MacroIndicatorRepository
import com.induce.common.proto.cbr.MacroIndicatorsProtoEvent
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class CbrService(
    private val repository: MacroIndicatorRepository,
    private val restTemplate: RestTemplate,
    private val cbrInflationXmlUrl: String,
    private val cbrEventProducer: CbrEventProducer
) {
    private val xmlMapper = XmlMapper().registerKotlinModule()
    private val cbrDateFormatter = DateTimeFormatter.ofPattern("MM.yyyy")

    fun getLatestIndicators(): MacroIndicatorResponse {
        val entity = repository.findFirstByOrderByRecordDateDesc()
            ?: throw NoSuchElementException("Данные в базе отсутствуют")

        return entity.toResponse()
    }

    fun getDataForPeriod(start: LocalDate, end: LocalDate): List<MacroIndicatorResponse> {

        if (start.isAfter(end)) {
            throw InvalidPeriodException("Начальная дата не может быть позже конечной.")
        }

        val entities = repository.findAllByRecordDateBetweenOrderByRecordDateAsc(start, end)

        return entities.map { it.toResponse() }
    }

    fun refreshMacroIndicators(dateFrom: LocalDate, dateTo: LocalDate) {
        val now = LocalDate.now()

        if (dateFrom.isAfter(now)) {
            throw InvalidPeriodException("Дата начала ($dateFrom) не может быть в будущем.")
        }

        if (dateFrom.isAfter(dateTo)) {
            throw InvalidPeriodException("Дата начала не может быть позже даты окончания.")
        }

        val rawXml = getInflationXml(
            dateFrom.format(DateTimeFormatter.ISO_DATE),
            dateTo.format(DateTimeFormatter.ISO_DATE)
        )
        if (rawXml.isBlank()) {
            println("Предупреждение: Получен пустой ответ от ЦБ.")
            return
        }

        try {
            val records = parseInflationXml(rawXml)

            if (records.isEmpty()) {
                println("Данные за период с $dateFrom по $dateTo пока отсутствуют в ЦБ.")
                return
            }

            val savedEntities = saveNewRecords(records)

            savedEntities.forEach { entity ->
                val protoEvent = MacroIndicatorsProtoEvent.newBuilder()
                    .setRecordDate(entity.recordDate.toString())
                    .setKeyRate(entity.keyRate)
                    .setInflationValue(entity.inflationValue)
                    .setTargetValue(entity.targetValue)
                    .build()

                cbrEventProducer.sendIndicatorsUpdated(protoEvent)
            }
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при обработке данных ЦБ: ${e.message}")
        }
    }

    private fun getInflationXml(dateFrom: String, dateTo: String): String {
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
              <soap12:Body>
                <InflationXML xmlns="http://web.cbr.ru/">
                  <DateFrom>$dateFrom</DateFrom>
                  <DateTo>$dateTo</DateTo>
                </InflationXML>
              </soap12:Body>
            </soap12:Envelope>
        """.trimIndent()

        val headers = HttpHeaders().apply {
            contentType = MediaType.valueOf("application/soap+xml; charset=utf-8")
        }

        return try {
            restTemplate.postForObject(cbrInflationXmlUrl, HttpEntity(soapBody, headers), String::class.java) ?: ""
        } catch (e: Exception) {
            println("Ошибка сети: ${e.message}")
            ""
        }
    }

    private fun parseInflationXml(xmlResponse: String): List<MacroIndicatorRecord> {
        if (xmlResponse.isBlank()) return emptyList()

        val resultXml = xmlResponse
            .substringAfter("<InflationXMLResult>")
            .substringBefore("</InflationXMLResult>")
            .replace("xmlns=\"\"", "")

        return xmlMapper.readValue(resultXml, MacroIndicatorData::class.java).records
    }

    @Transactional
    internal fun saveNewRecords(records: List<MacroIndicatorRecord>): List<MacroIndicator> {
        val entitiesToSave = records.mapNotNull { record ->
            val recordDate = YearMonth.parse(record.date, cbrDateFormatter).atDay(1)

            if (!repository.existsByRecordDate(recordDate)) {
                MacroIndicator(
                    recordDate = recordDate,
                    keyRate = record.keyRate,
                    inflationValue = record.inflationValue,
                    targetValue = record.targetValue
                )
            } else {
                null
            }
        }

        return if (entitiesToSave.isNotEmpty()) {
            val saved = repository.saveAll(entitiesToSave)
            println("БД обновлена. Добавлено записей: ${saved.size}")
            saved
        } else {
            println("Новых данных для сохранения не обнаружено.")
            emptyList()
        }
    }
}
