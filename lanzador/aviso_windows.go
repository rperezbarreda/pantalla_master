package main

import (
	"syscall"
	"unsafe"
)

// Un aviso de Windows (no hay consola: el .exe va sin ventana negra).
func avisarFalta(texto string) {
	u := syscall.NewLazyDLL("user32.dll").NewProc("MessageBoxW")
	t, _ := syscall.UTF16PtrFromString(texto)
	c, _ := syscall.UTF16PtrFromString("Pantalla de máster")
	u.Call(0, uintptr(unsafe.Pointer(t)), uintptr(unsafe.Pointer(c)), 0x10)
}
