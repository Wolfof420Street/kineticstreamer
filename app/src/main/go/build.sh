#!/bin/bash
set -e

# Default values
ANDROID_API=${ANDROID_API:-24}
OUTPUT_DIR=${1:-""}

# Check if OUTPUT_DIR is specified
if [ -z "$OUTPUT_DIR" ]; then
    echo "Error: Output directory not specified"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Change to the Go package directory
cd $(dirname "$0")

# Check for SRT libraries
for arch in arm64-v8a armeabi-v7a x86 x86_64; do
    if [ ! -f "libs/$arch/libsrt.so" ]; then
        echo "Warning: SRT library not found for $arch. The build might fail."
        echo "Make sure to run the native build with CMake first."
    fi
done

# Check for gomobile
if ! command -v gomobile &> /dev/null; then
    echo "gomobile not found, installing..."
    go install golang.org/x/mobile/cmd/gomobile@latest
    export PATH=$PATH:$(go env GOPATH)/bin
    gomobile init
fi

# Set CGO environment variables for SRT
export CGO_CFLAGS="-I/usr/local/Cellar/srt/1.5.4/include"
export CGO_LDFLAGS="-L/usr/local/Cellar/srt/1.5.4/lib -lsrt"

echo -e "\nBuilding kinetic AAR for Android API $ANDROID_API...\n"

# Build the AAR
gomobile bind -target=android -androidapi=$ANDROID_API -o="$OUTPUT_DIR/kinetic.aar" .

echo "Build complete. AAR file at: $OUTPUT_DIR/kinetic.aar"