import websocket
import json
import time

VM_ID = "a1b2c3d4e5f678901234567890123456"
TOKEN = "testtoken"
WS_URL = f"ws://localhost:8080/ws/vm/{VM_ID}?token={TOKEN}"

def on_message(ws, message):
    print("收到消息:", message)

def on_error(ws, error):
    print("发生错误:", error)

def on_close(ws, close_status_code, close_msg):
    print("连接关闭:", close_status_code, close_msg)

def on_open(ws):
    print("连接已建立，发送CONNECT")
    connect_msg = {
        "type": "CONNECT",
        "id": f"client-{int(time.time()*1000)}-1",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime()),
        "vmId": VM_ID,
        "data": {},
        "signature": "testsig"
    }
    ws.send(json.dumps(connect_msg))

if __name__ == "__main__":
    ws = websocket.WebSocketApp(
        WS_URL,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    ws.run_forever()