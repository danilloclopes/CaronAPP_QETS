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

---

### Log #006 - Testes Unitários de Console

- **Responsável:** Nicole
- **Atividade:** Estruturação dos casos de teste unitário e plano de teste da classe Console
- **Ferramenta:** Github Copilot
- **Prompt/Instrução:**
  > "Quero criar testes unitários e plano de teste para a classe Console do projeto CaronAPP. Analise a classe Console, o padrão dos testes existentes e me diga como montar os casos de teste, incluindo estrutura, escopo, manual e issue de defeito."
- **Resultado:** Foi gerado um conjunto de casos de teste para a classe Console, com escopo, exemplos de IDs, foco em logout, estágio, usuário autenticado, draw(), isAdm() e defeito provável com NullPointerException quando usuário é nulo.
- **Decisão:** O grupo aceitou a estrutura proposta, ajustando o foco para a classe Console e mantendo o padrão de documentação já usado no projeto. Foi validado que o principal defeito detectado é o uso de usuario.isAdm() sem verificar usuario != null.
- **Validação:** A resposta foi conferida contra a implementação real da classe Console.java e comparada ao padrão observado nos outros testes de classe. A análise confirmou que os casos de teste são consistentes com a arquitetura do projeto e com o fluxo de console.

---

### Log #007 - Interface web para testes de sistema

- **Responsável:** Ricardo Tanaka
- **Atividade:** Implementação de uma interface web simples para acessar os fluxos do CaronAPP_QETS e permitir que o grupo escreva testes de sistema posteriormente.
- **Ferramenta:** OpenAI Codex (agente baseado em GPT-6), via ambiente local de desenvolvimento.
- **Prompt/Instrução:**
  > "Vamos criar uma interface web para um repositorio que usa o console como meio de interação, isso é para uma materia de Qualidade e Teste de software, ou seja a interface não é o importante, apenas como vamos interagir para fazer testes de sistema de seus requisitos usando Selenium. Esse projeto que vamos fazer a interface web é CaronAPP_QETS."
  > "nao cria os testes do selenium agora eu vou fazer isso depois"
  O usuário também forneceu as exigências de registro de uso de IA, incluindo responsável, atividade, ferramenta, prompt, resultado, decisão, validação e preservação das versões dos testes. Registro ampliado em [`interface-web/PROMPTS.md`](interface-web/PROMPTS.md).
- **Resultado:** Adicionado `WebMain`, com servidor HTTP nativo do JDK, formulários HTML, sessões por cookie, persistência web separada e IDs estáveis. Reutilizadas as classes `Auxiliar`, `Usuario`, `Carona`, `Rota`, `Campus` e `Posicao`. Adicionados dois testes HTTP de integração e instruções no README. Nenhum teste ou dependência Selenium foi mantido.
- **Decisão:** O usuário definiu que os testes Selenium serão feitos depois pelo grupo. O agente removeu o rascunho e a dependência que havia acrescentado. A implementação restante e os testes HTTP são uma proposta gerada e revisada pelo agente; o aceite e a revisão humana do grupo permanecem pendentes. Não se atribui aprovação ao grupo.
- **Validação:** A suíte original apresentou 58 testes, cinco falhas e um erro antes das mudanças no domínio. Os dois testes HTTP novos passaram, incluindo cadastro, duplicidade, login, autorização, vagas, participação, saída, reinício, identidade dos objetos, logout, partida e finalização. Relatórios e diferenças entre a versão inicial e final dos testes estão em [`interface-web/REVISAO.md`](interface-web/REVISAO.md). A execução de um rascunho Selenium antes da mudança de escopo não produziu uma validação completa aprovada e não é apresentada como evidência de sucesso.
- **Instruções, ferramentas e configurações disponíveis:** Priorizar interface simples e interação para testes; registrar contribuições substanciais de IA; não produzir testes Selenium nesta etapa. O agente aplicou a skill local `ponytail` para reduzir dependências e escopo. Usou leitura de arquivos, shell, `apply_patch`, Maven/JUnit e inspeção de relatórios. Java 19.0.2 local; compatibilidade de compilação Java 11; Maven 3.8.7; JUnit 5.7.0 já existente. Não foram fornecidas configurações adicionais de agentes pelo grupo. Detalhes em `interface-web/PROMPTS.md`.

