"""Testes de integração locais das regras; requer o JAR oficial do Firestore Emulator.

python tools/test_firestore_rules.py --java java --emulator-jar tmp/firebase/firestore.jar
Não usa o projeto real, google-services.json ou credenciais do usuário.
"""
import argparse
import base64
import json
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

PROJECT = "demo-kanban-grupo2"
HOST = "http://127.0.0.1:8686"
DB = f"projects/{PROJECT}/databases/(default)"
DOCUMENTS = f"{DB}/documents"
ROOT = Path(__file__).resolve().parents[1]
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def token(uid):
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip("=")
    return encode({"alg": "none", "typ": "JWT"}) + "." + encode({
        "sub": uid, "user_id": uid, "aud": PROJECT,
        "iss": f"https://securetoken.google.com/{PROJECT}",
        "iat": int(time.time()), "exp": int(time.time()) + 3600,
        "firebase": {"sign_in_provider": "password", "identities": {}},
    }) + "."


def request(method, path, payload=None, actor="owner"):
    headers = {"Content-Type": "application/json"}
    if actor:
        headers["Authorization"] = "Bearer " + (actor if actor == "owner" else token(actor))
    req = urllib.request.Request(HOST + path, data=None if payload is None else json.dumps(payload).encode(),
                                 headers=headers, method=method)
    try:
        with HTTP.open(req, timeout=10) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def value(v):
    if v is None: return {"nullValue": None}
    if isinstance(v, bool): return {"booleanValue": v}
    if isinstance(v, int): return {"integerValue": str(v)}
    if isinstance(v, list): return {"arrayValue": {"values": [value(x) for x in v]}}
    return {"stringValue": v}


def fields(data):
    return {key: value(v) for key, v in data.items()}


def put(path, data, actor="owner"):
    return request("PATCH", f"/v1/{DOCUMENTS}/{path}", {"fields": fields(data)}, actor)


def write(path, data, timestamp=None):
    w = {"update": {"name": f"{DOCUMENTS}/{path}", "fields": fields(data)}}
    if timestamp:
        w["updateTransforms"] = [{"fieldPath": timestamp, "setToServerValue": "REQUEST_TIME"}]
    return w


def commit(writes, actor):
    return request("POST", f"/v1/{DOCUMENTS}:commit", {"writes": writes}, actor)


