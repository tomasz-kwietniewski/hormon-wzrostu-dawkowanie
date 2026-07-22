# --- kotlinx.serialization ---
# Biblioteka dostarcza własne reguły consumer, wystarczające dla klas @Serializable
# BEZ nazwanych companion objectów (nasze Backup/Schedule takich nie mają). Poniżej
# dokładamy twarde zabezpieczenie modelu danych apki, bo backupy to dane medyczne.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Model danych serializowany do JSON (backupy + SharedPreferences: Backup, Schedule,
# InjectionSite itd.). Klasy, pola i generowane $$serializery muszą przetrwać R8 bez
# zmian - inaczej zapis/odczyt danych lub import starszej kopii mógłby się zepsuć.
-keep class pl.hormonwzrostu.data.** { *; }
-keepclassmembers class pl.hormonwzrostu.data.** {
    *** Companion;
}
-keepclasseswithmembers class pl.hormonwzrostu.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
