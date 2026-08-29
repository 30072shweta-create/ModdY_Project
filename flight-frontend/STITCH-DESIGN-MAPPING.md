# STITCH-DESIGN-MAPPING.md
## Visual Design Tokens & Component Specifications
**Project Name**: SkyRoute Premium Flight Management  
**Stitch Project ID**: `6969113012666830143`

---

## 1. Color System (Material 3 & Tailwind Theme)

### Primary & Dark Brand Palette
| Token | Hex Value | Usage / Description |
|---|---|---|
| `primary` | `#000000` | Primary brand headings, dark accents, high-contrast text |
| `primary-container` | `#131b2e` | Deep Navy Sidebar background, hero containers |
| `on-primary-container` | `#7c839b` | Muted text on dark primary containers |
| `primary-fixed` | `#dae2fd` | Light blue-grey container accents |
| `on-primary-fixed` | `#131b2e` | Text on fixed primary containers |

### Secondary Brand Palette (SkyRoute Accent Blue)
| Token | Hex Value | Usage / Description |
|---|---|---|
| `secondary` | `#0058be` | Action buttons, active tab indicators, focus rings |
| `secondary-container` | `#2170e4` | Active sidebar item highlights, selected badge backgrounds |
| `secondary-fixed` | `#d8e2ff` | Light blue badge chips, highlight backgrounds |
| `on-secondary` | `#ffffff` | Primary text on secondary buttons |
| `on-secondary-fixed-variant` | `#004395` | Dark blue text on light blue chips |

### Neutral & Surface Palette
| Token | Hex Value | Usage / Description |
|---|---|---|
| `background` | `#f7f9fb` | Global page background color |
| `surface` | `#f7f9fb` | Top navigation bar background, page canvas |
| `surface-container-lowest` | `#ffffff` | Pure white card background (flight cards, forms) |
| `surface-container-low` | `#f2f4f6` | Light grey secondary background |
| `surface-container` | `#eceef0` | Table row hover, input default backgrounds |
| `surface-container-high` | `#e6e8ea` | Hero background container |
| `surface-container-highest` | `#e0e3e5` | Footer background, progress bar tracks |
| `on-surface` | `#191c1e` | Default body text color |
| `on-surface-variant` | `#45464d` | Secondary body text, field labels, metadata |
| `outline` | `#76777d` | Icons, divider lines |
| `outline-variant` | `#c6c6cd` | Card borders, input field borders |

### Semantic & Status Palette
| Token | Hex Value | Background | Usage / Description |
|---|---|---|---|
| `success` | `#166534` | `#DCFCE7` | Direct flight tags, booking confirmation checkmarks |
| `warning` | `#92400E` | `#FEF3C7` | Seat lock expiration alerts, pending payment warning |
| `error` | `#ba1a1a` | `#ffdad6` | Form validation error text, error icon badges |

---

## 2. Typography System

### Font Families
- **Display & Headings**: `Plus Jakarta Sans`, sans-serif (Weights: 600, 700)
- **Body & Labels**: `Inter`, sans-serif (Weights: 400, 500, 600)

### Type Scale Specification
| Style Name | Font Family | Size | Line Height | Weight | Letter Spacing |
|---|---|---|---|---|---|
| `display-lg` | Plus Jakarta Sans | 48px | 56px | 700 (Bold) | -0.02em |
| `headline-lg` | Plus Jakarta Sans | 32px | 40px | 700 (Bold) | -0.01em |
| `headline-md` | Plus Jakarta Sans | 24px | 32px | 600 (Semibold) | 0 |
| `headline-sm` | Plus Jakarta Sans | 20px | 28px | 700 (Bold) | 0 |
| `body-lg` | Inter | 18px | 28px | 400 (Regular) | 0 |
| `body-md` | Inter | 16px | 24px | 400 (Regular) | 0 |
| `body-sm` | Inter | 14px | 20px | 400 (Regular) | 0 |
| `label-md` | Inter | 14px | 16px | 600 (Semibold) | +0.02em |
| `label-sm` | Inter | 12px | 14px | 500 (Medium) | +0.04em |

