//go:build windows

package main

import (
	"fmt"
	"os"
	"os/exec"
	"strings"
	"syscall"

	"golang.org/x/sys/windows"
)

const (
	fwRuleTCP = "TapBoard Companion TCP"
	fwRuleUDP = "TapBoard Companion UDP"
)

func IsAdmin() bool {
	var sid *windows.SID
	err := windows.AllocateAndInitializeSid(
		&windows.SECURITY_NT_AUTHORITY,
		2,
		windows.SECURITY_BUILTIN_DOMAIN_RID,
		windows.DOMAIN_ALIAS_RID_ADMINS,
		0, 0, 0, 0, 0, 0,
		&sid,
	)
	if err != nil {
		return false
	}
	defer windows.FreeSid(sid)
	token := windows.Token(0)
	member, err := token.IsMember(sid)
	return err == nil && member
}

func FirewallReady() bool {
	out, err := exec.Command("netsh", "advfirewall", "firewall", "show", "rule", "name="+fwRuleTCP).CombinedOutput()
	if err != nil || !strings.Contains(string(out), fwRuleTCP) {
		return false
	}
	// Require "Any" profile or explicit Public coverage — old Private-only rules fail on Public Wi‑Fi.
	text := string(out)
	if strings.Contains(text, "Public") || strings.Contains(strings.ToLower(text), "any") ||
		(strings.Contains(text, "Domain") && strings.Contains(text, "Private") && strings.Contains(text, "Public")) {
		out2, err2 := exec.Command("netsh", "advfirewall", "firewall", "show", "rule", "name="+fwRuleUDP).CombinedOutput()
		return err2 == nil && strings.Contains(string(out2), fwRuleUDP)
	}
	// Private+Domain only → treat as not ready so UI prompts upgrade
	return false
}

func InstallFirewallRules() error {
	_ = exec.Command("netsh", "advfirewall", "firewall", "delete", "rule", "name="+fwRuleTCP).Run()
	_ = exec.Command("netsh", "advfirewall", "firewall", "delete", "rule", "name="+fwRuleUDP).Run()

	tcp := exec.Command("netsh", "advfirewall", "firewall", "add", "rule",
		"name="+fwRuleTCP,
		"dir=in",
		"action=allow",
		"protocol=TCP",
		fmt.Sprintf("localport=%d", sessionPort),
		"profile=any",
		"enable=yes",
	)
	if out, err := tcp.CombinedOutput(); err != nil {
		return fmt.Errorf("TCP rule: %v (%s)", err, strings.TrimSpace(string(out)))
	}

	udp := exec.Command("netsh", "advfirewall", "firewall", "add", "rule",
		"name="+fwRuleUDP,
		"dir=in",
		"action=allow",
		"protocol=UDP",
		fmt.Sprintf("localport=%d", discoveryPort),
		"profile=any",
		"enable=yes",
	)
	if out, err := udp.CombinedOutput(); err != nil {
		return fmt.Errorf("UDP rule: %v (%s)", err, strings.TrimSpace(string(out)))
	}

	// Best-effort: mark active Wi‑Fi/Ethernet as Private so Windows is less aggressive.
	_ = exec.Command("powershell", "-NoProfile", "-Command",
		`Get-NetConnectionProfile | Where-Object { $_.IPv4Connectivity -ne 'NoTraffic' } | Set-NetConnectionProfile -NetworkCategory Private -ErrorAction SilentlyContinue`,
	).Run()

	return nil
}

func RelaunchElevatedFirewall() error {
	exe, err := os.Executable()
	if err != nil {
		return err
	}
	verb, _ := syscall.UTF16PtrFromString("runas")
	file, _ := syscall.UTF16PtrFromString(exe)
	args, _ := syscall.UTF16PtrFromString("--ensure-firewall")
	cwd, _ := syscall.UTF16PtrFromString("")
	showCmd := int32(1)
	return windows.ShellExecute(0, verb, file, args, cwd, showCmd)
}
