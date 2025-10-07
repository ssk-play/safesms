#!/usr/bin/env python3
"""Generate app icon PNG with gradient background"""

from PIL import Image, ImageDraw
import math

# Create 512x512 image
size = 512
img = Image.new('RGB', (size, size))
draw = ImageDraw.Draw(img, 'RGBA')

# Draw gradient background from #2196F3 to #00BCD4
for y in range(size):
    for x in range(size):
        # Calculate gradient ratio (0.0 to 1.0)
        ratio = math.sqrt((x/size)**2 + (y/size)**2) / math.sqrt(2)

        # Interpolate between #2196F3 and #00BCD4
        r = int(0x21 + (0x00 - 0x21) * ratio)
        g = int(0x96 + (0xBC - 0x96) * ratio)
        b = int(0xF3 + (0xD4 - 0xF3) * ratio)

        img.putpixel((x, y), (r, g, b))

# Draw rounded rectangle for message bubble
bubble_left = 90
bubble_top = 160
bubble_right = 420
bubble_bottom = 340
bubble_radius = 30

# White message bubble
draw.rounded_rectangle(
    [(bubble_left, bubble_top), (bubble_right, bubble_bottom)],
    radius=bubble_radius,
    fill='white'
)

# Triangle pointer at bottom left
pointer_points = [
    (bubble_left + 30, bubble_bottom),
    (bubble_left, bubble_bottom + 60),
    (bubble_left, bubble_bottom)
]
draw.polygon(pointer_points, fill='white')

# Three blue dots
dot_y = 250
dot_radius = 18
dot_color = '#2196F3'

draw.ellipse([(190-dot_radius, dot_y-dot_radius),
              (190+dot_radius, dot_y+dot_radius)], fill=dot_color)
draw.ellipse([(256-dot_radius, dot_y-dot_radius),
              (256+dot_radius, dot_y+dot_radius)], fill=dot_color)
draw.ellipse([(322-dot_radius, dot_y-dot_radius),
              (322+dot_radius, dot_y+dot_radius)], fill=dot_color)

# Apply rounded corners to the entire image
mask = Image.new('L', (size, size), 0)
mask_draw = ImageDraw.Draw(mask)
mask_draw.rounded_rectangle([(0, 0), (size, size)], radius=80, fill=255)

# Create output image with transparency
output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
output.paste(img, (0, 0))
output.putalpha(mask)

# Save
output.save('app_icon.png')
print("Icon generated: app_icon.png")
