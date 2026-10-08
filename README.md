# Cubo 3x3

Aplicativo Android offline, em portugues, para estudo e pratica do cubo 3x3 e
do metodo CFOP.

## Recursos principais

- catalogos auditados de F2L, OLL e PLL;
- diagramas gerados pelo modelo proprio de 54 adesivos;
- reproducao animada de algoritmos e controle de velocidade;
- formula de resolucao e preparacao de cada caso;
- editor visual e calculo offline para preparar casos CFOP;
- cronometro com embaralhamentos 3x3, inspecao opcional e medias;
- planificacao 2D do embaralhamento;
- historico, sessoes, comentarios, penalidades e analise de tempos;
- treino dirigido, revisao espacada e comparacao de casos confundidos;
- quizzes de notacao e reconhecimento CFOP;
- importacao e exportacao de tempos no formato JSON do csTimer;
- backup local completo pelo seletor de documentos do Android;
- lembretes e metas locais opcionais.

O aplicativo nao solicita permissao de Internet e nao inclui anuncios,
analytics, rastreamento, compras ou assinatura.

## Requisitos de desenvolvimento

- JDK 17;
- Android SDK 36.1;
- Android Studio compativel com Android Gradle Plugin 9.4.0;
- Gradle Wrapper 9.6.0, incluido no projeto.

## Compilacao e testes

No PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease
```

O APK debug e destinado a desenvolvimento. Publicar ou distribuir uma release
exige assinatura propria, validacao no aparelho e revisao dos avisos de
terceiros.

## Documentacao

- [Formato do backup](BACKUP_FORMAT.md)
- [Portabilidade csTimer](PORTABILITY_FORMAT.md)
- [Preparacao de casos](CASE_PREPARATION.md)
- [Reconhecimento CFOP](CFOP_RECOGNITION.md)
- [Revisao espacada](CFOP_SPACED_REVIEW.md)
- [Plano de treino](TRAINING_PLAN.md)
- [Como contribuir](CONTRIBUTING.md)
- [Seguranca](SECURITY.md)
- [Componentes de terceiros](THIRD_PARTY_NOTICES.md)

## TNoodle e competicoes

O projeto usa `scrambler-min2phase` do TNoodle para recursos 3x3 offline. O
gerador incluido no aplicativo nao substitui a versao oficial atual exigida em
competicoes WCA. Consulte `THIRD_PARTY_NOTICES.md`.

## Licenca

Este projeto e distribuido sob a GNU General Public License v3.0 somente
(`GPL-3.0-only`). Consulte [LICENSE](LICENSE).
