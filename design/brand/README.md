# Open Scanner brand mark

The launcher mark combines an open radar aperture, wireless signal arcs, and a
three-bar spectrum indicator. It is intentionally text-free and uses the app's
existing cyan and amber accents so it remains recognizable at launcher size.

## Assets

- `open-scanner-logo-master.png` is the 1254 x 1254 transparent master.
- `app/src/main/res/drawable-nodpi/open_scanner_icon.png` is the 512 x 512
  Android launcher foreground generated from that master.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` supplies the adaptive
  icon on Android 8 through Android 12.
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml` adds Android 13+
  monochrome/themed-icon support.

The launcher background remains `#090B0C`. The mark is centered inside Android's
66 x 66 dp adaptive-icon safe circle. Keep the transparent padding: launchers
apply different masks and animated scaling to adaptive icons.

## Generation record

The source artwork was generated with OpenAI's built-in image generation tool
on 2026-07-31 and converted from a flat chroma-key background to a transparent
PNG. The working prompt was:

> Create one original, clean, modern geometric Android app mark for Open
> Scanner. Combine a bold broken circular radar aperture forming an abstract
> open O with wireless signal arcs and three compact spectrum bars. Use cyan
> `#45C9E9` with a small amber `#FFB10F` accent, a strong silhouette, generous
> adaptive-icon padding, and no text, watermark, scene, shadow, or inner app-tile
> container.
