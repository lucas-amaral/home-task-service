# home-task-service — contexto

Kotlin + Spring Boot 3.5 (compila Java 17, roda em Java 21 no Docker), pacote `com.amaral.hometask`. Render free (dorme) + Postgres (Neon/Supabase). Schema de prod é do Flyway (`db/migration`, `ddl-auto=none`); local e testes usam H2 com `create-drop`. Frontend: `../home-task-app`.

- Não commitar: a usuária faz os commits. Sempre adicionar testes para features novas (`./gradlew test`).
- Rotina diária: `DailyRoutineService.run(today)` = fecha ontem (`ensureAssignments` + penalidade das não feitas) e prepara hoje. `BoardService.ensureAssignments(date)` é idempotente (upsert).
- Lembretes: `ReminderService.send(slot, date)` (NOON/EVENING), uma vez por dia/slot via `reminder_log`; Web Push com VAPID (`WebPushSender`, `nl.martijndwars:web-push` + BouncyCastle); assinaturas em `push_subscriptions` (CHILD1/CHILD2); 404/410 apagam a assinatura.
- Disparo: `@EnableScheduling` (`SchedulingConfig`) só roda com a instância acordada. `WakeUpCatchUp` (ApplicationReadyEvent) roda a rotina do dia e manda o lembrete da janela atual (`ReminderSlot.dueAt`: 12–19h, 19–22h) a cada inicialização. Quem acorda o serviço às 12h e 19h é um agendador externo de URLs (ex.: cron-job.org) chamando `GET /api/health`. `/api/cron/*` (header `X-Cron-Token` = `CRON_TOKEN`) é opcional, para rodar à mão.
- **NUNCA usar GitHub Actions para cron/keep-alive/disparar rotinas**: viola os termos do GitHub (runners só para build/teste/deploy/publicação do projeto) e a conta da usuária já foi suspensa por isso. Antes de recomendar qualquer serviço/ferramenta, ler os termos de uso dele.
- Boot rápido (Render free hiberna): `lazy-initialization`, flags de JVM no `Dockerfile`, runtime Java 21 + virtual threads (ver DEPLOY.md, "Boot settings"). Bean que precisa existir no boot (agendamento, seed, listener de inicialização) leva `@Lazy(false)` e entra no `SchedulingStartupTest`.
- Mudou entidade/tabela: nova migration `V<n>__*.sql` em `src/main/resources/db/migration` (nunca editar a V1, que já rodou em prod). `PostgresMigrationTest` sobe o app num PostgreSQL embutido (zonky, sem Docker) com `ddl-auto=validate`; ele quebra se a migration não bater com as entidades. Atenção ao nome das colunas: `child1Name` vira `child1name` (sem `_` depois de dígito).
- Chaves VAPID: `./gradlew generateVapidKeys` (uma vez só). Detalhes de deploy em DEPLOY.md.
