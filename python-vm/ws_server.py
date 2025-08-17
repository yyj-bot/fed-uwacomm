import socket
import threading
import json
import time
import base64
import logging
from http import HTTPStatus
from urllib.parse import urlparse, parse_qs

import websockets
from websockets.sync.server import serve

# ========== 配置 ==========
WS_PORT = 8080
JWT_SECRET = "your_jwt_secret"  # 用于JWT校验

# ========== 日志 ==========
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(levelname)s: %(message)s')

# ========== 工具 ==========
def verify_jwt(token: str) -> bool:
    # TODO: 实现JWT校验（可用pyjwt等库）
    return True if token else False

def make_message(msg_type, vmId, data, msg_id=None, signature=None):
    return {
        "type": msg_type,
        "id": msg_id or f"server-{int(time.time()*1000)}",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
        "vmId": vmId,
        "data": data,
        "signature": signature or "base64_encoded_signature"
    }

# ========== 连接管理 ==========
class VMConnection:
    def __init__(self, ws, vmId):
        self.ws = ws
        self.vmId = vmId
        self.last_heartbeat = time.time()
        self.session_id = f"session-{int(time.time()*1000)}"

connections = {}
connections_lock = threading.Lock()

# ========== 消息处理 ==========
def handle_message(msg, conn: VMConnection):
    msg_type = msg.get("type")
    if msg_type == "CONNECT":
        # 连接确认
        ack = make_message(
            "CONNECT_ACK", conn.vmId,
            data={
                "sessionId": conn.session_id,
                "serverTime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
                "heartbeatInterval": 30,
                "maxMessageSize": 10*1024*1024,
                "supportedFeatures": ["ENCRYPTION", "COMPRESSION", "BATCH_OPERATIONS"]
            }
        )
        conn.ws.send(json.dumps(ack))
        logging.info(f"CONNECT_ACK sent to {conn.vmId}")
    elif msg_type == "HEARTBEAT":
        conn.last_heartbeat = time.time()
        ack = make_message("HEARTBEAT_ACK", conn.vmId, data={
            "serverTime": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
            "nextHeartbeat": 30,
            "systemStatus": "NORMAL"
        })
        conn.ws.send(json.dumps(ack))
    elif msg_type in ("VM_START", "VM_STOP", "TRAINING_START", "TRAINING_STOP", "STATUS_QUERY"):
        # TODO: 实现命令/训练/状态处理
        logging.info(f"Received {msg_type} from {conn.vmId}: {msg.get('data')}")
        # 示例：直接回显
        resp_type = {
            "VM_START": "VM_START_ACK",
            "VM_STOP": "VM_STOP_ACK",
            "TRAINING_START": "TRAINING_START_ACK",
            "TRAINING_STOP": "TRAINING_STOP_ACK",
            "STATUS_QUERY": "STATUS_RESPONSE"
        }[msg_type]
        resp = make_message(resp_type, conn.vmId, data={"status": "OK"})
        conn.ws.send(json.dumps(resp))
    elif msg_type == "ERROR":
        logging.error(f"Error from {conn.vmId}: {msg.get('data')}")
    else:
        logging.warning(f"Unknown message type: {msg_type}")

# ========== WebSocket主循环 ==========
def ws_handler(ws):
    # 认证
    path = ws.path
    query = urlparse(path).query
    params = parse_qs(query)
    token = params.get("token", [None])[0]
    vmId = ws.path.split("/")[-1].split("?")[0]
    if not token or not verify_jwt(token):
        ws.close(code=HTTPStatus.UNAUTHORIZED, reason="Invalid JWT")
        logging.warning(f"Connection rejected for vmId={vmId}")
        return
    conn = VMConnection(ws, vmId)
    with connections_lock:
        connections[vmId] = conn
    logging.info(f"VM connected: {vmId}")
    try:
        for msg_str in ws:
            try:
                msg = json.loads(msg_str)
                handle_message(msg, conn)
            except Exception as e:
                logging.error(f"Message handling error: {e}")
    finally:
        with connections_lock:
            if vmId in connections:
                del connections[vmId]
        logging.info(f"VM disconnected: {vmId}")

# ========== 启动服务器 ==========
def main():
    logging.info(f"WebSocket server starting on port {WS_PORT}...")
    with serve(ws_handler, "0.0.0.0", WS_PORT):
        logging.info("Server started. Press Ctrl+C to stop.")
        while True:
            time.sleep(1)

if __name__ == "__main__":
    main()
