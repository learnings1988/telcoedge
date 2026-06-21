CREATE INDEX idx_usage_event_sub_type_time
ON usage_events (subscriber_id, usage_type, processed_at desc );