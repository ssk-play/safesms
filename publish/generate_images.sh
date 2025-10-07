#!/bin/bash

# Script to generate PNG images from SVG files
# Requires: librsvg (rsvg-convert) or Inkscape or ImageMagick

echo "Kids SMS - Image Generator"
echo "=========================="

# Check for available tools
if command -v rsvg-convert &> /dev/null; then
    echo "Using rsvg-convert..."
    rsvg-convert -w 512 -h 512 app_icon.svg -o app_icon.png
    rsvg-convert -w 1024 -h 500 featured_graphic.svg -o featured_graphic.png
    echo "✓ Images generated successfully"
elif command -v inkscape &> /dev/null; then
    echo "Using Inkscape..."
    inkscape app_icon.svg --export-filename=app_icon.png --export-width=512 --export-height=512
    inkscape featured_graphic.svg --export-filename=featured_graphic.png --export-width=1024 --export-height=500
    echo "✓ Images generated successfully"
elif command -v convert &> /dev/null; then
    echo "Using ImageMagick..."
    convert -background none -density 300 app_icon.svg -resize 512x512 app_icon.png
    convert -background none -density 300 featured_graphic.svg -resize 1024x500 featured_graphic.png
    echo "✓ Images generated successfully"
else
    echo "❌ Error: No SVG converter found"
    echo ""
    echo "Please install one of the following:"
    echo "  - librsvg: brew install librsvg"
    echo "  - Inkscape: brew install inkscape"
    echo "  - ImageMagick: brew install imagemagick"
    echo ""
    echo "Or use online converter:"
    echo "  - https://cloudconvert.com/svg-to-png"
    echo "  - https://convertio.co/svg-png/"
    exit 1
fi

echo ""
echo "Generated files:"
ls -lh app_icon.png featured_graphic.png 2>/dev/null || echo "  (PNG files not found - conversion may have failed)"
