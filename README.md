# QuiverQuant Signal Filter & Email Alert Service

A production-ready backend service (Kotlin/Spring Boot) that monitors the QuiverQuant Twitter account for congressional trading signals, classifies them using an LLM, and sends real-time email alerts plus daily summary digests.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           QuiverQuant Signal Service                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │  Ingestion   │───▶│Classification│───▶│   Alerting   │───▶│   Email   │ │
│  │   Service    │    │   Service    │    │   Service    │    │  Provider │ │
│  │  (5 min)     │    │  (1 min)     │    │  (30 sec)    │    │           │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └───────────┘ │
│         │                   │                                               │
│         │                   │            ┌──────────────┐    ┌───────────┐ │
│         │                   │            │   Summary    │───▶│   Email   │ │
│         │                   │            │   Service    │    │  Provider │ │
│         │                   │            │  (Daily)     │    │           │ │
│         │                   │            └──────────────┘    └───────────┘ │
│         ▼                   ▼                   │                           │
│  ┌─────────────────────────────────────────────┴────────────────────────┐  │
│  │                         PostgreSQL Database                           │  │
│  │  ┌────────────┐    ┌────────────┐    ┌──────────────────┐            │  │
│  │  │   tweets   │───▶│  signals   │    │ email_recipients │            │  │
│  │  └────────────┘    └────────────┘    └──────────────────┘            │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Tech Stack

- **Language**: Kotlin 1.9
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL 16 with Flyway migrations
- **Scheduling**: Spring Scheduling
- **HTTP Client**: OkHttp 4
- **Build**: Gradle with Kotlin DSL

## Data Flow

1. **Ingestion (every 5 minutes)**
   - Polls QuiverQuant Twitter account for new tweets
   - Stores raw tweet data in PostgreSQL
   - Tracks `sinceId` to avoid duplicate processing

2. **Classification (every 1 minute)**
   - Processes unclassified tweets through LLM
   - Extracts: signal strength, category, tickers, summary
   - Stores signals in database with classification metadata

3. **Alerting (every 30 seconds)**
   - Monitors for high-strength signals (≥0.7 by default)
   - Sends immediate email alerts to registered recipients
   - Includes signal summary, tickers, and link to original tweet

4. **Daily Summary (8 AM EST)**
   - Collects all signals from the past 24 hours
   - Generates LLM-based narrative summary
   - Sends digest email to all summary recipients

## Signal Categories

| Category | Description | Typical Strength |
|----------|-------------|------------------|
| `DIRECT_TRADE` | Individual politician trade disclosure | 0.8 - 1.0 |
| `AGGREGATE_TREND` | Patterns across multiple members | 0.6 - 0.85 |
| `POLICY_SIGNAL` | Legislative events impacting markets | 0.5 - 0.8 |
| `LOW_SIGNAL` | Promotional content, general updates | 0.0 - 0.3 |

## Project Structure

```
src/main/kotlin/com/quiverbot/
├── domain/                    # Core business logic (no external dependencies)
│   ├── entities/              # Tweet, Signal, EmailRecipient
│   ├── repositories/          # Repository interfaces (ports)
│   ├── services/              # Service interfaces (ports)
│   └── enums/                 # SignalCategory enum
├── infrastructure/            # Adapters (implementations of interfaces)
│   ├── database/              # JPA entities, repositories, adapters
│   ├── twitter/               # RapidAPI Twitter client
│   ├── llm/                   # OpenAI client
│   ├── email/                 # Sender.net client
│   └── config/                # Spring configuration
├── application/               # Use cases / application services
│   ├── services/              # Ingestion, Classification, Alerting, Summary
│   └── controllers/           # Health, Debug
└── QuiverBotApplication.kt    # Main entry point
```

## Getting Started

### Prerequisites

- JDK 17+
- Docker and Docker Compose (for database)
- Gradle (or use the wrapper)

### Option 1: Docker (Recommended)

The easiest way to run the service is with Docker Compose.

```bash
# Copy environment variables
cp .env.example .env

# Edit .env with your API keys

# Start everything (database + app)
docker compose up -d

# View logs
docker compose logs -f app

# Stop everything
docker compose down

# Stop and remove data
docker compose down -v
```

The services will be available at:
- **App**: http://localhost:3000
- **Health**: http://localhost:3000/health
- **Database**: localhost:5433

### Option 2: Local Development

#### Database Only in Docker

```bash
# Start just the database
docker compose up -d db

# Verify it's running
docker compose ps
```

#### Run the App Locally

```bash
# Copy environment variables
cp .env.example .env

# Edit .env with your configuration (use DB_PORT=5433)

# Build and run
./gradlew bootRun

# Or build a JAR
./gradlew bootJar
java -jar build/libs/quiver-bot-1.0.0.jar
```

