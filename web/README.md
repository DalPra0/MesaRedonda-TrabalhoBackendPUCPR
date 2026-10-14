# Tela de ensaio

Página única para ensaiar a apresentação de 29/10. O Vite publica a página e encaminha `/api` para o Spring, na mesma origem. O backend não precisa de CORS.

## Backend

Na raiz do repositório, na branch `main`:

```sh
./gradlew bootRun
```

A API fica em `http://localhost:8080/api`.

Login da aula, criado no `Bootstrapper`: `admin@authserver.com` / `admin`.

## Página

Nesta pasta `web/`:

```sh
npm install
npm run dev
```

Abre em `http://127.0.0.1:43123`.

A zona da fila só aparece se `GET /api/game-nights/{id}/music-room` existir. Na `main` essa rota não está no servidor, então a zona fica escondida.
