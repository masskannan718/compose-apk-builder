#!/bin/bash
echo "Opening Puyal v5.3 Ultra-lightweight Console (<50MB RAM)..."
if command -v termux-open-url >/dev/null 2>&1; then
    termux-open-url "$(pwd)/index.html"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$(pwd)/index.html"
else
    echo "Console generated at: $(pwd)/index.html"
    echo "You can open this file in any browser."
fi
