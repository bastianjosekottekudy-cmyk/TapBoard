//go:build !windows

package main

import "unsafe"

type trayController struct{}

func attachSystemTray(hwnd unsafe.Pointer, onQuit func()) *trayController {
	return &trayController{}
}

func (t *trayController) HideToTray()  {}
func (t *trayController) ShowWindow() {}
func (t *trayController) Quit()       {}
