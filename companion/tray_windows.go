//go:build windows

package main

import (
	"runtime"
	"sync"
	"syscall"
	"unsafe"

	"golang.org/x/sys/windows"
)

const (
	wmTray            = 0x0401 // WM_USER+1
	wmAppQuit         = 0x8001
	nimAdd            = 0x00000000
	nimModify         = 0x00000001
	nimDelete         = 0x00000002
	nifMessage        = 0x00000001
	nifIcon           = 0x00000002
	nifTip            = 0x00000004
	nifInfo           = 0x00000010
	nisHidden         = 0x00000001
	wmLButtonUp       = 0x0202
	wmRButtonUp       = 0x0205
	wmLButtonDblClk   = 0x0203
	wmSysCommand      = 0x0112
	scMinimize        = 0xF020
	sizeMinimized     = 1
	swHide            = 0
	swRestore         = 9
	swShow            = 5
	tpmRightButton    = 0x0002
	tpmReturnCmd      = 0x0100
	mfString          = 0x0000
	mfSeparator       = 0x0800
	idiApplication    = 32512
	imageIcon         = 1
	lrShared          = 0x8000
)

// GWLP_WNDPROC must be a var so conversion to uintptr is not a negative constant.
var gwlpWndProcIndex int32 = -4

var (
	shell32              = windows.NewLazySystemDLL("shell32.dll")
	user32Tray           = windows.NewLazySystemDLL("user32.dll")
	procShellNotifyIcon  = shell32.NewProc("Shell_NotifyIconW")
	procShowWindow       = user32Tray.NewProc("ShowWindow")
	procSetForeground    = user32Tray.NewProc("SetForegroundWindow")
	procGetWindowLongPtr = user32Tray.NewProc("GetWindowLongPtrW")
	procSetWindowLongPtr = user32Tray.NewProc("SetWindowLongPtrW")
	procCallWindowProc   = user32Tray.NewProc("CallWindowProcW")
	procDefWindowProc    = user32Tray.NewProc("DefWindowProcW")
	procCreatePopupMenu  = user32Tray.NewProc("CreatePopupMenu")
	procAppendMenu       = user32Tray.NewProc("AppendMenuW")
	procTrackPopupMenu   = user32Tray.NewProc("TrackPopupMenu")
	procDestroyMenu      = user32Tray.NewProc("DestroyMenu")
	procGetCursorPos     = user32Tray.NewProc("GetCursorPos")
	procLoadIcon         = user32Tray.NewProc("LoadIconW")
	procPostMessage      = user32Tray.NewProc("PostMessageW")
	procIsWindowVisible  = user32Tray.NewProc("IsWindowVisible")
)

type notifyIconData struct {
	CbSize           uint32
	Hwnd             uintptr
	UID              uint32
	UFlags           uint32
	UCallbackMessage uint32
	HIcon            uintptr
	SzTip            [128]uint16
	DwState          uint32
	DwStateMask      uint32
	SzInfo           [256]uint16
	UVersion         uint32
	SzInfoTitle      [64]uint16
	DwInfoFlags      uint32
	GUIDItem         windows.GUID
	HBalloonIcon     uintptr
}

type point struct {
	X, Y int32
}

type trayController struct {
	hwnd       uintptr
	origProc   uintptr
	icon       uintptr
	uid        uint32
	quitting   bool
	mu         sync.Mutex
	onQuit     func()
	added      bool
}

var activeTray *trayController

func attachSystemTray(hwnd unsafe.Pointer, onQuit func()) *trayController {
	runtime.LockOSThread()
	t := &trayController{
		hwnd:   uintptr(hwnd),
		uid:    1,
		onQuit: onQuit,
	}
	activeTray = t

	icon, _, _ := procLoadIcon.Call(0, uintptr(idiApplication))
	t.icon = icon

	orig, _, _ := procGetWindowLongPtr.Call(t.hwnd, uintptr(gwlpWndProcIndex))
	t.origProc = orig
	procSetWindowLongPtr.Call(t.hwnd, uintptr(gwlpWndProcIndex), syscall.NewCallback(trayWndProc))

	t.addIcon(false)
	return t
}

