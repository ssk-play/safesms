# App Icon Specifications

## Google Play Store Icon Requirements
- **Size**: 512x512 pixels
- **Format**: 32-bit PNG with alpha channel
- **Color Space**: sRGB
- **Max file size**: 1024 KB

## Design Recommendations

### Concept 1: Shield with Message Bubble (Recommended)
- **Background**: Gradient from blue (#2196F3) to teal (#00BCD4)
- **Icon**: White shield outline with a message bubble inside
- **Badge**: Small "no link" icon (🚫) or child icon
- **Style**: Minimalist, modern, Material Design, parent-friendly

### Concept 2: Message Bubble with Blocked Link
- **Background**: Solid green/blue background (#4CAF50 or #2196F3)
- **Icon**: Large message bubble with a crossed-out link/chain symbol
- **Style**: Direct, clear safety message

### Concept 3: Parent & Child Protection
- **Background**: Gradient background (safe, warm colors)
- **Icon**: Parent/child silhouette with message bubble and shield
- **Style**: Friendly, family-focused, trustworthy

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
