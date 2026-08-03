package main

import (
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	protocolVersion = 1
	discoveryPort   = 19528
	sessionPort     = 19529
	discoverMagic   = "TAPBOARD_DISCOVER"
	serverVersion   = "1.0.0"
)

type CompanionServer struct {
	Pin      string
	HostName string
	IPs      []string

	mu        sync.Mutex
	running   bool
	status    string
	client    string
	udp       net.PacketConn
	tcp       net.Listener
	onChange  func()
}

func NewCompanionServer(pin string) *CompanionServer {
	host, _ := os.Hostname()
	return &CompanionServer{
		Pin:      pin,
		HostName: host,
		IPs:      localIPv4s(),
		status:   "Stopped",
	}
}

func (s *CompanionServer) SetOnChange(fn func()) {
	s.mu.Lock()
	s.onChange = fn
	s.mu.Unlock()
}

func (s *CompanionServer) notify() {
	s.mu.Lock()
	fn := s.onChange
	s.mu.Unlock()
	if fn != nil {
		fn()
	}
}

func (s *CompanionServer) Status() (running bool, status, client string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.running, s.status, s.client
}

func (s *CompanionServer) setStatus(status, client string) {
	s.mu.Lock()
	s.status = status
	if client != "" || status == "Waiting for phone…" || status == "Stopped" {
		s.client = client
	}
	s.mu.Unlock()
	s.notify()
}

func (s *CompanionServer) Start() error {
	s.mu.Lock()
	if s.running {
		s.mu.Unlock()
		return nil
	}
	s.mu.Unlock()

	s.IPs = localIPv4s()
	udp, err := net.ListenPacket("udp4", fmt.Sprintf("0.0.0.0:%d", discoveryPort))
	if err != nil {
		return fmt.Errorf("discovery port %d: %w", discoveryPort, err)
	}
	tcp, err := net.Listen("tcp4", fmt.Sprintf("0.0.0.0:%d", sessionPort))
	if err != nil {
		_ = udp.Close()
		return fmt.Errorf("session port %d: %w", sessionPort, err)
	}

	s.mu.Lock()
	s.udp = udp
	s.tcp = tcp
	s.running = true
	s.status = "Waiting for phone…"
	s.client = ""
	s.mu.Unlock()
	s.notify()

	go s.serveDiscovery(udp)
	go s.acceptSessions(tcp)
	return nil
}

func (s *CompanionServer) Stop() {
	s.mu.Lock()
	s.running = false
	udp := s.udp
	tcp := s.tcp
	s.udp = nil
	s.tcp = nil
	s.status = "Stopped"
	s.client = ""
	s.mu.Unlock()
	if udp != nil {
		_ = udp.Close()
	}
	if tcp != nil {
		_ = tcp.Close()
	}
	s.notify()
}

func (s *CompanionServer) serveDiscovery(pc net.PacketConn) {
	buf := make([]byte, 2048)
	for {
		n, addr, err := pc.ReadFrom(buf)
		if err != nil {
			return
		}
		msg := strings.TrimSpace(string(buf[:n]))
		if msg != discoverMagic {
			continue
		}
		ips := s.IPs
		host := ""
		if len(ips) > 0 {
			host = ips[0]
		}
		if ua, ok := addr.(*net.UDPAddr); ok {
			for _, ip := range ips {
				if sameClassC(ip, ua.IP.String()) {
					host = ip
					break
				}
			}
		}
		reply, _ := json.Marshal(map[string]any{
			"v":           protocolVersion,
			"type":        "discover_reply",
			"name":        s.HostName,
			"host":        host,
			"port":        sessionPort,
			"pinRequired": true,
		})
		_, _ = pc.WriteTo(reply, addr)
	}
}

func (s *CompanionServer) acceptSessions(ln net.Listener) {
	for {
		conn, err := ln.Accept()
		if err != nil {
			return
		}
		go s.handleSession(conn)
	}
}

