//go:build !windows

package main

import (
	"fmt"
	"os"
	"os/exec"
	"runtime"
)

func avisarFalta(texto string) {
	fmt.Fprintln(os.Stderr, texto)
	if runtime.GOOS == "darwin" {
		exec.Command("osascript", "-e", "display alert \"Pantalla de máster\" message \""+texto+"\"").Run()
	}
}
