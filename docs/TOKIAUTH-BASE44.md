# TokiAuth Original: compra pelo site (Base44)

O jogo **não pede mais para colar chave**. A compra termina no site do Tokito
(`https://tokitolauncher.base44.app`), que **assina a licença e traz o jogador de volta pro jogo**.
A conta vira **Original** sozinha.

```
Tokito Launcher ── botão "Comprar" ──▶ site (Base44) ── pagamento ──▶ backend assina a licença
        ▲                                                                   │
        └──────────── tokito://tokiauth/activate?license=TKA1... ◀──────────┘
```

## 1. O que o jogo manda para o site

O botão **Comprar** abre:

```
https://tokitolauncher.base44.app/?player=<nome>&return=<link de volta>&price=3
```

| Parâmetro | O que é |
|-----------|---------|
| `player`  | nome do jogador (a licença vale só para esse nome, sem diferenciar maiúsculas) |
| `return`  | link para voltar pro jogo: `tokito://tokiauth/activate` (Tokito Launcher) ou `tokitopreview://tokiauth/activate` (Tokito Preview). **Use exatamente o que veio**, só acrescente `?license=...` |
| `price`   | valor em reais (hoje 3) |

## 2. O que o site precisa fazer

1. Ler `player` e `return` da URL e guardar (por exemplo na sessão ou no pedido).
2. Cobrar o jogador (Pix, Stripe etc.).
3. **Só depois que o servidor confirmar o pagamento** (webhook ou consulta ao provedor), chamar a função de backend `signLicense(player)` (abaixo).
4. Redirecionar para `return + "?license=" + encodeURIComponent(license)`.
   Também mostre um botão grande **"Voltar para o Tokito Launcher"** com esse mesmo link (o Chrome às vezes bloqueia redirecionamento automático para um app sem toque do usuário) e a chave em texto para copiar (o jogo ainda aceita colar a chave em "Já tenho uma chave").

Nunca assine a licença no navegador nem na página de "sucesso" sem confirmar o pagamento no servidor.

## 3. Função que assina a licença (backend, Deno / Node 20+)

A chave privada é o segredo **`TOKIAUTH_SHOP_KEY`** (conteúdo do arquivo `tokiauth_shop_private_pkcs8.pem`,
que começa com `-----BEGIN PRIVATE KEY-----`). **Ela fica só no backend do Base44, nunca no navegador e nunca no GitHub.**

```js
const b64url = (bytes) =>
  btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

export async function signLicense(player) {
  const pem = Deno.env.get("TOKIAUTH_SHOP_KEY"); // no Node: process.env.TOKIAUTH_SHOP_KEY
  const der = Uint8Array.from(
    atob(pem.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "")),
    (c) => c.charCodeAt(0),
  );
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "ECDSA", namedCurve: "P-256" }, false, ["sign"]);
  const payload = new TextEncoder().encode(
    JSON.stringify({ n: player.trim().toLowerCase(), k: "std", i: Math.floor(Date.now() / 1000) }),
  );
  const signature = await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, payload);
  return `TKA1.${b64url(payload)}.${b64url(signature)}`;
}
```

Formato da licença: `TKA1.<payload em base64url>.<assinatura em base64url>`. O jogo confere a assinatura no aparelho
(ECDSA P-256 / SHA-256, aceita a assinatura crua de 64 bytes do WebCrypto). A chave do site **só** pode emitir licença
comum (`"k": "std"`); a licença `admin` só existe com a chave do admin (workflow `tokiauth-license.yml`).

## 4. Prompt pronto para colar no Base44

> Crie o fluxo de compra do "TokiAuth Original" do Tokito Launcher.
> A página inicial recebe `?player=<nome>&return=<link>&price=3`. Mostre o nome do jogador (somente leitura) e o preço R$ 3,00 (pagamento único) e um botão "Comprar".
> Depois que o servidor CONFIRMAR o pagamento, rode uma função de backend que gera a licença com o código `signLicense(player)` da documentação (segredo `TOKIAUTH_SHOP_KEY`), guarde a licença no pedido e redirecione para `return + "?license=" + encodeURIComponent(licença)`.
> Na página de sucesso mostre também um botão grande "Voltar para o Tokito Launcher" com o mesmo link e a chave em texto com botão de copiar. Se o pagamento falhar, não gere nada.
> Nunca assine a licença no navegador e nunca mostre o segredo.

## 5. Testar sem pagar

Com a chave do site é possível gerar uma licença de teste para um nome qualquer, abrir no celular
`tokito://tokiauth/activate?license=TKA1...` e ver a conta daquele nome virar Original.

## 6. Limites honestos

- TokiAuth Original **não é** uma conta Mojang / Microsoft: servidores que checam o login premium não a aceitam.
- A conferência é feita no aparelho (sem servidor). Quem editar o app consegue burlar; isso protege contra
  compartilhamento casual, não contra quem modifica o launcher.
- Se a chave privada do site vazar, gere outra, troque a chave pública em `TokiAuthLicense.kt` e lance uma versão nova.