func (s *CompanionServer) handleSession(conn net.Conn) {
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(2 * time.Minute))
	remote := conn.RemoteAddr().String()
	s.setStatus("Phone connecting…", remote)

	authed := false
	var mu sync.Mutex
	buttons := 0
	pin := s.Pin

	send := func(v any) error {
		mu.Lock()
		defer mu.Unlock()
		b, err := json.Marshal(v)
		if err != nil {
			return err
		}
		var hdr [4]byte
		binary.BigEndian.PutUint32(hdr[:], uint32(len(b)))
		if _, err := conn.Write(hdr[:]); err != nil {
			return err
		}
		_, err = conn.Write(b)
		return err
	}

	for {
		_ = conn.SetDeadline(time.Now().Add(5 * time.Minute))
		msg, err := readFrame(conn)
		if err != nil {
			s.setStatus("Waiting for phone…", "")
			return
		}
		v, _ := msg["v"].(float64)
		if int(v) != protocolVersion {
			_ = send(map[string]any{"v": protocolVersion, "type": "error", "reason": "unsupported_version"})
			return
		}
		switch msg["type"] {
		case "hello":
			_ = send(map[string]any{
				"v":             protocolVersion,
				"type":          "hello_ack",
				"server":        "TapBoard Companion",
				"serverVersion": serverVersion,
				"authRequired":  true,
			})
		case "auth":
			got, _ := msg["pin"].(string)
			if got == pin {
				authed = true
				_ = send(map[string]any{"v": protocolVersion, "type": "auth_ok"})
				s.setStatus("Connected", remote)
			} else {
				_ = send(map[string]any{"v": protocolVersion, "type": "auth_fail", "reason": "invalid_pin"})
				s.setStatus("Waiting for phone…", "")
				return
			}
		case "ping":
			_ = send(map[string]any{"v": protocolVersion, "type": "pong", "t": msg["t"]})
		case "goodbye":
			s.setStatus("Waiting for phone…", "")
			return
		case "mouse":
			if !authed {
				return
			}
			dx := asInt(msg["dx"])
			dy := asInt(msg["dy"])
			btn := asInt(msg["buttons"])
			wheel := asInt(msg["wheel"])
			hwheel := asInt(msg["hwheel"])
			if dx != 0 || dy != 0 {
				MoveMouse(dx, dy)
			}
			if wheel != 0 {
				ScrollVertical(wheel)
			}
			if hwheel != 0 {
				ScrollHorizontal(hwheel)
			}
			applyButtons(&buttons, btn)
		case "key":
			if !authed {
				return
			}
			HandleKey(asInt(msg["hid"]), asInt(msg["mods"]), msg["down"] == true)
		}
	}
}

func localIPv4s() []string {
	var out []string
	ifaces, err := net.Interfaces()
	if err != nil {
		return out
	}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 {
			continue
		}
		addrs, _ := iface.Addrs()
		for _, a := range addrs {
			var ip net.IP
			switch v := a.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil || ip.To4() == nil || ip.IsLoopback() {
				continue
			}
			out = append(out, ip.String())
		}
	}
	return out
}

func sameClassC(a, b string) bool {
	pa := strings.Split(a, ".")
	pb := strings.Split(strings.Split(b, ":")[0], ".")
	if len(pa) < 3 || len(pb) < 3 {
		return false
	}
	return pa[0] == pb[0] && pa[1] == pb[1] && pa[2] == pb[2]
}

func applyButtons(current *int, next int) {
	diff := *current ^ next
	for bit := 0; bit < 3; bit++ {
		mask := 1 << bit
		if diff&mask == 0 {
			continue
		}
		MouseButton(bit, next&mask != 0)
	}
	*current = next
}

func asInt(v any) int {
	switch t := v.(type) {
	case float64:
		return int(t)
	case json.Number:
		i, _ := t.Int64()
		return int(i)
	case string:
		i, _ := strconv.Atoi(t)
		return i
	default:
		return 0
	}
}

func readFrame(r io.Reader) (map[string]any, error) {
	var hdr [4]byte
	if _, err := io.ReadFull(r, hdr[:]); err != nil {
		return nil, err
	}
	n := binary.BigEndian.Uint32(hdr[:])
	if n == 0 || n > 1_000_000 {
		return nil, fmt.Errorf("bad frame length %d", n)
	}
	buf := make([]byte, n)
	if _, err := io.ReadFull(r, buf); err != nil {
		return nil, err
	}
	var msg map[string]any
	if err := json.Unmarshal(buf, &msg); err != nil {
		return nil, err
	}
	return msg, nil
}
