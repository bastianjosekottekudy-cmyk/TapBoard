//go:build !windows

package main

func IsAdmin() bool { return true }

func FirewallReady() bool { return true }

func InstallFirewallRules() error { return nil }

func RelaunchElevatedFirewall() error { return nil }
