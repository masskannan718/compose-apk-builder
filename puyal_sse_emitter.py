import time
import json

def format_sse(event, data):
    return f"event: {event}\ndata: {json.dumps(data)}\n\n"

def stream_telemetry_generator():
    yield format_sse("init", {"status": "CONNECTED", "timestamp": time.time()})
    yield format_sse("heartbeat", {"ping": "pong", "ram_target": "<15MB"})

if __name__ == "__main__":
    for payload in stream_telemetry_generator():
        print(payload.strip())