---

### Log #008 - Inspeção e correções de HostJoin com SonarQube

- **Responsável:** Ricardo Tanaka
- **Atividade:** Apoio no uso do SonarQube e na aplicação das correções apontadas na classe `HostJoin`.
- **Ferramenta:** OpenAI Codex, via ambiente local de desenvolvimento.
- **Prompt/Instrução:**
  > "Auxilie na configuração e utilização do SonarQube para inspecionar a classe HostJoin"
- **Resultado:** A IA auxiliou na configuração e execução local do SonarQube, na interpretação dos apontamentos e na captura dos prints antes e depois. Aplicou a criação de um logger com `java.util.logging`, a substituição de `Calendar` por `LocalDateTime` em `HostJoin`, a inclusão de um construtor compatível em `Carona`, a restrição do construtor de `HostJoin` e a remoção de uma atribuição desnecessária. Adaptou os testes para capturar e restaurar os logs e repetiu as verificações de cobertura e mutação.
- **Decisão:** O usuário solicitou a aplicação das correções do SonarQube, incluindo o logger e a migração para `java.time`, preservando a compatibilidade com o restante do projeto.
- **Validação:** O SonarQube, com o mesmo perfil `Sonar way comprehensive` nas análises antes e depois, passou de 30 para zero apontamentos em `HostJoin`. Os 89 casos de `HostJoinTest` passaram; o JaCoCo registrou 100% de instruções e ramos. O PIT gerou 65 mutantes: 64 mortos e um detectado por timeout, correspondendo a 98,46% de mortos e 100% de detecção na ferramenta. Prints em `relatorios/hostjoin/inspecao/sonar-antes.png` e `relatorios/hostjoin/inspecao/sonar-depois.png`.

---

### Log #009 - Inspeção da StorageManager com SonarQube

