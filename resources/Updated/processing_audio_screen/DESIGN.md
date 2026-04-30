# Design System Specification: Mindful & Natural

## 1. Overview & Creative North Star

### Creative North Star: "The Digital Sanctuary"
This design system moves away from the rigid, cold efficiency of "Enterprise & Structured" software. Instead, it embraces a "Digital Sanctuary" aesthetic—an environment that feels curated, breathable, and deeply human. We achieve this by rejecting traditional grids in favor of organic, pill-shaped containment and soft, tonal shifts.

The system breaks the "template" look through:
*   **Intentional Asymmetry:** Using varying container heights and "floating" elements to mimic the unpredictability of nature.
*   **Softened Geometry:** Every edge is a radius. There are no hard corners, reflecting the organic curves of stones, leaves, and landscapes.
*   **Tactile Depth:** Using color and light (rather than lines) to create a UI that feels layered, like sheets of fine handmade paper resting upon one another.

---

## 2. Colors

The palette is a sophisticated blend of earth-toned neutrals and muted botanicals. It is designed to reduce cognitive load and provide a calming user experience.

### The "No-Line" Rule
**Standard 1px borders are strictly prohibited for sectioning.** To define boundaries, designers must use background color shifts. For example, a card should be defined by placing a `surface_container_lowest` (#ffffff) element on a `surface_container` (#f6f4ec) background. If visual separation is needed, use white space or a tonal shift—never a line.

### Surface Hierarchy & Nesting
Treat the UI as physical layers. Depth is achieved through the nesting of Material tokens:
*   **Base Layer:** `background` (#fffcf7)
*   **Secondary Sectioning:** `surface_container_low` (#fcf9f3)
*   **Elevated Components:** `surface_container_lowest` (#ffffff) for maximum "pop" and clarity.

### The "Glass & Gradient" Rule
To add a premium editorial feel, use **Glassmorphism** for floating navigation bars or modal overlays. 
*   **Recipe:** Apply `surface_container_lowest` at 70% opacity with a 20px Backdrop Blur.
*   **Signature Textures:** Use subtle linear gradients for primary CTAs, transitioning from `primary` (#7b5e53) to `primary_dim` (#6e5248) to give buttons a "weighted," high-end feel.

---

## 3. Typography

The typography strategy pairs **Plus Jakarta Sans** (for character and authority) with **Be Vietnam Pro** (for readability and warmth).

*   **Display & Headlines (Plus Jakarta Sans):** These are the "voice" of the system. Large font sizes (`display-lg` at 3.5rem) should be used with generous leading to create an editorial, magazine-like feel.
*   **Body & Titles (Be Vietnam Pro):** Chosen for its friendly, open apertures. It maintains the "Mindful" aesthetic while ensuring long-form content is accessible.
*   **Hierarchy as Identity:** Use `headline-lg` in `on_surface` (#373831) to ground the page, and `label-md` in `on_surface_variant` (#64655c) for metadata to create a clear, non-aggressive hierarchy.

---

## 4. Elevation & Depth

We eschew traditional "box shadows" in favor of **Tonal Layering** and **Ambient Light**.

*   **The Layering Principle:** Stack `surface_container` tiers to create a natural lift. An inner container should always be at least one tier "brighter" or "dimmer" than its parent to signify importance.
*   **Ambient Shadows:** If an element must float (e.g., a FAB or a primary card), use a shadow with a 40px–60px blur and 4%–6% opacity. The shadow color should be a tint of `on_surface` (#373831), never pure black.
*   **The "Ghost Border" Fallback:** If a container sits on a background of the same color, use a 1px "Ghost Border": `outline_variant` (#babaaf) at 15% opacity. It should be felt, not seen.
*   **Roundedness Scale:**
    *   **Buttons/Chips:** `full` (9999px) for the signature "pill" look.
    *   **Cards/Containers:** `xl` (3rem) or `lg` (2rem) to maintain the organic softness.

---

## 5. Components

### Buttons
*   **Primary:** Pill-shaped (`full`), `primary` background, `on_primary` text. Use the "Signature Texture" gradient for a premium feel.
*   **Secondary:** Pill-shaped, `secondary_container` background with `on_secondary_container` text. 
*   **Tertiary:** No background. Use `primary` text weight with an icon.

### Cards & Lists
*   **The Divider Forbiddance:** Never use a horizontal line to separate list items. Use 16px–24px of vertical white space or a subtle `surface_container_high` background on hover.
*   **Content Cards:** Always use the `xl` (3rem) corner radius. Content should have generous internal padding (min 24px).

### Input Fields
*   **Style:** Pill-shaped containers (`full`) using `surface_container_highest` background. 
*   **States:** On focus, transition the "Ghost Border" to `primary` at 40% opacity. Avoid heavy glows.

### Specialized Components
*   **Audio Waveforms:** To match the "Mindful" reference, use rounded bars of varying heights using `primary_container` and `secondary_container` tokens.
*   **Progress Pillars:** Instead of thin lines, use thick, highly rounded bars (pill-shaped) to represent data or progress, as seen in the "Journal Stats" reference.

---

## 6. Do’s and Don’ts

### Do
*   **Do** embrace negative space. If a layout feels "full," it is no longer mindful.
*   **Do** use asymmetrical layouts where one element is slightly offset to create a custom, high-end feel.
*   **Do** use the `secondary` green (#586a45) for success states and "growth" related features to reinforce the natural theme.

### Don’t
*   **Don’t** use 90-degree corners. Even a 4px radius is too sharp for this system.
*   **Don’t** use pure black (#000000) for text. Use `on_surface` (#373831) to maintain warmth.
*   **Don’t** use standard Material dividers. They break the organic flow. Rely on the "No-Line" Rule for all sectioning.