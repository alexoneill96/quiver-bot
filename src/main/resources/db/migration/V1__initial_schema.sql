-- Initial database schema for QuiverBot

-- Tweets table: stores raw tweets from QuiverQuant
CREATE TABLE tweets (
    id VARCHAR(255) PRIMARY KEY,
    text TEXT NOT NULL,
    author_username VARCHAR(255) NOT NULL,
    posted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ingested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    url VARCHAR(512) NOT NULL,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tweets_is_processed ON tweets(is_processed);
CREATE INDEX idx_tweets_posted_at ON tweets(posted_at DESC);

-- Signals table: stores LLM classification results
CREATE TABLE signals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tweet_id VARCHAR(255) NOT NULL REFERENCES tweets(id),
    is_signal BOOLEAN NOT NULL,
    signal_strength DOUBLE PRECISION NOT NULL,
    category VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL,
    classified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    included_in_summary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_signals_is_signal ON signals(is_signal);
CREATE INDEX idx_signals_alert_sent ON signals(alert_sent);
CREATE INDEX idx_signals_classified_at ON signals(classified_at DESC);

-- Signal tickers: stores tickers associated with each signal
CREATE TABLE signal_tickers (
    signal_id UUID NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
    ticker VARCHAR(20) NOT NULL
);

CREATE INDEX idx_signal_tickers_signal_id ON signal_tickers(signal_id);

-- Email recipients table: stores recipients for alerts and summaries
CREATE TABLE email_recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    receives_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    receives_summary BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_recipients_active_alerts ON email_recipients(is_active, receives_alerts);
CREATE INDEX idx_email_recipients_active_summary ON email_recipients(is_active, receives_summary);
