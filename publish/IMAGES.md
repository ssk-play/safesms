# Generated Images

## Files

### App Icon
- **File**: `app_icon.png`
- **Size**: 512x512 pixels
- **Format**: PNG with transparency
- **Design**: Shield with message bubble + blocked link symbol
- **Colors**: Blue gradient (#2196F3 → #00BCD4)

### Featured Graphic
- **File**: `featured_graphic.png`
- **Size**: 1024x500 pixels
- **Format**: PNG
- **Design**: App icon + "Kids SMS - Safe Messaging" + key features
- **Features shown**: Links Blocked, Kid-Safe, No Distractions

## Source Files
- `app_icon.svg` - Editable vector source for app icon
- `featured_graphic.svg` - Editable vector source for featured graphic

## Regenerate

To regenerate PNG files from SVG sources:

```bash
./generate_images.sh
```

Requires: ImageMagick, Inkscape, or librsvg

## Edit

To customize the designs, edit the SVG files with:
- Inkscape (free, GUI)
- Adobe Illustrator
- Any text editor (SVG is XML)

Then run `./generate_images.sh` to regenerate PNGs.
