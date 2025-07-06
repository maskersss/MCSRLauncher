package com.redlimerl.mcsrlauncher.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ISO8601Serializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ISO8601Date", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Date {
        return Date.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(decoder.decodeString()) { Instant.from(it) })
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.toInstant().atZone(ZoneId.systemDefault())))
    }
}