"""
虚拟机API异常和错误码定义
"""

from enum import Enum


class VMApiErrorCode(Enum):
    SUCCESS = 200
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    CONFLICT = 409
    VALIDATION_FAILED = 422
    SERVER_ERROR = 500
    GATEWAY_ERROR = 502
    SERVICE_UNAVAILABLE = 503
    # 业务错误码
    VM_NOT_FOUND = "VM_NOT_FOUND"
    VM_ALREADY_EXISTS = "VM_ALREADY_EXISTS"
    VM_INVALID_ID = "VM_INVALID_ID"
    VM_INVALID_STATUS = "VM_INVALID_STATUS"
    VM_CONNECTION_FAILED = "VM_CONNECTION_FAILED"
    VM_OPERATION_TIMEOUT = "VM_OPERATION_TIMEOUT"
    VM_INSUFFICIENT_RESOURCES = "VM_INSUFFICIENT_RESOURCES"
    VM_SECURITY_ERROR = "VM_SECURITY_ERROR"


class VMApiError(Exception):
    def __init__(self, code, message, data=None):
        super().__init__(f"[VMApiError {code}] {message}")
        self.code = code
        self.message = message
        self.data = data


def parse_vm_api_response(resp_json):
    code = resp_json.get("code")
    if code != 200:
        raise VMApiError(code, resp_json.get("message"), resp_json.get("data"))
    return resp_json.get("data")

