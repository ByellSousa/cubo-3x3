# O codec usa reflexao do Gson. Manter os nomes e campos garante que backups
# .3x3backup continuem compativeis no APK otimizado de release.
-keep class com.gabs.cubo3x3.data.backup.** { *; }
