//go:build !windows

package main

import "log"

func MoveMouse(dx, dy int) {
	log.Printf("mouse move %d,%d (input injection not implemented on this OS build)", dx, dy)
}

func ScrollVertical(notches int) {
	log.Printf("scroll vertical %d", notches)
}

func ScrollHorizontal(notches int) {
	log.Printf("scroll horizontal %d", notches)
}

func MouseButton(button int, down bool) {
	log.Printf("mouse button %d down=%v", button, down)
}

func HandleKey(hid, mods int, down bool) {
	log.Printf("key hid=%d mods=%d down=%v", hid, mods, down)
}
