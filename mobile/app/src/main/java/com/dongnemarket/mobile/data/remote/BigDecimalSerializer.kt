package com.dongnemarket.mobile.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

/**
 * `price` 전용 시리얼라이저. kotlinx.serialization 에 BigDecimal 기본 지원이 없어서 직접 만든다.
 * **상품 말고 찜·채팅 DTO 도 이걸 공용으로 쓴다**(파일이 여기 하나뿐인 이유).
 *
 * 왜 BigDecimal 인가:
 * 서버 `price` 는 DB `decimal(38,2)` 라서 GET 응답이 `800000.00` 으로 오고,
 * POST/PATCH 응답은 요청 body 를 그대로 되돌려주므로 `800000` 으로 온다.
 * 같은 필드인데 스케일이 다르므로 `Int`/`Long` 으로 받으면 파싱이 깨지고,
 * `Double` 로 받으면 금액에 부동소수 오차가 섞인다.
 *
 * 구현 메모: descriptor 는 STRING 으로 선언하지만 실제 JSON 은 **따옴표 없는 숫자**다.
 * 그래서 읽을 때는 [JsonDecoder] 로 내려가 원문 토큰을 문자열로 꺼내 `BigDecimal(...)` 에 넣고,
 * 쓸 때는 [JsonEncoder] 로 숫자 그대로 내보낸다(문자열로 보내면 서버 Jackson 이 400 을 낼 수 있다).
 * JSON 이 아닌 포맷으로 쓰일 때를 위해 문자열 경로도 남겨 뒀다.
 *
 * 사용법:
 * ```
 * @Serializable(with = BigDecimalSerializer::class)
 * val price: BigDecimal
 * ```
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val json = decoder as? JsonDecoder ?: return BigDecimal(decoder.decodeString())
        return BigDecimal(json.decodeJsonElement().jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val json = encoder as? JsonEncoder
        if (json == null) {
            encoder.encodeString(value.toPlainString())
        } else {
            json.encodeJsonElement(JsonPrimitive(value as Number))
        }
    }
}
