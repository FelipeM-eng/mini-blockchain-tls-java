# mini-blockchain-tls-java

Projeto da UC Criptografia e Segurança nas Comunicações (ERSC — IPVC).

Implementação de uma blockchain simples em Java com envio de blocos entre nós através de TLS.

Grupo
- Diogo Nunes — 34734
- Luiz Felipe Mesquita — 33158
- Samuel Ferreira — 33846

---

## O que o projeto faz

Cada nó guarda uma cadeia de blocos (Blockchain). Cada bloco tem dados, hash anterior, timestamp, nonce e hash (calculado com SHA-256 em HashUtil).

Para adicionar um bloco novo é preciso minerar (mineBlock na classe Block) até o hash começar com um certo número de zeros (Proof-of-Work).

Os nós comunicam pela classe Node: um lado fica à escuta com SSLServerSocket e o outro envia com SSLSocket. A configuração TLS está em TLSConfig (ficheiros .jks na pasta certs).

Na demonstração com dois PCs, usamos o NodeApp, que permite escrever uma mensagem no terminal, minerar o bloco e enviá-lo ao outro nó.

---

## Estrutura principal

src/main/java/pt/ipvc/ersc/blockchain/
├── core/
│   ├── Block.java
│   └── Blockchain.java
├── network/
│   └── Node.java
├── security/
│   └── TLSConfig.java
├── utils/
│   └── HashUtil.java
├── exception/
│   ├── InvalidBlockException.java
│   ├── InvalidChainException.java
│   └── MalformedBlockException.java
└── app/
    ├── Main.java             # demo local (2 nós na mesma máquina)
    ├── MainTest.java         # testes do núcleo blockchain
    ├── NodeApp.java          # 1 nó por PC (apresentação)
    ├── TLSTest.java          # testa se os .jks carregam
    
src/test/java/pt/ipvc/ersc/blockchain/
├── BlockchainCoreTest.java
└── NetworkTlsIntegrationTest.java

src/main/resources/certs/
├── server-keystore.jks
├── client-truststore.jks
└── server-cert.cer

---

## Requisitos

- Java **17** ou superior
- **Maven** 3.8+

```bash
java -version
mvn -version
```

Os comandos Maven devem ser corridos na **raiz** do projeto (mini-blockchain-tls-java), porque os caminhos dos certificados em TLSConfig são relativos a essa pasta.

---

## Compilar

```bash
mvn clean compile
```

---

## Testar

Testes automáticos (JUnit):

```bash
mvn test
```

Testes manuais no terminal:

```bash
# Núcleo blockchain (génesis, PoW, validação)
mvn exec:java -Dexec.mainClass=pt.ipvc.ersc.blockchain.app.MainTest

# Ver se os certificados e o TLS arrancam
mvn exec:java -Dexec.mainClass=pt.ipvc.ersc.blockchain.app.TLSTest

# Dois nós em localhost com TLS (script)
mvn exec:java -Dexec.mainClass=pt.ipvc.ersc.blockchain.app.IntegrationTest

# Nós A e B no mesmo programa
mvn exec:java -Dexec.mainClass=pt.ipvc.ersc.blockchain.app.Main

# teste do NodeApp em localhost
mvn exec:java "-Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp" "-Dexec.args=--port 5001 --peer-host localhost --peer-port 5000"

# NodeApp entre dois PCs na rede
# No PC A (troca o IP pelo do PC B):
mvn exec:java "-Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp" "-Dexec.args=--port 5000 --peer-host 192.168.1.11 --peer-port 5001"

# No PC B (troca o IP pelo do PC A):
mvn exec:java "-Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp" "-Dexec.args=--port 5001 --peer-host 192.168.1.10 --peer-port 5000"
```



O `pom.xml` tem por defeito o `MainTest` se correres só `mvn exec:java`.

---

## Correr a demonstração (2 PCs na rede)

Cada PC corre um NodeApp. Não uses o Main.java para isso — o Main cria os dois nós no mesmo processo.

### Preparar

1. Os dois PCs na mesma rede Wi‑Fi.
2. Ver o IP de cada um: ipconfig (Windows).
3. Copiar o projeto e a pasta `certs` para os dois PCs (têm de ser iguais).
4. Abrir a porta no firewall:
   - PC A: TCP **5000**
   - PC B: TCP **5001**

```powershell
New-NetFirewallRule -DisplayName "Node5000" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow
New-NetFirewallRule -DisplayName "Node5001" -Direction Inbound -Protocol TCP -LocalPort 5001 -Action Allow
```

5. Testar `ping` entre os dois IPs.

### PC A (porta 5000, envia para o B)

Substituir o IP pelo do PC B:

```powershell
mvn exec:java "-Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp" "-Dexec.args=--port 5000 --peer-host 192.168.1.11 --peer-port 5001"
```

### PC B (porta 5001, envia para o A)

Substituir o IP pelo do PC A:

```powershell
mvn exec:java "-Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp" "-Dexec.args=--port 5001 --peer-host 192.168.1.10 --peer-port 5000"
```

### Ordem na apresentação

1. Arrancar primeiro o `NodeApp` num PC e esperar aparecer `Servidor TLS à escuta...`
2. Arrancar no outro PC.
3. Esperar uns segundos.
4. Menu opção **1** — escrever a mensagem (ex: `Transferencia 10 moedas`).
5. O outro PC deve mostrar `Bloco adicionado!`
6. O segundo PC responde com outra mensagem (opção 1) **depois** de já ter recebido o bloco do primeiro.
7. Opção **3** nos dois — `Cadeia válida? true`

Se aparecer `Bloco rejeitado!`, normalmente é porque os dois mineraram ao mesmo tempo ou a mensagem não encadeia com o último bloco do outro nó.

---

## Certificados (Ficheiro e motivo)

Ficheiros em `src/main/resources/certs/`:



`server-keystore.jks` -> Servidor TLS (`Node` / `TLSConfig.buildServerContext`)
`client-truststore.jks` -> Cliente TLS (`TLSConfig.buildClientContext`)
`server-cert.cer` -> Certificado exportado (documentação) 

Password usada no projeto: `changeit`

Se ao ligar por **IP** (em vez de `localhost`) der erro de certificado, pode ser preciso gerar de novo os `.jks` com o IP incluído. Testar isto antes da apresentação.

---

## Problemas comuns (problema e possivel solução)

Connection refused: Servidor ainda não arrancou ou firewall bloqueia a porta (testar)
Erro SSL / certificado: Certificados não batem com o IP; ver secção certificados
Bloco rejeitado: Enviar só depois do outro nó ter o bloco anterior na cadeia
`mvn test` falha nos .jks: Correr na raiz do projeto

---

## Notas sobre o código

- O servidor e o cliente TLS estão na mesma classe `Node` (`startServer` e `sendBlock`).
- A validação de blocos recebidos está em `Blockchain.addReceivedBlock` e `isValidNewBlock`.
- O génesis usa valores fixos para todos os nós começarem com a mesma cadeia.
- Não há sincronização automática da cadeia completa — cada envio manda um bloco para o IP configurado no `NodeApp`.

---

## Licença

Ver [LICENSE](LICENSE).