---

## 3. Layout Grid & Spacing System

### Spacing Scale
- `xs`: 4px
- `sm`: 8px
- `md`: 16px
- `lg`: 24px
- `xl`: 32px
- `2xl`: 48px
- `3xl`: 64px
- `gutter`: 24px
- `container-max`: 1280px (Max content width)

### Border Radius Scale
- `sm` / `DEFAULT`: `0.25rem` (4px)
- `lg`: `0.5rem` (8px) - Inputs, buttons, small cards
- `xl`: `0.75rem` (12px) - Content cards, modals, flight cards
- `full`: `9999px` - Badges, avatars, pill buttons

### Elevation & Shadows
- **Flight & Content Cards**: `box-shadow: 0 4px 6px -1px rgba(15, 23, 42, 0.05), 0 2px 4px -2px rgba(15, 23, 42, 0.05)`
- **Hover Lift State**: `transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.1), 0 4px 6px -2px rgba(15, 23, 42, 0.05)`
- **Hero Search Widget (Floating)**: `box-shadow: 0 8px 30px rgba(0,0,0,0.12)`
- **Top Navigation Bar**: `box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05)`

---

## 4. Reusable Visual Components & Patterns

### A. Top Navigation Bar (Header)
- **Height**: Fixed 72px / 80px
- **Layout**: Brand Logo (Left) + Nav Links (Center) + Utility Icons & Auth Buttons (Right)
- **Sticky Behavior**: `fixed top-0 w-full z-50 bg-surface border-b border-outline-variant`

### B. Side Navigation Shell (Dashboard & Admin)
- **Width**: Fixed 280px
- **Background**: `primary-container` (`#131b2e`)
- **Active State**: `bg-secondary-container text-on-secondary-container font-bold`
- **Inactive State**: `text-on-primary-container opacity-80 hover:opacity-100 hover:bg-on-primary-fixed-variant`

### C. Flight Result Card
- **Background**: `#ffffff` (`surface-container-lowest`) with 12px border radius
- **Structure**: Airline Logo + Flight Number \| Departure Time & Code \| Duration & Stops Bar \| Arrival Time & Code \| Price & Action Button
- **Direct Tag**: `#166534` text on `#DCFCE7` pill background

### D. Form Inputs & Selects
- **Height**: 44px
- **Border**: `1px solid #c6c6cd` (`outline-variant`), `0.5rem` radius
- **Focus Ring**: `border-secondary ring-2 ring-secondary/20`
- **Icon Integration**: Absolute positioned Material Symbol on left (`pl-[36px]`)
- **Validation Errors**: `border-error text-error` with error helper caption

### E. Buttons
- **Primary Button**: `bg-secondary` (`#0058be`) + `text-white` + `px-lg py-md rounded-lg` + `hover:opacity-90 active:scale-95`
- **Outline Button**: `border border-outline-variant` + `bg-surface` + `text-primary` + `hover:bg-surface-container-low`
- **Dark Action Button**: `bg-primary` (`#000000`) + `text-white`

### F. Interactive Aircraft Seat Map Grid
- **Fuselage Container**: Centered 400px container with curved cockpit top (`rounded-[3rem]`)
- **Layout**: 6-abreast (A-B-C [Aisle] D-E-F) grid layout
- **Seat States**:
  - `Available`: `#ffffff` background with `#c6c6cd` border
  - `Premium`: `#f7f9fb` background with `#d8e2ff` border (+$45)
  - `Selected`: `#0058be` background with `#0058be` border & white text
  - `Occupied`: `#e0e3e5` solid background (disabled)

---

## 5. Responsive Breakpoint Rules
- **Mobile (`< 768px`)**: Hamburger navigation drawer, single-column search forms, stacked flight card details.
- **Tablet (`768px - 1024px`)**: Grid auto-fits 2 columns, floating search bar converts to inline stack.
- **Desktop (`> 1024px`)**: Fixed 280px sidebar, 12-column layout grids, bento grid image gallery.