func (t *trayController) addIcon(balloon bool) {
	var nid notifyIconData
	nid.CbSize = uint32(unsafe.Sizeof(nid))
	nid.Hwnd = t.hwnd
	nid.UID = t.uid
	nid.UFlags = nifMessage | nifIcon | nifTip
	nid.UCallbackMessage = wmTray
	nid.HIcon = t.icon
	tip, _ := syscall.UTF16FromString("TapBoard Companion")
	copy(nid.SzTip[:], tip)

	msg := uintptr(nimAdd)
	t.mu.Lock()
	if t.added {
		msg = nimModify
	} else {
		t.added = true
	}
	t.mu.Unlock()
	procShellNotifyIcon.Call(msg, uintptr(unsafe.Pointer(&nid)))
}

func (t *trayController) removeIcon() {
	t.mu.Lock()
	if !t.added {
		t.mu.Unlock()
		return
	}
	t.added = false
	t.mu.Unlock()
	var nid notifyIconData
	nid.CbSize = uint32(unsafe.Sizeof(nid))
	nid.Hwnd = t.hwnd
	nid.UID = t.uid
	procShellNotifyIcon.Call(nimDelete, uintptr(unsafe.Pointer(&nid)))
}

func (t *trayController) HideToTray() {
	procShowWindow.Call(t.hwnd, swHide)
	t.addIcon(false)
}

func (t *trayController) ShowWindow() {
	procShowWindow.Call(t.hwnd, swRestore)
	procShowWindow.Call(t.hwnd, swShow)
	procSetForeground.Call(t.hwnd)
}

func (t *trayController) Quit() {
	t.mu.Lock()
	t.quitting = true
	t.mu.Unlock()
	t.removeIcon()
	if t.onQuit != nil {
		t.onQuit()
	}
	procPostMessage.Call(t.hwnd, 0x0010 /* WM_CLOSE */, 0, 0)
}

func (t *trayController) showMenu() {
	menu, _, _ := procCreatePopupMenu.Call()
	if menu == 0 {
		return
	}
	defer procDestroyMenu.Call(menu)

	appendMenu(menu, 1, "Open TapBoard")
	appendMenu(menu, 0, "-")
	appendMenu(menu, 2, "Quit")

	var pt point
	procGetCursorPos.Call(uintptr(unsafe.Pointer(&pt)))
	procSetForeground.Call(t.hwnd)
	cmd, _, _ := procTrackPopupMenu.Call(
		menu,
		tpmRightButton|tpmReturnCmd,
		uintptr(pt.X),
		uintptr(pt.Y),
		0,
		t.hwnd,
		0,
	)
	switch cmd {
	case 1:
		t.ShowWindow()
	case 2:
		t.Quit()
	}
}

func appendMenu(menu uintptr, id uintptr, text string) {
	if text == "-" {
		procAppendMenu.Call(menu, mfSeparator, 0, 0)
		return
	}
	p, _ := syscall.UTF16PtrFromString(text)
	procAppendMenu.Call(menu, mfString, id, uintptr(unsafe.Pointer(p)))
}

func trayWndProc(hwnd uintptr, msg uint32, wParam, lParam uintptr) uintptr {
	t := activeTray
	if t == nil {
		r, _, _ := procDefWindowProc.Call(hwnd, uintptr(msg), wParam, lParam)
		return r
	}

	switch msg {
	case wmSysCommand:
		if wParam&0xFFF0 == scMinimize {
			t.HideToTray()
			return 0
		}
	case 0x0005: // WM_SIZE
		if wParam == sizeMinimized {
			t.HideToTray()
			return 0
		}
	case 0x0010: // WM_CLOSE
		t.mu.Lock()
		q := t.quitting
		t.mu.Unlock()
		if !q {
			t.HideToTray()
			return 0
		}
		t.removeIcon()
	case wmTray:
		switch lParam {
		case wmLButtonUp, wmLButtonDblClk:
			t.ShowWindow()
		case wmRButtonUp:
			t.showMenu()
		}
		return 0
	}

	r, _, _ := procCallWindowProc.Call(t.origProc, hwnd, uintptr(msg), wParam, lParam)
	return r
}