def run_tests():
    total = 0
    def expect(title, expected, response):
        nonlocal total
        code, body = response
        assert code == expected, f"{title}: esperado {expected}, recebido {code}: {body}"
        total += 1
        print(f"PASS {title}", flush=True)

    expect("seed operador", 200, put("usuarios/op", {"perfil": "operador"}))
    expect("seed logistica", 200, put("usuarios/log", {"perfil": "logistica"}))
    posto = {"nome": "Posto 07", "setor": "Montagem", "ativo": True}
    expect("seed posto", 200, put("postos/posto07", posto))
    expect("seed inativo", 200, put("postos/inativo", {**posto, "ativo": False}))
    expect("seed insumo", 200, put("insumos/parafuso", {
        "nome": "Parafuso", "codigo": "PAR", "unidade": "Caixa", "postoIds": ["posto07", "inativo"]}))
    expect("anonimo bloqueado", 403, request("GET", f"/v1/{DOCUMENTS}/postos/posto07", actor=None))
    expect("sem perfil bloqueado", 403, request("GET", f"/v1/{DOCUMENTS}/postos/posto07", actor="pendente"))
    expect("perfil proprio legivel", 200, request("GET", f"/v1/{DOCUMENTS}/usuarios/op", actor="op"))
    expect("perfil alheio bloqueado", 403, request("GET", f"/v1/{DOCUMENTS}/usuarios/log", actor="op"))
    expect("autopromocao bloqueada", 403, put("usuarios/op", {"perfil": "logistica"}, "op"))
    expect("cadastro por operador bloqueado", 403, put("postos/novo", posto, "op"))
    expect("cadastro por logistica permitido", 200, put("postos/novo", posto, "log"))

    cartao = {"postoId": "posto07", "postoNome": "Posto 07", "insumoId": "parafuso",
              "insumoNome": "Parafuso", "urgencia": "alta", "status": "aberto", "abertoEm": None,
              "emRotaEm": None, "entregueEm": None}
    path = "cartoes/posto07_parafuso"
    expect("logistica nao abre pedido", 403, commit([write(path, cartao, "abertoEm")], "log"))
    expect("posto inativo rejeitado", 403, commit([write("cartoes/inativo_parafuso", {**cartao, "postoId": "inativo"}, "abertoEm")], "op"))
    expect("insumo nao vinculado rejeitado", 403, commit([write("cartoes/novo_parafuso", {**cartao, "postoId": "novo"}, "abertoEm")], "op"))
    expect("operador abre pedido", 200, commit([write(path, cartao, "abertoEm")], "op"))
    expect("duplicado nao sobrescreve pedido", 403, commit([write(path, cartao, "abertoEm")], "op"))
    expect("confirmacao antecipada bloqueada", 403, request("DELETE", f"/v1/{DOCUMENTS}/{path}", actor="op"))

    def mover(status, campo, actor):
        w = write(path, {"status": status}, campo)
        w["updateMask"] = {"fieldPaths": ["status"]}
        return commit([w], actor)
    expect("nao pula em rota", 403, mover("entregue", "entregueEm", "log"))
    expect("operador nao move logistica", 403, mover("em_rota", "emRotaEm", "op"))
    expect("logistica move em rota", 200, mover("em_rota", "emRotaEm", "log"))
    expect("logistica marca entregue", 200, mover("entregue", "entregueEm", "log"))
    _, original = request("GET", f"/v1/{DOCUMENTS}/{path}", actor="op")
    historico = dict(original["fields"])
    historico["concluidoEm"] = {"timestampValue": datetime.now(timezone.utc).isoformat()}
    historico["minutosTotal"] = {"integerValue": "0"}
    arquivo = {"update": {"name": f"{DOCUMENTS}/atendimentos/teste", "fields": historico}}
    expect("historico sem conclusao bloqueado", 403, commit([arquivo], "op"))
    expect("confirmacao arquiva e remove ativo", 200, commit([arquivo, {"delete": f"{DOCUMENTS}/{path}"}], "op"))
    expect("ativo removido", 404, request("GET", f"/v1/{DOCUMENTS}/{path}", actor="op"))
    expect("historico preservado", 200, request("GET", f"/v1/{DOCUMENTS}/atendimentos/teste", actor="log"))
    expect("historico imutavel", 403, request("DELETE", f"/v1/{DOCUMENTS}/atendimentos/teste", actor="op"))
    expect("nova reposicao apos conclusao", 200, commit([write(path, cartao, "abertoEm")], "op"))
    print(f"{total} verificacoes passaram.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", default="java")
    parser.add_argument("--emulator-jar", required=True)
    args = parser.parse_args()
    logs = ROOT / "tmp/firebase/rules-test.log"
    logs.parent.mkdir(parents=True, exist_ok=True)
    with logs.open("w", encoding="utf-8") as log:
        process = subprocess.Popen([args.java, "-jar", str(Path(args.emulator_jar).resolve()),
            "--host", "127.0.0.1", "--port", "8686", "--project_id", PROJECT,
            "--single_project_mode", "true", "--rules", str(ROOT / "firestore.rules")],
            stdout=log, stderr=log, cwd=ROOT,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        try:
            deadline = time.monotonic() + 45
            while True:
                if process.poll() is not None:
                    raise RuntimeError(f"Emulador encerrou. Consulte {logs}")
                try:
                    request("GET", f"/v1/{DOCUMENTS}/usuarios/health")
                    break
                except (OSError, urllib.error.URLError):
                    if time.monotonic() > deadline: raise TimeoutError(f"Emulador indisponivel. Consulte {logs}")
                    time.sleep(.5)
            run_tests()
        finally:
            process.terminate()
            try: process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
