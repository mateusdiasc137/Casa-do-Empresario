# Implementação do Pacote 3 — Funcionalidades complementares

Esta versão mantém apenas o Pacote 3, sem onboarding e sem as alterações exclusivas do Pacote 1.

## Funcionalidades adicionadas

- Dashboard do organizador com métricas de eventos, interessados e próximos eventos.
- Central de notificações interna com lista visual, estados vazios e categorização.
- Perfil corporativo expandido com percentual de preenchimento.
- Tela de edição de perfil profissional com empresa, cargo, cidade, telefone, LinkedIn e bio.
- Integração das novas telas na aba Perfil da MainActivity.

## Principais arquivos novos

- `OrganizerDashboardActivity.java`
- `NotificationCenterActivity.java`
- `ProfileDetailsActivity.java`
- `EditProfileActivity.java`
- `NotificationCenterAdapter.java`
- `OrganizerEventSummaryAdapter.java`
- `AppNotification.java`
- Layouts correspondentes em `app/src/main/res/layout/`
- Drawables de apoio em `app/src/main/res/drawable/`

## Arquivos existentes alterados

- `AndroidManifest.xml`
- `MainActivity.java`
- `LoginActivity.java`
- `Usuario.java`
- `UsuarioDao.java`
- `AppDatabase.java`
- `UserRepository.java`
- `FirebaseUserRepository.java`
- `RoomUserRepository.java`
- `SessionManager.java`
- `activity_main.xml`

## Observação

O projeto original não possui `gradlew`; portanto, recomenda-se abrir no Android Studio, executar o sync do Gradle e validar o build antes do commit final.
