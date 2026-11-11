# MediTrack

Aplicativo Android em Java com backend separado em Java/Spring Boot. O módulo do integrante 1 implementa o cadastro e o controle de medicamentos e estoque.

## Abrir e executar

### Backend no IntelliJ IDEA

1. Abra backend/pom.xml e selecione o JDK 26.
2. Aguarde o Maven importar as dependências.
3. Execute com.meditrack.backend.MediTrackBackendApplication.
4. A API ficará disponível em http://localhost:8080; o SQLite será criado em backend/data/meditrack.db se o diretório de trabalho for backend/.

### Android no Android Studio

1. Abra a pasta android/ e sincronize o Gradle.
2. O daemon Gradle do projeto usa JDK 26, definido em android/gradle/gradle-daemon-jvm.properties; use um emulador ou dispositivo conectado para iniciar o app.
3. O endereço padrão da API para o emulador é http://10.0.2.2:8080/api/.
4. Para um aparelho físico, defina MEDTRACK_API_BASE_URL no gradle.properties do usuário (fora do repositório), apontando para o IP local do computador, por exemplo http://192.168.0.10:8080/api/.

O tráfego HTTP sem TLS fica habilitado somente na variante de desenvolvimento. Não use essa configuração para publicar o app.

## Funcionalidades do integrante 1

- Cadastro, edição, exclusão, lista e detalhes de medicamentos.
- Nome, dosagem, quantidade disponível, frequência, horários e período do tratamento.
- Ajuste de estoque com validação para impedir quantidade negativa.
- Aviso visual de estoque baixo, com limite configurável por medicamento (padrão: 5 unidades).
- Estimativa de dias restantes com base na quantidade e nos horários diários cadastrados.
- Persistência dos dados no SQLite do backend.
- Operação de confirmação de dose que desconta atomicamente uma unidade, disponível para integração com o módulo do integrante 2.

## API do módulo de medicamentos

- GET /api/medications e GET /api/medications/{id}
- POST /api/medications e PUT /api/medications/{id}
- DELETE /api/medications/{id}
- POST /api/medications/{id}/stock-adjustments com {"quantityChange": 5} (negativo para retirar)
- POST /api/medications/{id}/doses/confirm para descontar uma unidade após confirmação

Datas usam AAAA-MM-DD; horários usam HH:mm. O alerta de estoque baixo aparece no app e na resposta da API quando a quantidade for menor ou igual ao limite definido.

## Ambiente e Git

- JDK 26 para o backend e para o daemon Gradle do Android; o app mantém compatibilidade de código-fonte Java 17.
- Android Studio e Android SDK API 36.
- O Gradle Wrapper versionado fixa Gradle 9.6.1.
- backend/data/ e artefatos locais são ignorados pelo Git; não versione dados pessoais ou segredos.
