# home-task-service — contexto

Kotlin + Spring Boot 3.5 (Java 17), pacote `com.amaral.hometask`. Render free (dorme) + Postgres (Neon/Supabase), `ddl-auto=update` em prod (sem Flyway). Frontend: `../home-task-app`.

- Não commitar: a usuária faz os commits. Sempre adicionar testes para features novas (`./gradlew test`).
- Rotina diária: `DailyRoutineService.run(today)` = fecha ontem (`ensureAssignments` + penalidade das não feitas) e prepara hoje. `BoardService.ensureAssignments(date)` é idempotente (upsert).
- Lembretes: `ReminderService.send(slot, date)` (NOON/EVENING), uma vez por dia/slot via `reminder_log`; Web Push com VAPID (`WebPushSender`, `nl.martijndwars:web-push` + BouncyCastle); assinaturas em `push_subscriptions` (CHILD1/CHILD2); 404/410 apagam a assinatura.
- Disparo: `@EnableScheduling` (`SchedulingConfig`) só roda com a instância acordada. `WakeUpCatchUp` (ApplicationReadyEvent) roda a rotina do dia e manda o lembrete da janela atual (`ReminderSlot.dueAt`: 12–19h, 19–22h) a cada inicialização. Quem acorda o serviço às 12h e 19h é um agendador externo de URLs (ex.: cron-job.org) chamando `GET /api/health`. `/api/cron/*` (header `X-Cron-Token` = `CRON_TOKEN`) é opcional, para rodar à mão.
- **NUNCA usar GitHub Actions para cron/keep-alive/disparar rotinas**: viola os termos do GitHub (runners só para build/teste/deploy/publicação do projeto) e a conta da usuária já foi suspensa por isso. Antes de recomendar qualquer serviço/ferramenta, ler os termos de uso dele.
- Chaves VAPID: `./gradlew generateVapidKeys` (uma vez só). Detalhes de deploy em DEPLOY.md.
