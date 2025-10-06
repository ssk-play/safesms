# App Icon Specifications

## Google Play Store Icon Requirements
- **Size**: 512x512 pixels
- **Format**: 32-bit PNG with alpha channel
- **Color Space**: sRGB
- **Max file size**: 1024 KB

## Design Recommendations

### Concept 1: Shield with Message Bubble
- **Background**: Gradient from blue (#2196F3) to teal (#00BCD4)
- **Icon**: White shield outline with a message bubble inside
- **Style**: Minimalist, modern, Material Design

### Concept 2: Lock with SMS Symbol
- **Background**: Solid color background (#1976D2)
- **Icon**: White lock icon with SMS envelope/bubble
- **Style**: Clean, security-focused

### Concept 3: Chat Bubble with Check
- **Background**: Gradient background (primary brand colors)
- **Icon**: Rounded chat bubble with checkmark
- **Style**: Friendly, approachable

## Current Icon Location
The app currently uses the default Android launcher icon at:
- `app/src/main/res/mipmap-*/ic_launcher.png`
- `app/src/main/res/mipmap-*/ic_launcher_round.png`

## Tools for Creation
- Adobe Illustrator / Figma (vector design)
- Android Studio Image Asset Studio
- Online tools: https://romannurik.github.io/AndroidAssetStudio/

## Design Guidelines
- Follow Material Design icon guidelines
- Ensure icon is recognizable at small sizes
- Use simple, bold shapes
- Avoid fine details that won't scale well
- Test on various backgrounds (light/dark)
