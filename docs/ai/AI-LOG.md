# Registro de Uso de Ferramentas de IA (AI-LOG)

Este documento registra as interações relevantes com ferramentas de IA Generativa durante o desenvolvimento e testes do projeto, em conformidade com as diretrizes da disciplina.

---

## Registro de Interações

### Log #000 - Modelo

- **Responsável:** [Nome do integrante]
- **Atividade:** [Ex: Estruturação dos casos de teste unitário da classe X]
- **Ferramenta:** [Ex: ChatGPT-4o / Claude 3.5 Sonnet / Gemini]
- **Prompt/Instrução:**
  > "[Cole aqui o prompt utilizado]"
- **Resultado:** [Breve resumo do que a IA respondeu/gerou]
- **Decisão:** [O que o grupo aceitou, alterou ou rejeitou do código/texto]
- **Validação:** [Como o grupo testou e verificou a resposta gerada]

---

### Log #001 - Análise da Classe StorageManager Para Planejamento dos Casos de Testes

- **Responsável:** Danillo
- **Atividade:** Análise da Classe StorageManager Para Planejamento dos Casos de Testes
- **Ferramenta:** Claude/Claude Code
- **Prompt/Instrução:**
    > "Faça uma análise da classe StorageManager e como ela se comporta no código como um todo. Me informe quais são os I/O, como funciona e qual a finalidade de cada método. Me diga também onde ela é importada, suas dependências e sua função na aplicação."
- **Resultado:** O claude gerou uma resposta qualitativa do comportamento da classe, de todas as informações que pedi a respeito de seus métodos e que ela atua como a garantia da persistência do sistema como um todo realizando operações de leitura e escrever em arquivos bin.
- **Decisão:** Aceitei o resultado como um todo, não percebi nenhuma informação errada ou a ser corrigida.
- **Validação:** Comparei o resultado dado pela inteligência artificial com o código e fui tirando dúvidas em commits específicos.
 
---

### Log #001 - Documentação dos Casos de Teste

- **Responsável:** Danillo
- **Atividade:** Documentação dos Casos de Teste
- **Ferramenta:** Claude/Claude Code
- **Prompt/Instrução:**
    > "eu quero que você construa um documento Word para a gente montar o planejamento dos casos de testes. O documento deve conter colunas de ID, método/cenário a ser testado, entrada, saída esperada, status e observações sendo que essas duas eu vou preencher por último. eu quero que você monte casos de teste para situações em que se recebe string vazia, recebe argumentos nulos, valores numéricos indevidos para a lógica da aplicação, testes com caracteres especiais e outros que você ache necessário"
- **Resultado:** Ele gerou o documento como solicitado e parecido com o template que havia colocado de referência porém tive que corrigir as saídas esperadas dos casos de teste com id 6, 7, 9, 10, 11 e 12 porque
- **Decisão:** Aceitei o resultado como um todo, não percebi nenhuma informação errada ou a ser corrigida.
- **Validação:** Comparei o resultado dado pela inteligência artificial com o código e fui tirando dúvidas em commits específicos.

---

### Log #003 - Implementação dos Casos de Teste em JUnit

- **Responsável:** Danillo
- **Atividade:** Automação dos casos de teste CT-U-SM-01 a CT-U-SM-13 da classe StorageManager
- **Ferramenta:** Claude Code (Sonnet 5)
- **Prompt/Instrução:**
    > "implemente todos os casos de teste de docs no projeto"
- **Resultado:** O Claude leu o documento e criou test/me/br/caronapp/storage/StorageManagerTest.java, com um método @Test por caso. Como o projeto não tinha infraestrutura de testes, baixou o JUnit para lib/ e ajustou o .classpath. Para isolar o uso dos arquivos fixos usuarios.bin/caronas.bin, os testes movem os .bin existentes para um backup antes de cada teste e os restauram depois. Os 13 testes passaram.
- **Decisão:** Aceitei a estrutura de testes e o isolamento por backup dos .bin. A classe StorageManager não foi alterada.
- **Validação:** Executei os testes pelo console do JUnit e conferi que os arquivos usuarios.bin e caronas.bin versionados não foram modificados (git status).

---

### Log #004 - Métodos auxiliares dos testes de HostJoin

- **Responsável:** Ricardo
- **Atividade:** Criação de métodos auxiliares em `HostJoinTest` para preparar os dados e executar os cenários de teste.
- **Ferramenta:** ChatGPT
- **Prompt/Instrução:** Ricardo pediu ajuda para criar testes de `HostJoin` com Mockito, tornar legíveis os dados de exemplo e separar os cenários do método `host()`.
- **Resultado:** Foram criados os métodos `consoleComEntrada`, `donoFalso`, `enderecoFalso`, `hospedar` e `caronaComUmaVaga`. Eles preparam o `Console` simulado, os objetos de exemplo e a execução das caronas nos testes.
- **Decisão:** Ricardo utilizou os métodos auxiliares no arquivo de teste para reduzir a repetição dos dados e facilitar a leitura dos casos.
- **Validação:** Executou `mvn -Dtest=HostJoinTest test`: 15 testes encontrados, sem falhas ou erros.

---

### Log #005 - Estruturação e Testes Unitários de CaronaManager

- **Responsável:** Rafael
- **Atividade:** Planejamento e implementação dos testes unitários da classe `CaronaManager` com auxílio de IA.
- **Ferramenta:** Gemini
- **Prompt/Instrução:**
  > "Preciso criar casos de testes unitários para a classe CaronaManager cobrindo os fluxos de dono e passageiro, transições de estado e entradas de console. Como simular o Console/Scanner com Mockito e me ajude a estruturar os casos de teste em JUnit 5?"
- **Resultado:** A IA explicou como mockar o `Console`, simular entradas com `Scanner` e capturar a saída via `ByteArrayOutputStream`, além de auxiliar na geração do código inicial dos 12 casos de teste cobrindo fluxos de host (partida/finalização), passageiro (saída de corrida) e validações de ID inexistente.
- **Decisão:** Aceitei a estrutura de testes e a estratégia de mocks gerada pela IA, revisei os cenários para garantir o uso correto dos objetos reais do domínio (`Usuario`, `Carona`, `Rota`) e conferi a aderência às regras de negócio e transições de estado.
- **Validação:** Executou `mvn test -Dtest=CaronaManagerTest`: 12 testes executados com sucesso (0 falhas e 0 erros).
