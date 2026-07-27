# release 빌드 축소(minify) 규칙. 현재 isMinifyEnabled = false 라서 적용되지 않지만,
# build.gradle.kts 가 이 파일을 참조하므로 존재해야 한다.
# 나중에 minify 를 켤 때 아래 규칙들이 필요해진다.

# --- Retrofit ---
# 인터페이스의 제네릭 반환 타입(ApiEnvelope<T>)이 지워지면 파싱이 깨진다.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface <1>

# --- kotlinx.serialization ---
# @Serializable 클래스는 컴파일 시 생성된 serializer 를 리플렉션으로 찾으므로 보존한다.
-keepattributes *Annotation*
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
