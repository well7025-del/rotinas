# Etiquetas por Voz

Aplicativo Android para imprimir etiquetas de pedidos falando no celular, em vez
de escrever à mão com pincel atômico.

A tela tem **dois botões**:

| Botão | O que faz |
|---|---|
| **GRAVAR** | Ouve o pedido em português e monta a etiqueta na hora |
| **IMPRIMIR** | Manda a etiqueta em A5 para a impressora ligada ao celular ou no Wi‑Fi |

Entre os dois botões fica a **prévia da folha**, exatamente como vai sair no
papel — é aí que se confere antes de imprimir.

## Como falar o pedido

Só o nome e a cidade:

> “pedido de Aline Soares Caruaru”

Nome, cidade e as peças:

> “pedido Wellington Oliveira, Palmares: 2 camisa sport G, 3 camisa São Paulo M”

Vira uma etiqueta assim:

```
PEDIDO                              07/08/2026
──────────────────────────────────────────────
WELLINGTON
OLIVEIRA
PALMARES
──────────────────────────────────────────────
2   Camisa                                   G
    Sport
3   Camisa São                               M
    Paulo
──────────────────────────────────────────────
TOTAL: 5 peças                          2 itens
```

O tamanho da letra é calculado a cada etiqueta para **ocupar o máximo da folha
A5**: quando o pedido tem poucos itens, a letra cresce; quando tem muitos, ela
diminui só o necessário para tudo caber. O nome do cliente é sempre o maior
texto da folha.

### O que o aplicativo entende

- **Quantidade** em número (“3 camisa”) ou por extenso (“três camisas”, “duas calças”).
- **Tamanho** no fim do item: `PP P M G GG XG U`, ou anunciado — “calça tamanho 42”.
- **Cidade** separada por vírgula (“Wellington Oliveira, Palmares”), pela lista de
  cidades do aplicativo (“Aline Soares Caruaru”), ou por “para”/“em”
  (“Rita Nunes para Xique‑Xique”).
- **Pontuação falada**: “vírgula” e “dois pontos” viram pontuação de verdade.
- **Observação** entre parênteses ou depois de “obs”: “(entregar na sexta)”.
- Número no meio da descrição **não** vira quantidade: “2 camisa 10 do Brasil”
  continua sendo um item só.

Se o reconhecimento não colocar nenhuma vírgula, o aplicativo se vira pela lista
de cidades e pelas palavras de quantidade — os dois exemplos acima funcionam com
ou sem pontuação.

### Cidades

O botão **Cidades**, no alto da tela, guarda as cidades para onde vocês mandam
pedidos. É o que permite escrever “pedido de Aline Soares Caruaru” sem vírgula:
o aplicativo reconhece “Caruaru” como local e o resto como nome.

Já vêm cadastrados os municípios de Pernambuco e algumas capitais vizinhas
(`app/src/main/res/raw/cidades.txt`). Se a cidade não estiver na lista e não for
falada com vírgula, ela entra junto com o nome do cliente — dá para corrigir em
**Editar texto** e cadastrar a cidade para as próximas vezes.

## Instalar no celular

1. Na aba **Actions** deste repositório, abra a execução mais recente de
   **Gerar APK**.
2. Baixe o arquivo **etiquetas-por-voz-apk** e descompacte.
3. Passe o `.apk` para o celular e toque nele. O Android vai pedir para permitir
   a instalação de “fontes desconhecidas” — é normal para aplicativo que não vem
   da Play Store.
4. Ao abrir pela primeira vez, autorize o **microfone**.

## Preparar a impressão

O aplicativo usa o sistema de impressão do próprio Android, então a impressora
aparece sozinha se o celular já souber falar com ela:

- **Impressora Wi‑Fi**: celular e impressora na mesma rede, e o
  **Serviço de Impressão Mopria** instalado (vem de fábrica na maioria dos
  Android). Impressoras HP, Epson e Brother também funcionam pelo aplicativo da
  fabricante.
- **Wi‑Fi Direct**: funciona igual, conectando o celular direto na impressora.
- **USB (cabo OTG)**: o Android não imprime por USB sozinho — é preciso um
  aplicativo que ofereça esse serviço de impressão.

O tamanho **A5 já vai escolhido**. Se a sua impressora não tiver A5, é só trocar
para A4 na janela de impressão: o desenho se ajusta ao papel escolhido, ficando
com a letra ainda maior.

> Dica: para o ditado funcionar sem internet, baixe o português no celular em
> **Configurações → Google → Configurações de apps do Google → Pesquisa e
> Assistente → Voz → Reconhecimento de voz off‑line → Português (Brasil)**.

## Mexer no código

Projeto Android padrão, em Kotlin. Abra a pasta no Android Studio, ou pela linha
de comando:

```bash
./gradlew testDebugUnitTest   # testes do interpretador de voz
./gradlew assembleRelease     # gera o APK em app/build/outputs/apk/release/
```

Requer o Android SDK (API 35). O APK de release sai assinado com a chave de
depuração, para poder ser instalado direto no celular sem loja.

### Onde fica cada coisa

| Arquivo | Responsabilidade |
|---|---|
| `OrderParser.kt` | Transforma a frase falada em cliente, cidade e itens |
| `LabelRenderer.kt` | Desenha a folha A5 e calcula o maior tamanho de letra que cabe |
| `LabelPrintAdapter.kt` | Gera o PDF de uma página e entrega ao Android |
| `VoiceInput.kt` | Ditado contínuo em pt‑BR |
| `PlaceCatalog.kt` | Lista de cidades (a que vem pronta + as cadastradas) |
| `MainActivity.kt` | A tela: prévia e os dois botões |

O interpretador de voz tem testes em
`app/src/test/java/br/com/rotinas/etiquetas/OrderParserTest.kt`, incluindo os
dois exemplos citados aqui, com e sem pontuação.
