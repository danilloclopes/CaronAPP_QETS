# CaronAPP

Protótipo de aplicativo de carona sendo desenvolvido por Eperson Filho e Sérgio Ricardo para a disciplina de Programação Orientada a Objetos(POO).

CaronAPP tem o objetivo de criar um ambiente interativo para que alunos possam pedir carona para outros alunos que se encontram nas proximidades.

O diagrama de classes do aplicativo se encontra no repositório, e pode ser lido pela ferramenta draw.io;
Além disso, dentro de cada classe há um comentário informando quem foi o autor do código presente nela.

## Interface web para testes de sistema

A interface usa HTML com formulários e o servidor HTTP do JDK, sem frameworks ou dependências adicionais. O console original continua disponível em `me.br.caronapp.Main`.

Com Java 11 ou superior e Maven, execute na pasta `CaronAPP_QETS`:

```bash
mvn compile
java -cp target/classes me.br.caronapp.web.WebMain
```

Abra **http://localhost:8080**. Cadastre uma conta para começar; não há usuários de demonstração. Para trocar a porta e o arquivo de dados:

```bash
java -cp target/classes me.br.caronapp.web.WebMain 8081 /tmp/caronapp-teste.bin
```

Os dados web ficam em `web-data.bin`, separado de `usuarios.bin` e `caronas.bin` do console. O arquivo web mantém usuários e caronas no mesmo fluxo de serialização para preservar as referências entre os objetos. Reiniciar o servidor mantém os cadastros e corridas, mas exige novo login. Para começar com dados vazios, use outro caminho de arquivo. Execute uma instância por arquivo de dados.

Fluxos disponíveis: cadastro, login, logout, oferta de carona (ida ou volta), consulta das caronas abertas, participação, saída, confirmação da partida, finalização e histórico. A partida é liberada ao motorista quando faltam até 30 minutos para o horário informado. A página atualiza o estado ao receber uma requisição; atualize o navegador para consultar a passagem do tempo.

Evidências, limitações e revisão da geração com IA estão em [`docs/ai/interface-web/REVISAO.md`](docs/ai/interface-web/REVISAO.md).

## Artefatos da entrega

| Artefato | Localização |
| --- | --- |
| Documento compartilhado do grupo (Google Docs) | [Abrir documento](https://docs.google.com/document/d/1ekOuJkdEYtPGt5Mn_Prw_icX7OnHMUNa8h7mTlUdT9g/edit?usp=sharing) |
| Local de Testes | [Abrir documento](https://docs.google.com/document/d/1ekOuJkdEYtPGt5Mn_Prw_icX7OnHMUNa8h7mTlUdT9g/edit?usp=sharing) |
| Classe testada por Ricardo | [`HostJoin.java`](src/me/br/caronapp/console/util/HostJoin.java) |
| Testes unitários de Ricardo | [`HostJoinTest.java`](test/me/br/caronapp/console/util/HostJoinTest.java) |
| Classe estudada e alterada por Danillo | [`StorageManager.java`](src/me/br/caronapp/storage/StorageManager.java) |
| Testes unitários de Danillo | [`StorageManagerTest.java`](test/me/br/caronapp/storage/StorageManagerTest.java) |
| Classe testada por Rafael | [`CaronaManager.java`](src/me/br/caronapp/console/util/CaronaManager.java) |
| Testes unitários de Rafael | [`CaronaManagerTest.java`](test/me/br/caronapp/console/util/CaronaManagerTest.java) |
| Classe testada por Nicole | [`Console.java`](src/me/br/caronapp/console/Console.java) |
| Testes unitários de Nicole | [`ConsoleTest.java`](test/me/br/caronapp/console/ConsoleTest.java) |
| Registro de uso de IA | [`AI-LOG.md`](docs/ai/AI-LOG.md) |