### Health Checks

```bash
# Basic health check
curl http://localhost:3000/health
```

## Configuration

All configuration is via environment variables (or `application.yml`). See `.env.example` for all options.

### Required for Production

| Variable | Description |
|----------|-------------|
| `DB_*` | PostgreSQL connection details |
| `RAPIDAPI_KEY` | RapidAPI key for Twitter access |
| `QUIVERQUANT_REST_ID` | Twitter rest_id for QuiverQuant account |
| `OPENAI_API_KEY` | OpenAI API key |
| `SENDER_API_KEY` | Sender.net API key |
| `SENDER_CAMPAIGN_ID_ALERT` | Sender campaign ID for signal alerts |
| `SENDER_CAMPAIGN_ID_SUMMARY` | Sender campaign ID for daily summaries |

### Email Test Mode

For development without Sender.net credentials:

```env
EMAIL_TEST_MODE=true
```

This logs emails to the console instead of sending them.

### Tuning Signal Detection

```env
# Minimum signal strength to store (0.0-1.0)
SIGNAL_THRESHOLD=0.5

# Minimum strength for immediate alerts (0.0-1.0)
ALERT_THRESHOLD=0.7
```

### Feature Flags

```env
# Disable individual components
INGESTION_ENABLED=false     # Stop polling Twitter
CLASSIFICATION_ENABLED=false # Stop LLM processing
ALERTING_ENABLED=false      # Stop real-time alerts
SUMMARY_ENABLED=false       # Stop daily summaries
```

## Debug / Manual Testing

For local development and testing, you can disable the scheduled cron jobs and trigger each job manually via HTTP endpoints.

### Setup

1. Set in your `.env`:
```env
DISABLE_CRON_JOBS=true
EMAIL_TEST_MODE=true
```

2. Start the app:
```bash
./gradlew bootRun
```

### Debug Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/debug/trigger/ingestion` | Fetch new tweets from Twitter |
| POST | `/debug/trigger/classification` | Process unclassified tweets through LLM |
| POST | `/debug/trigger/alerting` | Send pending alert emails |
| POST | `/debug/trigger/summary` | Generate and send daily summary |
| POST | `/debug/trigger/full-pipeline` | Run all three: ingestion → classification → alerting |

### Curl Examples

**Trigger Ingestion:**
```bash
curl -X POST http://localhost:3000/debug/trigger/ingestion
```

**Trigger Classification:**
```bash
curl -X POST http://localhost:3000/debug/trigger/classification
```

**Trigger Alerting:**
```bash
curl -X POST http://localhost:3000/debug/trigger/alerting
```

**Trigger Daily Summary:**
```bash
curl -X POST http://localhost:3000/debug/trigger/summary
```

**Run Full Pipeline:**
```bash
curl -X POST http://localhost:3000/debug/trigger/full-pipeline
```

### Example Response

```json
{
  "success": true,
  "message": "Pipeline complete: 3 tweets → 2 signals → 2 emails",
  "results": {
    "tweetsIngested": 3,
    "signalsCreated": 2,
    "emailsSent": 2
  }
}
```

## Adding Email Recipients

Recipients must be added directly to the database:

```sql
INSERT INTO email_recipients (email, name, receives_alerts, receives_summary, is_active)
VALUES ('user@example.com', 'John Doe', true, true, true);
```

## Scheduled Jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| Tweet Ingestion | Every 5 minutes | Poll Twitter for new tweets |
| Classification | Every 1 minute | Process tweets through LLM |
| Alerting | Every 30 seconds | Send pending alerts |
| Daily Summary | 8 AM EST | Generate and send digest |

## Development

### Building

```bash
# Build JAR
./gradlew bootJar

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Database Migrations

Migrations are managed by Flyway and run automatically on startup. Migration files are in:
```
src/main/resources/db/migration/
```

To add a new migration, create a file like:
```
V2__add_new_column.sql
```

## Docker

### Building Images

```bash
# Build app image
docker build -t quiverbot .
```

### Docker Compose Services

| Service | Description | Port |
|---------|-------------|------|
| `db` | PostgreSQL 16 database | 5433 |
| `app` | QuiverBot application | 3000 |

### Running with External Database

To use an external database instead of the containerized one:

```bash
docker run -d \
  -p 3000:3000 \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  -e OPENAI_API_KEY=your-key \
  quiverbot
```

## Deployment Considerations

1. **Database**: Use a managed PostgreSQL service (RDS, Cloud SQL, etc.)
2. **Secrets**: Store API keys in a secret manager
3. **Monitoring**: The `/health` endpoint is suitable for Kubernetes probes
4. **Scaling**: This is a single-instance service (multiple instances would create duplicate alerts)
5. **Docker**: Use the provided `docker-compose.yml` or deploy the image to your container platform

## License

ISC
