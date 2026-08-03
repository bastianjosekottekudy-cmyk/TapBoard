package main

import (
	"fmt"
	"math/rand"
	"os"
	"strings"
	"time"

	"github.com/jchv/go-webview2"
)

func main() {
	if len(os.Args) > 1 && os.Args[1] == "--ensure-firewall" {
		if err := InstallFirewallRules(); err != nil {
			_ = messageBox("TapBoard", "Firewall setup failed:\n"+err.Error())
			os.Exit(1)
		}
		_ = messageBox("TapBoard", "Network access enabled.\nYou can close this and use TapBoard Companion.")
		return
	}

	pin := os.Getenv("TAPBOARD_PIN")
	if pin == "" {
		pin = fmt.Sprintf("%06d", rand.New(rand.NewSource(time.Now().UnixNano())).Intn(1_000_000))
	}
	server := NewCompanionServer(pin)

	w := webview2.NewWithOptions(webview2.WebViewOptions{
		Debug:     false,
		AutoFocus: true,
		WindowOptions: webview2.WindowOptions{
			Title:  "TapBoard Companion",
			Width:  440,
			Height: 640,
			Center: true,
		},
	})
	if w == nil {
		fmt.Fprintln(os.Stderr, "WebView2 runtime required. Install the Evergreen WebView2 Runtime from Microsoft.")
		os.Exit(1)
	}
	defer w.Destroy()

	tray := attachSystemTray(w.Window(), func() {
		server.Stop()
		w.Dispatch(func() {
			w.Terminate()
		})
	})

	refreshUI := func() {
		running, status, client := server.Status()
		ips := strings.Join(server.IPs, ", ")
		if ips == "" {
			ips = "No LAN IP"
		}
		fw := "needs setup"
		if FirewallReady() {
			fw = "ready"
		}
		btn := "Start"
		if running {
			btn = "Stop"
		}
		js := fmt.Sprintf(
			`window.__apply(%q,%q,%q,%q,%q,%q,%q)`,
			server.HostName, ips, server.Pin, status, client, fw, btn,
		)
		w.Eval(js)
	}

	server.SetOnChange(func() {
		w.Dispatch(refreshUI)
	})

	_ = w.Bind("startStop", func() {
		running, _, _ := server.Status()
		if running {
			server.Stop()
			return
		}
		if err := server.Start(); err != nil {
			w.Dispatch(func() {
				w.Eval(fmt.Sprintf(`window.__toast(%q)`, err.Error()))
			})
		}
	})

	_ = w.Bind("copyPin", func() string {
		return server.Pin
	})

	_ = w.Bind("enableNetwork", func() string {
		if FirewallReady() {
			return "already"
		}
		if IsAdmin() {
			if err := InstallFirewallRules(); err != nil {
				return "error:" + err.Error()
			}
			return "ok"
		}
		if err := RelaunchElevatedFirewall(); err != nil {
			return "error:" + err.Error()
		}
		return "elevating"
	})

	_ = w.Bind("firewallStatus", func() string {
		if FirewallReady() {
			return "ready"
		}
		return "needs setup"
	})

	_ = w.Bind("minimizeToTray", func() {
		tray.HideToTray()
	})

	w.SetHtml(companionHTML)
	w.Dispatch(func() {
		refreshUI()
		// Auto firewall if admin
		go func() {
			if !FirewallReady() && IsAdmin() {
				_ = InstallFirewallRules()
				w.Dispatch(refreshUI)
			}
		}()
		go func() {
			time.Sleep(250 * time.Millisecond)
			if err := server.Start(); err != nil {
				w.Dispatch(func() {
					w.Eval(fmt.Sprintf(`window.__toast(%q)`, err.Error()))
				})
				return
			}
			w.Dispatch(refreshUI)
		}()
		// Poll firewall after elevation
		go func() {
			for i := 0; i < 60; i++ {
				time.Sleep(500 * time.Millisecond)
				if FirewallReady() {
					w.Dispatch(refreshUI)
					return
				}
			}
		}()
	})

	w.Run()
	server.Stop()
}

