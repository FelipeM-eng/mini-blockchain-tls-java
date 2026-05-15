# mini-blockchain-tls-java
Implementação de Rede Blockchain com Camada de Comunicação Segura TLS em Java

## Como executar o projeto

### Pré-requisitos
Antes de executar o projeto, certifique-se de ter instalado:

- Java JDK 17 ou superior
- Maven 3.8+ (ou superior)

Verifique as instalações com:

```bash
java -version
mvn -version
```

---

### Clonar o projeto

```bash
git clone <URL_DO_REPOSITORIO>
cd mini-blockchain-tls-java
```

---

### Compilar o projeto

Na raiz do projeto, execute:

```bash
mvn clean compile
```

---

### Executar a aplicação

Execute o comando abaixo na raiz do projeto:

```bash
mvn exec:java
```

O Maven irá iniciar automaticamente a classe principal configurada no `pom.xml`.

---

### Estrutura esperada

O projeto utiliza Maven e organização por packages Java.  
A execução deve ser feita pela raiz do projeto (`mini-blockchain-tls-java`) e **não diretamente pelo ficheiro `Main.java`** no VS Code.

❌ Não recomendado:

```bash
javac Main.java
java Main
```

✅ Correto:

```bash
mvn clean compile
mvn exec:java
```