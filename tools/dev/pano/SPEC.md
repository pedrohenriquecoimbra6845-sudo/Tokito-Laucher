# Voxel panorama pipeline — spec (for the panorama agent)

Goal: 6 Minecraft title-screen style panoramas = the camera stands INSIDE a world made of 16x16-textured cubes, rendered with a
BSL-Shaders look (soft sun shadows, ambient occlusion, volumetric god rays, bloom, rich sky/clouds, fog, tone mapping). In the
launcher the strip is scrolled slowly (like the Minecraft menu rotating), so the output is a seamless 360° CYLINDRICAL strip.
The old flat 2D art in pano/lib.py + scenes_a.py is superseded (keep only as a reference); do not ship it.

Work dir: S0/pano/  (S0 = tools/dev). Machine: 2 CPUs, 8 GB, gcc/clang +
OpenMP OK, numpy/scipy/cv2/PIL(WebP) OK, NO numba/torch, no pip/network. Outbound network from the shell is blocked.

## Engine (C via ctypes)  — vox.c + engine.py
- Build: `gcc -O3 -march=native -ffast-math -fopenmp -shared -fPIC vox.c -o libvox.so -lm`. Pass render parameters as ONE flat float array
  whose indices come from a generated `params.h` (engine.py writes it from a Python list of (NAME, n_floats)) so C and Python never drift.
- World: uint8 voxels NX*NY*NZ (about 384 x 128 x 384), index = x + NX*(z + NZ*y). Block table (int32[256*8]): kind (0 air, 1 solid, 2 cutout/leaves,
  3 water, 4 cross-plant), emit (0..15 block light), refl, random-rotation flag, tex ids for top/side/bottom. Textures: procedural 16x16, float
  linear RGB + alpha + emissive (5 floats/texel). Light volumes by BFS in C: skylight and block light 0..15 (leaves -2, water -3).
- Camera: world centre, ~1.7 blocks above ground. Cylindrical projection: pixel (i,j) -> azimuth th = 2*pi*(i+.5)/W, direction
  (sin th, (row0 - j - .5)/fpx, cos th), fpx = W/(2*pi). W=4096, H=704, row0 ~ 0.55*H (horizon a bit below the middle; sun/clouds visible above).
  Seamless: no vignette; every blur wraps in x.
- Ray: voxel DDA (Amanatides-Woo). Texel nearest; blend toward the texture's average colour beyond a LOD distance to kill shimmer.
  Leaves: alpha-tested (holes), no inner faces. Cross plants (grass/flowers/vines): ray vs two diagonal quads in the cell, alpha-tested.
- Lighting per hit = Minecraft smooth lighting (bilinear of sky/block light of the 4 cells in front of the face + AO from neighbours) +
  sun: N.L with ONE jittered shadow ray per sample (soft penumbra, leaves give dappled shadows) + sky ambient (hemisphere colours) + block light
  (warm, curve ~ (l/15)^2.2) + emissive texels (HDR gain) + a minimum ambient so caves are not black.
- Fog: exponential to the sky horizon colour (+ sun scatter toward the sun) so distant land melts into the sky like BSL.
- Sky: vertical gradient + sun glow + SQUARE sun/moon discs (Minecraft style) + stars + blocky slab clouds (2D DDA with side faces).
- Water: Fresnel mix of a reflected ray (wave-perturbed normal) and the refracted continuation with per-channel absorption; ice = reflective solid.
- Supersample 2x2 (average in HDR after clamping each sample to <=16), render with OpenMP. Depth buffer out for god rays.
- God rays: separate low-res (1/4) pass: 48 samples per ray with shadow rays, Henyey-Greenstein phase, height falloff, jittered; upsample (wrap x), add in HDR.
- Post (numpy/scipy): bloom (multi-scale gaussians on HDR > 1, wrap x), exposure + ACES-like tone map, saturation/contrast, tiny dither,
  optional pixel-square particles (petals / snowflakes / ash) added in post. NO baked vignette. Save PNG (preview) + WebP.

## Scenes (names are fixed: cherry, sakura, cave, snow, pale, bawmc)
Each scene: generator in Python builds the voxel world (noise terrain, blocks, trees/structures placed around the camera at all azimuths so
the whole 360° is interesting), a params dict (sun/moon dir+colour, sky colours, fog, clouds, ambient, god-ray density, exposure, grading).
1. cherry  — Cherry Grove, golden morning (sun elevation ~20-25°, warm), pastel pink/blue sky. Hillside + big cherry trees (pink canopies,
   twisty trunks, leaves with holes), pink-petal speckled grass, flowers/tall grass (cross plants), small pond, path, far hills + snowy peaks in fog,
   a few falling petals. This one is the launcher's still default -> record `start_u` of the best 100° window in manifest.
2. sakura  — Blue hour/sunset Japanese garden: red torii gate over a pond (reflection), stone lanterns + glowing paper lanterns, a pagoda in
   the distance (dark tile roofs, red/white walls), cherry trees, magenta/orange sky, low sun with big god rays, petals.
3. cave    — Lush cave: moss, azalea, glow berries (emissive), clay/dripstone, big underground lake with reflections, ceiling hole letting a
   volumetric sun shaft in; dark, moody, colourful lights (lava/glow berries/lanterns).
4. snow    — Snowy taiga, cold early morning: spruce forest with snow, frozen lake (reflective ice), low sun, blue-white fog, falling snow.
5. pale    — Pale Garden dusk: pale oak trees (grey-white bark, dark muted foliage), pale moss carpet, hanging moss, thick grey fog,
   glowing eyeblossoms (orange/blue emissive), overcast mood, desaturated.
6. bawmc   — offline fallback for the BawMC server wallpaper (the app downloads the real image at runtime): server hub at sunset, floating
   islands + castle spawn + bridges, purple/orange sky, lanterns.
Quality bar: must read as a Minecraft screenshot with BSL shaders (crisp pixel textures near, soft shadows, bloom, great sky + fog), NOT flat art,
no blown-out bloom, rich but natural colours. Check seams by viewing a half-width roll of each strip.

## Deliverables (all under S0/pano/final/ ; the coordinator copies them into the repo — do NOT touch the repo)
- pano_<name>.webp   4096x704, lossy q~86-90 (each <= ~1.4 MB), 360° seamless
- thumb_<name>.webp  480x270 crop of the best ~100° window
- preview_<name>.png 2048x352 (for the coordinator's review; not shipped)
- manifest.json: {name: {title_en, title_pt, start_u (0..1, best still view), row0_frac, vfov_up_deg, vfov_down_deg}}
- NOTES.md: how to re-render (one command per scene), parameters worth tweaking, known flaws.
Do NOT touch the repo at all (no Kotlin, no strings, no git).