const companionHTML = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>TapBoard Companion</title>
<style>
  :root {
    --ink: #07141c;
    --elev: #0e2430;
    --teal: #2dd4bf;
    --teal-dim: #149e8c;
    --sand: #e7f2f0;
    --mist: #b7cfc9;
    --alert: #ff7a59;
  }
  * { box-sizing: border-box; }
  html, body {
    margin: 0; height: 100%;
    font-family: "Segoe UI", system-ui, sans-serif;
    background: radial-gradient(120% 80% at 10% 0%, #143342 0%, var(--ink) 55%);
    color: var(--sand);
  }
  .wrap {
    padding: 28px 24px 20px;
    min-height: 100%;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  h1 {
    margin: 0;
    font-size: 34px;
    letter-spacing: -0.04em;
    color: var(--teal);
  }
  .sub { color: var(--mist); margin-top: -6px; font-size: 14px; }
  .card {
    background: linear-gradient(160deg, rgba(45,212,191,0.08), transparent 40%), var(--elev);
    border: 1px solid rgba(45,212,191,0.22);
    border-radius: 18px;
    padding: 16px 18px;
  }
  .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--mist); }
  .value { font-size: 16px; margin-top: 4px; word-break: break-all; }
  .pin {
    font-size: 48px;
    font-weight: 700;
    letter-spacing: 0.18em;
    text-align: center;
    color: var(--teal);
    font-variant-numeric: tabular-nums;
    margin: 8px 0 4px;
  }
  .row { display: flex; gap: 10px; }
  button {
    appearance: none;
    border: 0;
    border-radius: 12px;
    padding: 12px 16px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    background: var(--teal);
    color: var(--ink);
  }
  button.secondary {
    background: transparent;
    color: var(--sand);
    border: 1px solid rgba(183,207,201,0.35);
  }
  button:hover { filter: brightness(1.06); }
  button:active { transform: translateY(1px); }
  .grow { flex: 1; }
  .status-dot {
    display: inline-block;
    width: 8px; height: 8px;
    border-radius: 50%;
    background: var(--teal);
    margin-right: 8px;
    box-shadow: 0 0 0 4px rgba(45,212,191,0.18);
  }
  .status-dot.warn { background: var(--alert); box-shadow: 0 0 0 4px rgba(255,122,89,0.18); }
  .hint { color: var(--mist); font-size: 13px; line-height: 1.45; }
  .toast {
    position: fixed; left: 16px; right: 16px; bottom: 16px;
    background: #1a3340; border: 1px solid rgba(255,122,89,0.5);
    color: var(--sand); padding: 12px 14px; border-radius: 12px;
    display: none;
  }
  .spacer { flex: 1; }
</style>
</head>
<body>
  <div class="wrap">
    <div>
      <h1>TapBoard</h1>
      <div class="sub">Wi‑Fi keyboard &amp; mouse receiver</div>
    </div>

    <div class="card">
      <div class="label">This PC</div>
      <div class="value" id="host">—</div>
      <div class="value" id="ips" style="color:var(--mist);font-size:13px;margin-top:6px">—</div>
    </div>

    <div class="card">
      <div class="label">Connection PIN</div>
      <div class="pin" id="pin">------</div>
      <div class="row">
        <button class="secondary grow" onclick="onCopy()">Copy PIN</button>
      </div>
    </div>

    <div class="card">
      <div class="label">Status</div>
      <div class="value" id="status"><span class="status-dot"></span>Starting…</div>
      <div class="value" id="client" style="color:var(--mist);font-size:13px;margin-top:6px"></div>
      <div class="value" id="fw" style="color:var(--mist);font-size:13px;margin-top:8px">Network access: —</div>
    </div>

    <div class="row">
      <button class="secondary grow" id="fwBtn" onclick="onNetwork()">Enable network access</button>
    </div>
    <div class="row">
      <button class="grow" id="startBtn" onclick="onStart()">Start</button>
    </div>
    <div class="row">
      <button class="secondary grow" onclick="onTray()">Minimize to tray</button>
    </div>

    <div class="hint">
      1. Tap <b>Enable network access</b> once (Windows may ask for permission — no manual firewall settings).<br/>
      2. On your phone open TapBoard → Wi‑Fi → Scan.<br/>
      3. Enter the PIN above.<br/>
      Minimize or close keeps TapBoard running in the system tray — right-click the tray icon to Quit.
    </div>
    <div class="spacer"></div>
  </div>
  <div class="toast" id="toast"></div>
<script>
  window.__apply = (host, ips, pin, status, client, fw, btn) => {
    document.getElementById('host').textContent = host || '—';
    document.getElementById('ips').textContent = ips || '—';
    document.getElementById('pin').textContent = pin || '------';
    const warn = /stop|error|fail/i.test(status);
    document.getElementById('status').innerHTML =
      '<span class="status-dot' + (warn ? ' warn' : '') + '"></span>' + (status || '—');
    document.getElementById('client').textContent = client ? ('Client: ' + client) : '';
    document.getElementById('fw').textContent = 'Network access: ' + fw;
    document.getElementById('startBtn').textContent = btn || 'Start';
    document.getElementById('fwBtn').style.display = (fw === 'ready') ? 'none' : 'block';
  };
  window.__toast = (msg) => {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 4000);
  };
  async function onCopy() {
    const pin = await window.copyPin();
    try { await navigator.clipboard.writeText(pin); window.__toast('PIN copied'); }
    catch { window.__toast(pin); }
  }
  async function onStart() { await window.startStop(); }
  async function onTray() { await window.minimizeToTray(); }
  async function onNetwork() {
    const r = await window.enableNetwork();
    if (r === 'ok' || r === 'already') {
      window.__toast('Network access ready');
      const fw = await window.firewallStatus();
      document.getElementById('fw').textContent = 'Network access: ' + fw;
      document.getElementById('fwBtn').style.display = (fw === 'ready') ? 'none' : 'block';
    } else if (r === 'elevating') {
      window.__toast('Approve the Windows prompt to finish setup…');
    } else if (r && r.startsWith('error:')) {
      window.__toast(r.slice(6));
    }
  }
</script>
</body>
</html>`
