# Como contribuir

Este projeto e licenciado sob `GPL-3.0-only` e esta sendo preparado para
abertura publica por uma exportacao sanitizada. Consulte `LICENSE`.

## Ambiente

- JDK 17;
- Android SDK com API 36.1 instalada;
- Android Studio compativel com AGP 9.4.0;
- Gradle Wrapper 9.6.0 incluido no repositorio.

Configure o SDK local pelo mecanismo padrao do Android Studio. Nao envie
`local.properties`, credenciais ou configuracoes pessoais.

## Validacao minima

No PowerShell, a partir da raiz do projeto:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease
```

Mudancas em banco, backup, parser de notacao, motor do cubo ou formula CFOP
devem incluir testes de regressao proporcionais ao risco. Uma build aprovada
localmente nao equivale a validacao visual no aparelho nem a publicacao.

## Regras do produto

- preserve a notacao informada pelo usuario, inclusive movimentos como `U2'`;
- nao normalize silenciosamente uma formula apenas porque duas representacoes
  produzem o mesmo estado;
- use o modelo proprio de 54 adesivos para diagramas e animacoes;
- mantenha o aplicativo offline, sem anuncios, analytics ou rastreamento;
- nao copie codigo, layout, identidade visual ou ativos de aplicativos usados
  apenas como referencia funcional;
- nao inclua screenshots, APKs de referencia ou material proprietario;
- mantenha chaves de assinatura e senhas fora do Git;
- nao inclua backups `.3x3backup`, bancos, tempos, comentarios ou preferencias
  reais de usuarios em testes ou exemplos.

## Pull requests

Uma contribuicao deve:

1. explicar o problema e o comportamento esperado;
2. manter o escopo pequeno e separar refatoracoes de mudancas funcionais;
3. listar os testes executados e o que ainda depende de aparelho;
4. atualizar a documentacao quando alterar um contrato duravel;
5. confirmar que nao adiciona segredo, dado pessoal ou material sem licenca.

O envio de uma contribuicao indica concordancia em disponibiliza-la sob
`GPL-3.0-only`, a mesma licenca do repositorio.

Falhas de seguranca nao devem ser abertas como issue publica. Consulte
`SECURITY.md`.
