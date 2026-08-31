# kotlinx.serialization：@Serializable 資料類別（Card.kt／DataUpdater.kt／
# AnnouncementCenter.kt 裡那些）背後靠 compiler plugin 生成 $serializer 類別做
# JSON 轉換，R8 從呼叫圖看不出這些生成類別被誰用到，放著不管會被當成沒用到砍掉——
# release 版會直接連卡表 JSON 都解不開。
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class com.mark.wsdeck.data.**$$serializer {
    *** serializer(...);
}
-keepclassmembers class com.mark.wsdeck.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.mark.wsdeck.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
