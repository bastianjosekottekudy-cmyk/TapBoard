//go:build windows

package main

import (
	"syscall"
	"unsafe"
)

var (
	user32               = syscall.NewLazyDLL("user32.dll")
	procSendInput         = user32.NewProc("SendInput")
	procMapVirtualKeyW    = user32.NewProc("MapVirtualKeyW")
)

const (
	inputMouse    = 0
	inputKeyboard = 1

	mouseMove       = 0x0001
	mouseLeftDown   = 0x0002
	mouseLeftUp     = 0x0004
	mouseRightDown  = 0x0008
	mouseRightUp    = 0x0010
	mouseMiddleDown = 0x0020
	mouseMiddleUp   = 0x0040
	mouseWheel      = 0x0800
	mouseHWheel     = 0x1000

	keyEventKeyUp   = 0x0002
	keyEventScanCode = 0x0008
	keyEventExtended = 0x0001

	wheelDelta = 120
)

type mouseInput struct {
	Type uint32
	Mi   mouseInputData
}

type mouseInputData struct {
	Dx          int32
	Dy          int32
	MouseData   uint32
	Flags       uint32
	Time        uint32
	ExtraInfo   uintptr
}

type keybdInput struct {
	Type uint32
	Ki   keybdInputData
}

type keybdInputData struct {
	Vk        uint16
	Scan      uint16
	Flags     uint32
	Time      uint32
	ExtraInfo uintptr
	_         [8]byte // padding to match MOUSEINPUT size on amd64 union
}

// SendInput on 64-bit uses a union sized to MOUSEINPUT (32 bytes payload + type).
type input struct {
	Type uint32
	_    uint32 // padding for alignment on amd64
	Data [32]byte
}

func sendMouse(flags uint32, dx, dy int32, data uint32) {
	var in input
	in.Type = inputMouse
	mi := mouseInputData{Dx: dx, Dy: dy, MouseData: data, Flags: flags}
	*(*mouseInputData)(unsafe.Pointer(&in.Data[0])) = mi
	procSendInput.Call(1, uintptr(unsafe.Pointer(&in)), unsafe.Sizeof(in))
}

func MoveMouse(dx, dy int) {
	sendMouse(mouseMove, int32(dx), int32(dy), 0)
}

func ScrollVertical(notches int) {
	sendMouse(mouseWheel, 0, 0, uint32(int32(notches*wheelDelta)))
}

func ScrollHorizontal(notches int) {
	sendMouse(mouseHWheel, 0, 0, uint32(int32(notches*wheelDelta)))
}

func MouseButton(button int, down bool) {
	var flags uint32
	switch button {
	case 0:
		if down {
			flags = mouseLeftDown
		} else {
			flags = mouseLeftUp
		}
	case 1:
		if down {
			flags = mouseRightDown
		} else {
			flags = mouseRightUp
		}
	case 2:
		if down {
			flags = mouseMiddleDown
		} else {
			flags = mouseMiddleUp
		}
	default:
		return
	}
	sendMouse(flags, 0, 0, 0)
}

// HID usage → Windows VK
var hidToVK = map[int]uint16{
	4: 0x41, 5: 0x42, 6: 0x43, 7: 0x44, 8: 0x45, 9: 0x46, 10: 0x47, 11: 0x48,
	12: 0x49, 13: 0x4A, 14: 0x4B, 15: 0x4C, 16: 0x4D, 17: 0x4E, 18: 0x4F, 19: 0x50,
	20: 0x51, 21: 0x52, 22: 0x53, 23: 0x54, 24: 0x55, 25: 0x56, 26: 0x57, 27: 0x58,
	28: 0x59, 29: 0x5A,
	30: 0x31, 31: 0x32, 32: 0x33, 33: 0x34, 34: 0x35, 35: 0x36, 36: 0x37, 37: 0x38, 38: 0x39, 39: 0x30,
	40: 0x0D, 41: 0x1B, 42: 0x08, 43: 0x09, 44: 0x20,
	45: 0xBD, 46: 0xBB, 47: 0xDB, 48: 0xDD, 49: 0xDC, 51: 0xBA, 52: 0xDE, 53: 0xC0,
	54: 0xBC, 55: 0xBE, 56: 0xBF, 57: 0x14,
	58: 0x70, 59: 0x71, 60: 0x72, 61: 0x73, 62: 0x74, 63: 0x75,
	64: 0x76, 65: 0x77, 66: 0x78, 67: 0x79, 68: 0x7A, 69: 0x7B,
	73: 0x2D, 74: 0x24, 75: 0x21, 76: 0x2E, 77: 0x23, 78: 0x22,
	79: 0x27, 80: 0x25, 81: 0x28, 82: 0x26,
	127: 0xAD, 128: 0xAF, 129: 0xAE,
	0xCD: 0xB3, 0xB5: 0xB0, 0xB6: 0xB1,
}

var modVKs = []struct {
	mask int
	vk   uint16
}{
	{0x01, 0xA2}, // LCtrl
	{0x02, 0xA0}, // LShift
	{0x04, 0xA4}, // LAlt
	{0x08, 0x5B}, // LWin
	{0x10, 0xA3},
	{0x20, 0xA1},
	{0x40, 0xA5},
	{0x80, 0x5C},
}

var heldMods int

func sendKeyVK(vk uint16, down bool) {
	var in input
	in.Type = inputKeyboard
	var flags uint32
	if !down {
		flags = keyEventKeyUp
	}
	ki := keybdInputData{Vk: vk, Flags: flags}
	*(*keybdInputData)(unsafe.Pointer(&in.Data[0])) = ki
	procSendInput.Call(1, uintptr(unsafe.Pointer(&in)), unsafe.Sizeof(in))
}

func syncMods(mods int) {
	diff := heldMods ^ mods
	for _, m := range modVKs {
		if diff&m.mask == 0 {
			continue
		}
		sendKeyVK(m.vk, mods&m.mask != 0)
	}
	heldMods = mods
}

func HandleKey(hid, mods int, down bool) {
	syncMods(mods)
	vk, ok := hidToVK[hid]
	if !ok {
		return
	}
	sendKeyVK(vk, down)
	if !down {
		// keep mods as reported
	}
	_ = procMapVirtualKeyW
}
