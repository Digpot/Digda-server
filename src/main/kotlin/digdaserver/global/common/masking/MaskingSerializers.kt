package digdaserver.global.common.masking

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * DTO 필드에 `@get:JsonSerialize(using = MaskedName::class)` 처럼 붙여 응답 직전에 마스킹한다.
 * DTO 를 어디서 만들든(정적 팩토리·서비스 조립) 직렬화 한 곳에서 빠짐없이 가려진다.
 * null 은 Jackson 이 이 직렬화기를 거치지 않고 그대로 null 로 쓴다.
 */
class MaskedName : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(PiiMasker.name(value))
    }
}

class MaskedEmail : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(PiiMasker.email(value))
    }
}
