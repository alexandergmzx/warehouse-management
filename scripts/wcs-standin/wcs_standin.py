#!/usr/bin/env python3
"""Scripted agv-fleet-controller (WCS) stand-in for the MFC work package
consumer-proof gate (PLAN.md gate 5; ADR 0011; TELEGRAMS.md).

agv-fleet-controller does not exist yet in this ecosystem's sequencing
(../ECOSYSTEM.md: it is step 3, after warehouse-android's walking skeleton
and this repository's MFC work package). Until it does, this script acts in
its name: it listens for TRANSPORT telegrams the WMS dispatches, then logs
in as the seeded WCS user (wcs01/AGV-FC-01, db/devdata/V2_1__seed_wcs_client.sql)
and confirms the mission ACCEPTED then COMPLETED over the real /api/v1
surface -- proving the whole loop end to end, not just the WMS side of it.

Stdlib only, matching this work package's "no new dependency" scope
(ADR 0011). Not a production WCS: no persistence, no concurrency handling,
no retry -- a scripted stand-in, not a fleet controller.

Usage:
    python3 wcs_standin.py [--listen-port 8090] [--wms-base-url http://localhost:8080]
                            [--username wcs01] [--password wcs01pass] [--device-code AGV-FC-01]
"""

import argparse
import json
import sys
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer


def log(message: str) -> None:
    print(f"[wcs-standin] {time.strftime('%Y-%m-%dT%H:%M:%S%z')} {message}", flush=True)


class WmsClient:
    def __init__(self, base_url: str, username: str, password: str, device_code: str):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.device_code = device_code
        self.token = None

    def login(self) -> None:
        body = json.dumps({
            "username": self.username,
            "password": self.password,
            "deviceCode": self.device_code,
        }).encode("utf-8")
        request = urllib.request.Request(
            f"{self.base_url}/api/v1/auth/login", data=body, method="POST",
            headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = json.loads(response.read())
        self.token = payload["token"]
        log(f"logged in as {self.username}/{self.device_code}")

    def confirm(self, mission_id: int, state: str, reason: str = None) -> dict:
        body = {"state": state, "occurredAt": time.strftime("%Y-%m-%dT%H:%M:%S%z")}
        if reason:
            body["reason"] = reason
        data = json.dumps(body).encode("utf-8")
        url = f"{self.base_url}/api/v1/mfc/missions/{mission_id}/confirmations"
        request = urllib.request.Request(
            url, data=data, method="POST",
            headers={"Content-Type": "application/json", "Authorization": f"Bearer {self.token}"})
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                result = json.loads(response.read())
                log(f"confirmed mission={mission_id} state={state} -> {response.status} {result}")
                return result
        except urllib.error.HTTPError as exc:
            body_text = exc.read().decode("utf-8", errors="replace")
            log(f"confirmation REJECTED mission={mission_id} state={state} -> {exc.code} {body_text}")
            raise


def make_handler(wms: WmsClient, transit_seconds: float):
    class MissionHandler(BaseHTTPRequestHandler):
        # Java's HttpClient (Spring RestClient's default) speaks HTTP/1.1 and
        # keeps connections alive; answering as HTTP/1.0 (this class's
        # inherited default) confuses that on retries. Match it explicitly.
        protocol_version = "HTTP/1.1"

        def do_POST(self):
            if self.path != "/missions":
                self.send_response(404)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
            try:
                body = self._read_body()
                telegram = json.loads(body)
            except Exception as exc:  # noqa: BLE001 - always answer, never hang the client
                log(f"failed to read/parse request body: {exc!r}")
                self.send_response(400)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
            log(f"received telegram: {telegram}")

            self.send_response(200)
            self.send_header("Content-Length", "0")
            self.end_headers()

            threading.Thread(target=self._execute_mission, args=(telegram,), daemon=True).start()

        def _read_body(self) -> bytes:
            """Content-Length is the common case; fall back to de-chunking
            Transfer-Encoding: chunked, which Python's http.server does not
            decode automatically."""
            content_length = self.headers.get("Content-Length")
            if content_length is not None:
                return self.rfile.read(int(content_length))
            if (self.headers.get("Transfer-Encoding") or "").lower() == "chunked":
                chunks = []
                while True:
                    size_line = self.rfile.readline().strip()
                    size = int(size_line.split(b";")[0], 16)
                    if size == 0:
                        self.rfile.readline()  # trailing CRLF after the 0-size chunk
                        break
                    chunks.append(self.rfile.read(size))
                    self.rfile.read(2)  # CRLF after each chunk
                return b"".join(chunks)
            return b""

        def _execute_mission(self, telegram: dict) -> None:
            mission_id = telegram["missionId"]
            if telegram.get("missionType") != "TRANSPORT":
                log(f"mission={mission_id} type={telegram.get('missionType')} not TRANSPORT; ignoring")
                return
            if wms.token is None:
                wms.login()
            wms.confirm(mission_id, "ACCEPTED")
            log(f"mission={mission_id} simulating transit ({transit_seconds}s): "
                f"{telegram['sourceLocationCode']} -> {telegram['destinationLocationCode']}")
            time.sleep(transit_seconds)
            wms.confirm(mission_id, "COMPLETED")

        def log_message(self, format, *args):  # noqa: A002 - stdlib signature
            pass  # replaced by the structured log() calls above

    return MissionHandler


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--listen-port", type=int, default=8090)
    parser.add_argument("--wms-base-url", default="http://localhost:8080")
    parser.add_argument("--username", default="wcs01")
    parser.add_argument("--password", default="wcs01pass")
    parser.add_argument("--device-code", default="AGV-FC-01")
    parser.add_argument("--transit-seconds", type=float, default=2.0)
    args = parser.parse_args()

    wms = WmsClient(args.wms_base_url, args.username, args.password, args.device_code)
    handler = make_handler(wms, args.transit_seconds)
    server = HTTPServer(("0.0.0.0", args.listen_port), handler)
    log(f"listening on :{args.listen_port}/missions, confirming against {args.wms_base_url}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log("shutting down")
    return 0


if __name__ == "__main__":
    sys.exit(main())
