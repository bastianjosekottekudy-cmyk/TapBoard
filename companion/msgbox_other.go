//go:build !windows

package main

import "fmt"

func messageBox(title, text string) error {
	fmt.Printf("%s: %s\n", title, text)
	return nil
}
