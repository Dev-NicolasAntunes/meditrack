# MediTrack

Repositório para dois projetos separados:

- `android/`: aplicativo Android em Java, para abrir no Android Studio.
- `backend/`: backend Java com Spring Boot, para abrir no IntelliJ IDEA.

Esta configuração prepara os projetos para começar o desenvolvimento. Ainda não há classes de aplicação, endpoints, funcionalidades ou banco de dados.

## Requisitos

- Android Studio com Android SDK e plataforma Android API 36 instalada pelo SDK Manager.
- JDK 25 para o backend. O runtime Java 25 incluído no Android Studio pode ser selecionado como SDK do projeto no IntelliJ.
- Git para sincronizar alterações com o GitHub.

## Abrir os projetos

1. No Android Studio, use **Open** e selecione a pasta `android/`; o Gradle Wrapper 9.6 já está incluído.
2. No IntelliJ IDEA, use **Open** e selecione `backend/pom.xml` ou a pasta `backend/`; importe como projeto Maven.
3. Aguarde os IDEs sincronizarem os plugins e dependências na primeira abertura.

## Comunicação local futura

Quando o backend tiver uma porta configurada, use a URL base apropriada ao ambiente. No emulador Android padrão, `10.0.2.2` aponta para o host da máquina. Em dispositivo físico, use o IP local da máquina e mantenha ambos na mesma rede. A URL ainda não está configurada porque o servidor não foi criado.

Não versione credenciais, chaves ou configurações locais. Consulte `.gitignore`.
