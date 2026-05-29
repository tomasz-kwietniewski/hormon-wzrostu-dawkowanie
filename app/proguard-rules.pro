# Reguły kotlinx.serialization — zachowaj generowane serializery klas @Serializable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class pl.hormonwzrostu.data.** {
    *** Companion;
}
-keepclasseswithmembers class pl.hormonwzrostu.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
