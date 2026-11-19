# Mesa Redonda — Trabalho Backend PUCPR

Backend da liga de board games (noite de jogo + fila de música). Spring Boot 4 / Kotlin, JPA + H2, JWT.

**Equipe:** Lucas Brascher.

## Vídeo

Link do YouTube entra aqui antes da entrega.

## Base

Projeto baseado em [ViniGodoy/backend-dev](https://github.com/ViniGodoy/backend-dev), branch 06-testes-final (commit f4217af).
Usuários, roles, login e JWT vieram da aula; o domínio abaixo é meu.

## Como rodar

Java 21.

    ./gradlew test
    ./gradlew bootRun

Contexto da aula: `/api`. Swagger em <http://localhost:8080/api>. Login: `POST /api/users/login`
(admin de desenvolvimento criado no start: `admin@authserver.com` / `admin`).

## O que foi feito (Opção 1 — estendendo os serviços)

11 entidades novas e 47 endpoints, todos no Swagger.

| Módulo | Entidades | Relações |
|---|---|---|
| `boardgames/` | BoardGame, Tag | `@ManyToMany` com `@JoinTable` |
| `nights/` | GameNight, NightGame, NightRegistration, Member, ScoreEntry | N:N **com tabela no meio** (`NightGame` guarda a ordem do draft, `NightRegistration` guarda o papel) e N:1 |
| `music/` | MusicRoom, Track, QueueEntry, Vote | `@OneToOne` (sala ↔ noite) e N:1 |

**CRUD completo:** jogos, tags, membros, noites de jogo, faixas e placares.
**Associações:** tag em jogo, jogo em noite (draft), membro em noite, faixa na fila, voto na faixa.
**Filtro dinâmico e ordenação por query param.** A consulta é montada em tempo de execução (`Specification` + `DynamicFilter`):
o cliente escolhe os critérios na própria URL, como `campo=valor` ou `campo.operador=valor` (operadores `eq`, `ne`, `gt`, `gte`, `lt`, `lte`
e `like`), todos combinados com AND. Só campos permitidos por entidade entram, e campo, operador ou valor inválido devolve 400.
Vale para jogos, noites, faixas e membros; os parâmetros nomeados antigos (`tag`, `minPlayers`, `status`, `from`...) continuam funcionando.

    GET /api/board-games?durationMinutes.lte=60&weight.gte=2&name.like=cat&tag=coop&sort=durationMinutes,desc
    GET /api/game-nights?status.ne=CANCELLED&scheduledAt.gte=2026-11-01T00:00:00Z&sort=scheduledAt,desc
    GET /api/tracks?durationSeconds.lte=240&artist.like=ana&sort=title,asc
    GET /api/members?seasonPoints.gte=10&sort=seasonPoints,desc

**Rotas com substantivos** (REST, Aula 02): `PUT /game-nights/{id}/draft` confirma o draft; `PUT /game-nights/{id}/queue/{id}/playing`
toca a faixa e `PUT .../skipped` pula. `GET /health` devolve `{"status":"OK"}`.
**Segurança:** todo `DELETE` (e confirmar draft, tocar e pular faixa) exige JWT (`Authorization: Bearer <token>`).
Sem token ou com token inválido a resposta é **401**; 403 fica para quem está logado mas sem permissão (ex.: não-ADMIN em `/roles`).
O segredo do JWT vem da variável de ambiente `JWT_SECRET` (com valor de desenvolvimento como padrão em `security.yaml`),
lido por `SecurityProperties` (`@ConfigurationProperties`).
**Logs:** `log.info` ao criar/atualizar e `log.warn` ao apagar, gravados em `./logs`.
**Erros:** `@RestControllerAdvice` devolvendo `ProblemDetail` (RFC 9457) com o campo `errors`: `@Valid` inválido → **422**; JSON malformado,
valor de enum inválido ou tipo errado na URL → 400 apontando o campo; exceções de domínio mantêm o status do `@ResponseStatus`
(400/401/403/404); qualquer outra vira 500 sem detalhes. Segue o `GlobalExceptionHandler` de referência da aula 07.
**Testes:** 168 (service com MockK, controller, repositório com H2 e web com MockMvc) e CI no GitHub Actions.
Cobertura de linhas de 91% (`./gradlew test` gera `build/reports/jacoco/test/html/index.html`).

## Tela web de ensaio

`web/` é uma tela Vite para ensaiar a noite de jogo (veja `web/README.md`).
