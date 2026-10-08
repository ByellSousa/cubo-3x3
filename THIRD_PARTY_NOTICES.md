# Componentes de terceiros

Este arquivo resume os principais componentes distribuidos no APK. Os textos
integrais das licencas aplicaveis devem acompanhar toda distribuicao publica.
O codigo do aplicativo e distribuido sob `GPL-3.0-only`; consulte `LICENSE`.

## TNoodle `scrambler-min2phase` 0.20.0

- Projeto: TNoodle-LIB, World Cube Association Software Team.
- Artefato: `org.worldcubeassociation.tnoodle:scrambler-min2phase:0.20.0`.
- Origem: https://github.com/thewca/tnoodle-lib
- Licença declarada pelo projeto: GNU General Public License v3.0.
- Uso neste aplicativo: gerar estados aleatórios de cubo 3x3, calcular sequências de embaralhamento e resolver a transformação relativa entre um cubo informado e um caso CFOP para treino pessoal offline. O codec geométrico, editor, composição atual/alvo, conferência e interface são próprios; nenhum código-fonte do solver foi copiado.

O gerador incluído no aplicativo não substitui a versão oficial atual do programa TNoodle exigida em competições WCA. Competições oficiais devem usar o programa disponibilizado pela própria WCA.

## AndroidX e Jetpack Compose

- Componentes: Core, Activity, DataStore, Room, Lifecycle, SavedState, SQLite,
  Compose UI, Foundation, Material 3 e dependencias AndroidX transitivas.
- Origem: https://android.googlesource.com/platform/frameworks/support/
- Licenca predominante declarada nos POMs resolvidos: Apache License 2.0.
- Excecao observada: o modulo external protobuf do DataStore declara
  BSD-3-Clause.
- Uso: interface, ciclo de vida, persistencia local e integracao Android.

## Kotlin e bibliotecas JetBrains

- Componentes: Kotlin stdlib 2.4.20, kotlinx.coroutines 1.9.0,
  kotlinx.serialization 1.8.1 e annotations 23.0.0.
- Origens: https://github.com/JetBrains/kotlin,
  https://github.com/Kotlin/kotlinx.coroutines e
  https://github.com/Kotlin/kotlinx.serialization.
- Licenca declarada: Apache License 2.0.
- Uso: linguagem/runtime, concorrencia e serializacao.

## Gson 2.11.0

- Projeto: Gson, Google.
- Origem: https://github.com/google/gson
- Licenca declarada: Apache License 2.0.
- Uso: codec dos formatos locais de backup e portabilidade.

## Okio 3.9.1

- Projeto: Okio, Square.
- Origem: https://github.com/square/okio
- Licenca declarada: Apache License 2.0.
- Uso: dependencia transitiva do DataStore.

## Anotacoes transitivas

- Error Prone annotations 2.27.0: Apache License 2.0.
- JSpecify 1.0.0: Apache License 2.0.
- Guava `listenablefuture` 1.0: Apache License 2.0. O POM local nao trazia o
  campo de licenca, mas a origem oficial confirma que o artefato separado
  pertence ao projeto Guava sob Apache-2.0.

## Escopo da auditoria

As versoes acima foram obtidas da resolucao offline do
`debugRuntimeClasspath` em 2026-10-08. Dependencias apenas de build e teste nao
sao distribuidas no APK, mas continuam sujeitas as licencas de seus projetos.
Uma nova auditoria deve ser feita sempre que as dependencias forem alteradas.