- **Responsável:** Danillo
- **Atividade:** Configuração do SonarQube local, análise da classe `StorageManager`, correção dos apontamentos e registro antes/depois (Issue #11).
- **Ferramenta:** Claude Code (Claude Opus 5.5)
- **Prompt/Instrução:**
  > "Agora quero fazer a parte do SonarQube na StorageManager. Configure a análise só para essa classe e tire os prints do antes. Abra a Issue no padrão das outras e depois corrija o que o Sonar apontar, principalmente o fechamento manual dos arquivos e os catch vazios. No final, rode de novo, tire o print do depois e feche a Issue explicando o que foi corrigido."
- **Resultado:** A IA criou o perfil Maven `inspecao-storagemanager`, que analisa somente a `StorageManager`. Explicou que o perfil padrão "Sonar way core" não mostrava apontamentos e que era preciso usar o "Sonar way comprehensive", o mesmo que o Ricardo usou. Na correção, trocou o fechamento manual dos streams por *try-with-resources*, tratou os `catch (EOFException)` vazios com log e separou a leitura em métodos auxiliares. Na primeira análise de "depois", o Quality Gate falhou por cobertura 0%. A IA identificou que faltava enviar o relatório do JaCoCo e rodou a análise de novo com ele.
- **Decisão:** Aceitei as correções no código. Só troquei o perfil de qualidade depois de perceber que a análise não mostrava nenhuma issue e perguntar se isso era o esperado. Passei o token do Sonar apenas pela linha de comando, sem salvá-lo no `pom.xml`.
- **Validação:** No SonarQube, a classe passou de 12 apontamentos para 0, com Quality Gate aprovado e 100% de cobertura. Os testes continuaram passando depois da refatoração. Prints em `relatorios/storagemanager/inspecao/`. A Issue #11 foi fechada com um comentário explicando a correção.

---

### Log #010 - Isolamento dos testes unitários, JaCoCo e PITest da StorageManager

- **Responsável:** Danillo
- **Atividade:** Ajustes nos testes de `StorageManager` para que a suíte ficasse verde e atingisse as metas de cobertura de arestas (JaCoCo) e de mutação (PIT).
- **Ferramenta:** Claude Code (Claude Opus 5.5)
- **Prompt/Instrução:**
  > "Faça a etapa de testes unitários da StorageManager. Os testes precisam ficar isolados, sem mexer nos arquivos .bin reais, e a suíte tem que passar inteira para o PITest conseguir rodar. Os testes que falham de propósito da Entrega 1 não podem se perder. Quero pelo menos 80% de cobertura de arestas no JaCoCo e 80% de mutantes mortos no PIT, e a classe precisa ter complexidade de pelo menos 10."
- **Resultado:** A IA adicionou à `StorageManager` um construtor que recebe os caminhos dos arquivos, com validação de caminho nulo, vazio ou repetido. Reescreveu `StorageManagerTest` usando pasta temporária (`@TempDir`) em vez do backup dos `.bin` reais e acrescentou casos novos (CT-U-SM-14 a 24): arquivo corrompido, classe inexistente, listas nulas, sobrescrita e ordem dos registros. Os 5 casos da Entrega 1 que falhavam de propósito foram movidos sem alteração para `StorageManagerDefeitosTest`. Também criou os perfis Maven `estrutural-storagemanager` e `mutacao-storagemanager`.
- **Decisão:** Aceitei a separação dos testes de defeito, pois isso mantém as falhas documentadas (Issues #1, #2, #3, #5 e #8) sem impedir o PIT de rodar. Mantive o código original dos testes da Entrega 1 para preservar a versão anterior.
- **Validação:** Os 19 testes de `StorageManagerTest` passaram. JaCoCo: 100% de arestas (22/22) e de linhas (58/58). PIT: 18 de 18 mutantes mortos (100%). Prints em `relatorios/storagemanager/estrutural/` e `relatorios/storagemanager/mutacao/`.

---

### Log #011 - Teste de integração LoginRegistro + Auxiliar + StorageManager

- **Responsável:** Danillo
- **Atividade:** Criação do `AuthStorageIntegrationTest` (CT-I-SM-01 a 07) e registro do defeito encontrado (Issue #12).
- **Ferramenta:** Claude Code (Claude Opus 5.5)
- **Prompt/Instrução:**
  > "Crie o teste de integração entre LoginRegistro, Auxiliar e StorageManager, sem mocks. O fluxo é: cadastrar um usuário pela lógica de registro, salvar com a StorageManager, limpar a memória do Auxiliar, carregar de novo e fazer o login com o usuário recuperado. Se algum dado não voltar certo ou houver conflito de IDs, abra uma Issue descrevendo o problema."
- **Resultado:** A IA criou 7 cenários que usam o `Console` real com entrada de teclado simulada: cadastro e login após reinício, senha errada, maiúsculas no usuário, usuário duplicado, vários usuários e caronas persistidas. O CT-I-SM-07 revelou um defeito real. Como usuários e caronas são gravados em arquivos separados, depois de recarregar a carona ativa e a carona na lista do motorista viram objetos diferentes, e o motorista não vê o passageiro que entrou. A IA abriu a Issue #12 com os passos para reproduzir.
- **Decisão:** Aceitei os testes e a Issue. Cheguei a pedir a correção do defeito, mas ela foi interrompida e ainda não foi aplicada. O CT-I-SM-07 continua falhando de propósito, para documentar o problema.
- **Validação:** Executei a classe de integração: 6 testes passaram e o CT-I-SM-07 falhou da forma descrita na Issue #12.

---

### Log #012 - Testes Selenium das telas de cadastro e login

- **Responsável:** Danillo
- **Atividade:** Automação dos testes de sistema da interface web (cadastro e login) com Selenium.
- **Ferramenta:** Claude Code (Claude Opus 5.5)
- **Prompt/Instrução:**
  > "Agora faça os testes Selenium das telas de cadastro e login da interface web. Quero que o teste preencha nome, CPF, usuário e senha, cadastre e verifique se entrou no sistema. No login, teste com a senha errada para ver se aparece a mensagem de erro e depois com a senha certa para ver se entra no lobby. Se a tela aceitar usuário duplicado ou não mostrar o erro, abra uma Issue."
- **Resultado:** A IA adicionou a dependência do Selenium ao `pom.xml` e criou `CadastroLoginSeleniumTest`, com 7 casos (CT-S-SM-01 a 07): cadastro com sucesso, usuário duplicado, senha errada, usuário inexistente, login correto, campos obrigatórios e cadastro mantido após reiniciar o servidor. Cada teste sobe o `WebMain` numa porta livre, com dados em pasta temporária, e usa o Chrome sem janela (headless).
- **Decisão:** Aceitei os testes. Não encontrei defeito nessas telas, então não abri Issue. Depois de enviar o commit com a mensagem errada, corrigi o nome dele.
- **Validação:** Os 7 testes passaram (cerca de 21 s).

---

### Log #013 - Separação dos testes unitários e de integração de HostJoin

- **Responsável:** Ricardo Tanaka
- **Atividade:** Classificação e separação dos 89 casos existentes por nível de teste.
- **Ferramenta:** OpenAI Codex, via ambiente local de desenvolvimento.
- **Prompt/Instrução:** "vamos separar primeiro. separamos por arquivo?"
- **Resultado:** Mantidos 12 casos de validação local em `HostJoinTest`, com Console simulado e Scanner em memória. Movidos 77 casos que exercitam domínio e registro reais para `HostJoinIntegrationTest`. Ajustados os perfis JaCoCo e PIT para executar ambas as suítes. Nenhum caso ou regra de produção foi alterado; não foi introduzida uma classe-base de testes nem nova dependência.
- **Decisão:** O usuário autorizou a separação; a classificação e implementação foram realizadas pelo agente e estão disponíveis para revisão humana. Os fluxos host/join permanecem como integração; esta mudança não adiciona versões isoladas desses fluxos.
- **Validação:** `mvn -o -Pestrutural-hostjoin verify`: 12 unitários e 77 de integração passaram. JaCoCo: 42/42 ramos e 384/384 instruções cobertos, considerando ambas as suítes. PIT: 65 mutantes, 64 mortos e um timeout (98,46% mortos; 100% detectados). O PIT exigiu execução fora do sandbox após bloqueio por permissão. `git diff --check` executado.
- **Preservação:** Estado recebido antes desta alteração em `docs/ai/hostjoin-separacao/inicial/HostJoinTest.java`; resultado em `docs/ai/hostjoin-separacao/final/`. Esse estado inicial é o anterior à separação, não a primeira geração histórica com IA. A mudança consistiu na movimentação dos cenários e remoção do preparo de estado global dos unitários, preservando as entradas e asserções.
- **Instruções e ferramentas:** Skill local ponytail; instruções do usuário para legibilidade e indentação; shell, Python para movimentação dos blocos, Maven, JUnit, Mockito, JaCoCo e PIT. Sem delegação a outros agentes.

---

### Log #014 - Integração incremental top-down no fluxo join

- **Responsável:** Ricardo Tanaka
- **Atividade:** Aplicação top-down à participação em caronas, a partir de HostJoin.
- **Ferramenta:** OpenAI Codex, ambiente local.
- **Prompt/Instrução :** temos que aplicar o top down no join. vamos começar com a carona simulada e depois testar com a real
- **Resultado:** Criado `HostJoinTopDownTest` com quatro casos: sucesso nos dois sentidos, recusa e lotação. HostJoin e ListarCaronas reais chamam Carona, Usuario e Rota simulados. A etapa seguinte substitui os stubs pelo domínio real em HostJoinIntegrationTest. A listagem e o registro já são reais na etapa inicial; o escopo incremental é join, não host. README descreve componentes e comandos por etapa; perfis de métricas incluem as três suítes.
- **Decisão:** Implementação do agente conforme solicitação do usuário, disponível para revisão humana. Reutilizados Mockito e os testes integrados existentes, sem alterar produção ou adicionar dependências.
- **Validação:** A primeira execução com captura de logs falhou em uma asserção. Foi mantida uma referência forte ao Logger durante o teste para estabilizar a captura. Após a correção, os quatro casos passaram; o perfil estrutural também passou com os 93 casos. A integração real verifica efeitos no domínio; a etapa com stubs verifica delegação, mensagem e retorno ao lobby.
- **Preservação:** Versão anterior à correção da captura em `docs/ai/hostjoin-topdown/inicial/HostJoinTopDownTest.java`; a versão final é mantida no teste ativo `test/me/br/caronapp/console/util/HostJoinTopDownTest.java`, que posteriormente incorporou a etapa com domínio real. A cópia intermediária foi removida a pedido do usuário. Não foram alteradas as regras da aplicação.
- **Instruções e ferramentas:** Skill ponytail e regras de formatação do usuário; shell, Python, Maven, JUnit, Mockito, JaCoCo e PIT. Sem subagentes.

---

### Log #015 - Simplificação em dois arquivos de teste

- **Responsável:** Ricardo Tanaka
- **Atividade:** Consolidação das etapas de integração em HostJoinTopDownTest.
- **Ferramenta:** OpenAI Codex, ambiente local.
- **Prompt/Instrução:** simplifica isso aí. deixa os unitários num arquivo e juntar as duas etapas de integração no outro
- **Resultado:** Mantido HostJoinTest com 12 casos. Reunidos os quatro casos com stubs e os 77 casos com domínio real em HostJoinTopDownTest, usando duas classes JUnit @Nested. Removido o arquivo ativo HostJoinIntegrationTest. Atualizados README e perfis Maven; PIT inclui as classes internas com HostJoinTopDownTest*. Produção e cenários preservados.
- **Decisão:** Organização solicitada pelo usuário; implementação disponível para revisão humana. Mantidas as versões históricas de IA, sem sobrescrevê-las.
- **Validação:** `mvn -o clean -Pestrutural-hostjoin verify` passou com 93 casos e 42/42 ramos cobertos. PIT passou com 64 mutantes mortos e um timeout entre 65 gerados (98,46% mortos; 100% detectados). `git diff --check` sem erros.
- **Preservação:** Versão inicial top-down em `docs/ai/hostjoin-topdown/inicial/HostJoinTopDownTest.java`; resultado final no teste ativo `test/me/br/caronapp/console/util/HostJoinTopDownTest.java`. As cópias redundantes nas pastas `final` e `consolidado` de top-down foram removidas a pedido do usuário. Alteração organizacional: agrupamento dos corpos das classes existentes em duas etapas, sem remover asserções.
- **Instruções e ferramentas:** Regras de formatação do usuário e skill ponytail; shell, Python, Maven, JUnit, JaCoCo e PIT; sem novas dependências ou subagentes.

Nota de organização: a pedido de Ricardo, a cópia intermediária `docs/ai/hostjoin-separacao/final/HostJoinIntegrationTest.java` foi removida. Ela havia sido criada nesta sessão e ainda não estava versionada no Git. Os cenários permanecem no arquivo ativo `HostJoinTopDownTest.java`.

---

### Log #016 - Selenium para oferta de caronas

- **Responsável:** Ricardo Tanaka
- **Atividade:** Testes de sistema de ida/volta, campos e limites de passageiros.
- **Ferramenta:** OpenAI Codex, ambiente local.
- **Prompt/Instrução:** "cria ai, no folder de cima eu acho que ja criei antes, tenta achar e seguir meu padrao de codigo"
- **Resultado:** Encontrado `../aulaselenium/src/test/java/exselenium/TestaCadastroAutomationExercise.java`, também presente em `../qualidade_teste2026.2-aulaselenium`. Criado HostJoinSeleniumTest com o mesmo estilo: JUnit 5, ChromeDriver, WebDriverWait, Select, By.id, nomes em português e helpers simples. Adicionado Selenium 4.20.0, mesma versão do exemplo, com escopo test. Cada caso inicia servidor em porta livre e armazenamento temporário, cadastra pela interface e restaura estado global ao terminar. Os 20 casos cobrem sentidos, passageiros 0/1/4/5, sete campos obrigatórios, valores numéricos inválidos e rua com espaços. Os testes só executam com `-Dselenium=true`.
- **Decisão:** Implementação gerada conforme autorização, disponível para revisão humana. A data usa JavaScript apenas para preencher datetime-local sem depender do idioma; os formulários são enviados por clique normal, com validação HTML ativa. Não foram alteradas regras de produção. WebMain não chama HostJoin diretamente: são testes de sistema dos requisitos da oferta de caronas.
- **Validação:** Primeira execução: 20 casos, uma falha causada pela leitura da mensagem antiga de cadastro antes do redirecionamento. Correção: esperar o texto esperado com ExpectedConditions.textToBe. Segunda execução: 20 casos, zero falhas, erros ou skips. Chrome 151 em headless, Selenium 4.20.0, driver local 151.0.7922.138; avisos de CDP não impediram operações WebDriver. Evidência em `relatorios/hostjoin/selenium/resultado.txt`. Regressão: `mvn -o '-Dtest=HostJoinTest,HostJoinTopDownTest*,WebMainTest' test`, 95 casos passaram. O seletor sem asterisco não incluiu as classes @Nested na execução combinada; corrigido o comando do README. `git diff --check` sem erros.
- **Preservação:** Primeira versão em `docs/ai/hostjoin-selenium/inicial/HostJoinSeleniumTest.java`; final em `test/me/br/caronapp/web/HostJoinSeleniumTest.java`. Diferença: importação de ExpectedConditions e espera explícita do texto nas mensagens de criação e rejeição do servidor. Sem cópia redundante da versão final.
- **Instruções e ferramentas:** Skill ponytail e regras de formatação do usuário; leitura dos exemplos locais, shell, Python, Maven, JUnit, Selenium e Chrome; sem subagentes. README contém comandos e resumo dos cenários para apoiar o relatório no Google Docs.

---

### Log #017 - Um arquivo por etapa de integração

- **Responsável:** Ricardo Tanaka
- **Atividade:** Separar as duas etapas da integração top-down de HostJoin em arquivos próprios.
- **Ferramenta:** OpenAI Codex, ambiente local.
- **Prompt/Instrução:** "separa em 2 arquivos entao"
- **Resultado:** Os quatro casos com stubs passaram para `HostJoinDominioSimuladoTest` e os 35 com domínio real para `HostJoinDominioRealTest`. Os perfis de cobertura e mutação passaram a incluir as duas classes.
- **Decisão:** Ricardo escolheu manter um arquivo por etapa para que cada uma pudesse ser identificada e executada separadamente.
- **Validação:** Após compilação limpa, passaram os 39 casos de integração e os 14 unitários. O JaCoCo registrou 48 de 48 ramos cobertos. Dos 73 mutantes gerados pelo PIT, 72 foram mortos e um foi detectado por timeout.
- **Preservação:** A [versão inicial da etapa com stubs](hostjoin-topdown/inicial/HostJoinTopDownTest.java) está arquivada. Os testes desta revisão estão nos arquivos de [domínio simulado](../../test/me/br/caronapp/console/util/HostJoinDominioSimuladoTest.java) e [domínio real](../../test/me/br/caronapp/console/util/HostJoinDominioRealTest.java).
- **Instruções e ferramentas:** Regras de formatação do projeto, shell, Python, Maven, JUnit, JaCoCo e PIT.
