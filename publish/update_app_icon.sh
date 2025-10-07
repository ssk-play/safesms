#!/bin/bash

# Script to update app icon in Android project
# Converts app_icon.png to various densities and copies to res folders

echo "Kids SMS - Update App Icon"
echo "==========================="

SOURCE_ICON="app_icon.png"
APP_RES_DIR="../app/src/main/res"

if [ ! -f "$SOURCE_ICON" ]; then
    echo "❌ Error: $SOURCE_ICON not found"
    echo "Run this script from the publish/ directory"
    exit 1
fi

# Check for ImageMagick
if ! command -v magick &> /dev/null; then
    echo "❌ Error: ImageMagick not found"
    echo "Install with: brew install imagemagick"
    exit 1
fi

echo "Converting icon to various densities..."

# Generate icons for each density
magick convert "$SOURCE_ICON" -resize 48x48   ic_launcher_mdpi.png
magick convert "$SOURCE_ICON" -resize 72x72   ic_launcher_hdpi.png
magick convert "$SOURCE_ICON" -resize 96x96   ic_launcher_xhdpi.png
magick convert "$SOURCE_ICON" -resize 144x144 ic_launcher_xxhdpi.png
magick convert "$SOURCE_ICON" -resize 192x192 ic_launcher_xxxhdpi.png

# Round icon (same sizes)
magick convert "$SOURCE_ICON" -resize 48x48   ic_launcher_round_mdpi.png
magick convert "$SOURCE_ICON" -resize 72x72   ic_launcher_round_hdpi.png
magick convert "$SOURCE_ICON" -resize 96x96   ic_launcher_round_xhdpi.png
magick convert "$SOURCE_ICON" -resize 144x144 ic_launcher_round_xxhdpi.png
magick convert "$SOURCE_ICON" -resize 192x192 ic_launcher_round_xxxhdpi.png

echo "Copying to app resources..."

# Copy to mipmap directories
cp ic_launcher_mdpi.png    "$APP_RES_DIR/mipmap-mdpi/ic_launcher.png"
cp ic_launcher_hdpi.png    "$APP_RES_DIR/mipmap-hdpi/ic_launcher.png"
cp ic_launcher_xhdpi.png   "$APP_RES_DIR/mipmap-xhdpi/ic_launcher.png"
cp ic_launcher_xxhdpi.png  "$APP_RES_DIR/mipmap-xxhdpi/ic_launcher.png"
cp ic_launcher_xxxhdpi.png "$APP_RES_DIR/mipmap-xxxhdpi/ic_launcher.png"

cp ic_launcher_round_mdpi.png    "$APP_RES_DIR/mipmap-mdpi/ic_launcher_round.png"
cp ic_launcher_round_hdpi.png    "$APP_RES_DIR/mipmap-hdpi/ic_launcher_round.png"
cp ic_launcher_round_xhdpi.png   "$APP_RES_DIR/mipmap-xhdpi/ic_launcher_round.png"
cp ic_launcher_round_xxhdpi.png  "$APP_RES_DIR/mipmap-xxhdpi/ic_launcher_round.png"
cp ic_launcher_round_xxxhdpi.png "$APP_RES_DIR/mipmap-xxxhdpi/ic_launcher_round.png"

# Clean up temporary files
rm ic_launcher_*.png

echo "✓ App icon updated successfully!"
echo ""
echo "Updated files:"
echo "  - mipmap-mdpi/ic_launcher.png (48x48)"
echo "  - mipmap-hdpi/ic_launcher.png (72x72)"
echo "  - mipmap-xhdpi/ic_launcher.png (96x96)"
echo "  - mipmap-xxhdpi/ic_launcher.png (144x144)"
echo "  - mipmap-xxxhdpi/ic_launcher.png (192x192)"
echo ""
echo "Rebuild the app to see changes:"
echo "  ./gradlew clean assembleDebug"
